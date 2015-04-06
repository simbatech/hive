/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hadoop.hive.ql.exec.tez;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.hive.llap.io.api.LlapIoProxy;
import org.apache.hadoop.hive.ql.exec.DummyStoreOperator;
import org.apache.hadoop.hive.ql.exec.HashTableDummyOperator;
import org.apache.hadoop.hive.ql.exec.MapOperator;
import org.apache.hadoop.hive.ql.exec.MapredContext;
import org.apache.hadoop.hive.ql.exec.ObjectCache;
import org.apache.hadoop.hive.ql.exec.ObjectCacheFactory;
import org.apache.hadoop.hive.ql.exec.Operator;
import org.apache.hadoop.hive.ql.exec.OperatorUtils;
import org.apache.hadoop.hive.ql.exec.Utilities;
import org.apache.hadoop.hive.ql.exec.mr.ExecMapper.ReportStats;
import org.apache.hadoop.hive.ql.exec.mr.ExecMapperContext;
import org.apache.hadoop.hive.ql.exec.tez.TezProcessor.TezKVOutputCollector;
import org.apache.hadoop.hive.ql.exec.tez.tools.KeyValueInputMerger;
import org.apache.hadoop.hive.ql.exec.vector.VectorMapOperator;
import org.apache.hadoop.hive.ql.log.PerfLogger;
import org.apache.hadoop.hive.ql.plan.BaseWork;
import org.apache.hadoop.hive.ql.plan.MapWork;
import org.apache.hadoop.hive.ql.plan.OperatorDesc;
import org.apache.hadoop.hive.serde2.Deserializer;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspector;
import org.apache.hadoop.mapred.JobConf;
import org.apache.tez.mapreduce.input.MRInputLegacy;
import org.apache.tez.mapreduce.input.MultiMRInput;
import org.apache.tez.mapreduce.processor.MRTaskReporter;
import org.apache.tez.runtime.api.Input;
import org.apache.tez.runtime.api.LogicalInput;
import org.apache.tez.runtime.api.LogicalOutput;
import org.apache.tez.runtime.api.ProcessorContext;
import org.apache.tez.runtime.library.api.KeyValueReader;

/**
 * Process input from tez LogicalInput and write output - for a map plan
 * Just pump the records through the query plan.
 */
public class MapRecordProcessor extends RecordProcessor {


  private MapOperator mapOp;
  private final List<MapOperator> mergeMapOpList = new ArrayList<MapOperator>();
  public static final Log l4j = LogFactory.getLog(MapRecordProcessor.class);
  private MapRecordSource[] sources;
  private final Map<String, MultiMRInput> multiMRInputMap = new HashMap<String, MultiMRInput>();
  private int position;
  MRInputLegacy legacyMRInput;
  MultiMRInput mainWorkMultiMRInput;
  private final ExecMapperContext execContext;
  private boolean abort;
  protected static final String MAP_PLAN_KEY = "__MAP_PLAN__";
  private MapWork mapWork;
  List<MapWork> mergeWorkList;
  List<String> cacheKeys;
  ObjectCache cache;

  public MapRecordProcessor(final JobConf jconf) throws Exception {
    if (LlapIoProxy.isDaemon()) { // do not cache plan
      cache = new org.apache.hadoop.hive.ql.exec.mr.ObjectCache();
    } else {
      cache = ObjectCacheFactory.getCache(jconf);
    }
    execContext = new ExecMapperContext(jconf);
    execContext.setJc(jconf);
    cacheKeys = new ArrayList<String>();
  }

  @Override
  void init(final JobConf jconf, ProcessorContext processorContext, MRTaskReporter mrReporter,
      Map<String, LogicalInput> inputs, Map<String, LogicalOutput> outputs) throws Exception {
    perfLogger.PerfLogBegin(CLASS_NAME, PerfLogger.TEZ_INIT_OPERATORS);
    super.init(jconf, processorContext, mrReporter, inputs, outputs);

    String queryId = HiveConf.getVar(jconf, HiveConf.ConfVars.HIVEQUERYID);
    String key = queryId + processorContext.getTaskVertexName() + MAP_PLAN_KEY;
    cacheKeys.add(key);

    // create map and fetch operators
    mapWork = (MapWork) cache.retrieve(key, new Callable<Object>() {
        @Override
        public Object call() {
          return Utilities.getMapWork(jconf);
        }
      });
    Utilities.setMapWork(jconf, mapWork);

    String prefixes = jconf.get(DagUtils.TEZ_MERGE_WORK_FILE_PREFIXES);
    if (prefixes != null) {
      mergeWorkList = new ArrayList<MapWork>();

      for (final String prefix : prefixes.split(",")) {
        if (prefix == null || prefix.isEmpty()) {
          continue;
        }

        key = queryId + processorContext.getTaskVertexName() + prefix;
        cacheKeys.add(key);

	mergeWorkList.add(
          (MapWork) cache.retrieve(key,
              new Callable<Object>() {
                @Override
                public Object call() {
                  return Utilities.getMergeWork(jconf, prefix);
                }
              }));
      }
    }

    MapredContext.init(true, new JobConf(jconf));
    ((TezContext) MapredContext.get()).setInputs(inputs);
    ((TezContext) MapredContext.get()).setTezProcessorContext(processorContext);

    // Update JobConf using MRInput, info like filename comes via this
    legacyMRInput = getMRInput(inputs);
    if (legacyMRInput != null) {
      Configuration updatedConf = legacyMRInput.getConfigUpdates();
      if (updatedConf != null) {
        for (Entry<String, String> entry : updatedConf) {
          jconf.set(entry.getKey(), entry.getValue());
        }
      }
    }

    createOutputMap();
    // Start all the Outputs.
    for (Entry<String, LogicalOutput> outputEntry : outputs.entrySet()) {
      l4j.debug("Starting Output: " + outputEntry.getKey());
      outputEntry.getValue().start();
      ((TezKVOutputCollector) outMap.get(outputEntry.getKey())).initialize();
    }

    try {

      if (mapWork.getVectorMode()) {
        mapOp = new VectorMapOperator();
      } else {
        mapOp = new MapOperator();
      }

      mapOp.clearConnectedOperators();
      mapOp.setExecContext(execContext);

      if (mergeWorkList != null) {
        MapOperator mergeMapOp = null;
        for (BaseWork mergeWork : mergeWorkList) {
          MapWork mergeMapWork = (MapWork) mergeWork;
          if (mergeMapWork.getVectorMode()) {
            mergeMapOp = new VectorMapOperator();
          } else {
            mergeMapOp = new MapOperator();
          }

          mergeMapOpList.add(mergeMapOp);
          // initialize the merge operators first.
          if (mergeMapOp != null) {
            mergeMapOp.setConf(mergeMapWork);
            l4j.info("Input name is " + mergeMapWork.getName());
            jconf.set(Utilities.INPUT_NAME, mergeMapWork.getName());
            mergeMapOp.initialize(jconf, null);
            mergeMapOp.setChildren(jconf);

            DummyStoreOperator dummyOp = getJoinParentOp(mergeMapOp);
	          mapOp.setConnectedOperators(mergeMapWork.getTag(), dummyOp);

            mergeMapOp.passExecContext(new ExecMapperContext(jconf));
            mergeMapOp.initializeLocalWork(jconf);
          }
        }
      }

      // initialize map operator
      mapOp.setConf(mapWork);
      l4j.info("Main input name is " + mapWork.getName());
      jconf.set(Utilities.INPUT_NAME, mapWork.getName());
      mapOp.initialize(jconf, null);
      mapOp.setChildren(jconf);
      mapOp.passExecContext(execContext);
      l4j.info(mapOp.dump(0));

      mapOp.initializeLocalWork(jconf);

      initializeMapRecordSources();
      mapOp.initializeMapOperator(jconf);
      if ((mergeMapOpList != null) && mergeMapOpList.isEmpty() == false) {
        for (MapOperator mergeMapOp : mergeMapOpList) {
          jconf.set(Utilities.INPUT_NAME, mergeMapOp.getConf().getName());
          mergeMapOp.initializeMapOperator(jconf);
        }
      }

      // Initialization isn't finished until all parents of all operators
      // are initialized. For broadcast joins that means initializing the
      // dummy parent operators as well.
      List<HashTableDummyOperator> dummyOps = mapWork.getDummyOps();
      jconf.set(Utilities.INPUT_NAME, mapWork.getName());
      if (dummyOps != null) {
        for (Operator<? extends OperatorDesc> dummyOp : dummyOps){
          dummyOp.setExecContext(execContext);
          dummyOp.initialize(jconf, null);
        }
      }

      OperatorUtils.setChildrenCollector(mapOp.getChildOperators(), outMap);
      mapOp.setReporter(reporter);
      MapredContext.get().setReporter(reporter);

    } catch (Throwable e) {
      abort = true;
      if (e instanceof OutOfMemoryError) {
        // will this be true here?
        // Don't create a new object if we are already out of memory
        throw (OutOfMemoryError) e;
      } else {
        throw new RuntimeException("Map operator initialization failed", e);
      }
    }
    perfLogger.PerfLogEnd(CLASS_NAME, PerfLogger.TEZ_INIT_OPERATORS);
  }

  private void initializeMapRecordSources() throws Exception {

    int size = mergeMapOpList.size() + 1; // the +1 is for the main map operator itself
    sources = new MapRecordSource[size];
    position = mapOp.getConf().getTag();
    sources[position] = new MapRecordSource();
    KeyValueReader reader = null;
    if (mainWorkMultiMRInput != null) {
      reader = getKeyValueReader(mainWorkMultiMRInput.getKeyValueReaders(), mapOp);
    } else {
      reader = legacyMRInput.getReader();
    }
    sources[position].init(jconf, mapOp, reader);
    for (MapOperator mapOp : mergeMapOpList) {
      int tag = mapOp.getConf().getTag();
      sources[tag] = new MapRecordSource();
      String inputName = mapOp.getConf().getName();
      MultiMRInput multiMRInput = multiMRInputMap.get(inputName);
      Collection<KeyValueReader> kvReaders = multiMRInput.getKeyValueReaders();
      l4j.debug("There are " + kvReaders.size() + " key-value readers for input " + inputName);
      reader = getKeyValueReader(kvReaders, mapOp);
      sources[tag].init(jconf, mapOp, reader);
    }
    ((TezContext) MapredContext.get()).setRecordSources(sources);
  }

  @SuppressWarnings("deprecation")
  private KeyValueReader getKeyValueReader(Collection<KeyValueReader> keyValueReaders,
      MapOperator mapOp)
    throws Exception {
    List<KeyValueReader> kvReaderList = new ArrayList<KeyValueReader>(keyValueReaders);
    // this sets up the map operator contexts correctly
    mapOp.initializeContexts();
    Deserializer deserializer = mapOp.getCurrentDeserializer();
    KeyValueReader reader =
      new KeyValueInputMerger(kvReaderList, deserializer,
          new ObjectInspector[] { deserializer.getObjectInspector() }, mapOp
          .getConf()
          .getSortCols());
    return reader;
  }

  private DummyStoreOperator getJoinParentOp(Operator<? extends OperatorDesc> mergeMapOp) {
    for (Operator<? extends OperatorDesc> childOp : mergeMapOp.getChildOperators()) {
      if ((childOp.getChildOperators() == null) || (childOp.getChildOperators().isEmpty())) {
        return (DummyStoreOperator) childOp;
      } else {
        return getJoinParentOp(childOp);
      }
    }
    return null;
  }

  @Override
  void run() throws Exception {
    while (sources[position].pushRecord()) {}
  }

  @Override
  void close(){
    // check if there are IOExceptions
    if (!abort) {
      abort = execContext.getIoCxt().getIOExceptions();
    }

    if (cache != null && cacheKeys != null) {
      for (String k: cacheKeys) {
        cache.release(k);
      }
    }

    // detecting failed executions by exceptions thrown by the operator tree
    try {
      if (mapOp == null || mapWork == null) {
        return;
      }
      mapOp.close(abort);
      if (mergeMapOpList.isEmpty() == false) {
        for (MapOperator mergeMapOp : mergeMapOpList) {
          mergeMapOp.close(abort);
        }
      }

      // Need to close the dummyOps as well. The operator pipeline
      // is not considered "closed/done" unless all operators are
      // done. For broadcast joins that includes the dummy parents.
      List<HashTableDummyOperator> dummyOps = mapWork.getDummyOps();
      if (dummyOps != null) {
        for (Operator<? extends OperatorDesc> dummyOp : dummyOps){
          dummyOp.close(abort);
        }
      }

      ReportStats rps = new ReportStats(reporter, jconf);
      mapOp.preorderMap(rps);
      return;
    } catch (Exception e) {
      if (!abort) {
        // signal new failure to map-reduce
        l4j.error("Hit error while closing operators - failing tree");
        throw new RuntimeException("Hive Runtime Error while closing operators", e);
      }
    } finally {
      Utilities.clearWorkMap(jconf);
      MapredContext.close();
    }
  }

  private MRInputLegacy getMRInput(Map<String, LogicalInput> inputs) throws Exception {
    // there should be only one MRInput
    MRInputLegacy theMRInput = null;

    // start all mr/multi-mr inputs
    Set<Input> li = new HashSet<Input>();
    for (LogicalInput inp: inputs.values()) {
      if (inp instanceof MRInputLegacy || inp instanceof MultiMRInput) {
        inp.start();
        li.add(inp);
      }
    }
    processorContext.waitForAllInputsReady(li);

    l4j.info("The input names are: " + Arrays.toString(inputs.keySet().toArray()));
    for (Entry<String, LogicalInput> inp : inputs.entrySet()) {
      if (inp.getValue() instanceof MRInputLegacy) {
        if (theMRInput != null) {
          throw new IllegalArgumentException("Only one MRInput is expected");
        }
        // a better logic would be to find the alias
        theMRInput = (MRInputLegacy) inp.getValue();
      } else if (inp.getValue() instanceof MultiMRInput) {
        multiMRInputMap.put(inp.getKey(), (MultiMRInput) inp.getValue());
      }
    }
    if (theMRInput != null) {
      theMRInput.init();
    } else {
      String alias = mapWork.getAliasToWork().keySet().iterator().next();
      if (inputs.get(alias) instanceof MultiMRInput) {
        mainWorkMultiMRInput = (MultiMRInput) inputs.get(alias);
      } else {
        throw new IOException("Unexpected input type found: "
            + inputs.get(alias).getClass().getCanonicalName());
      }
    }
    return theMRInput;
  }
}
