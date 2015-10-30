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

package org.apache.hadoop.hive.llap.cache;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.hive.conf.HiveConf.ConfVars;
import org.apache.hadoop.hive.llap.DebugUtils;
import org.apache.hadoop.hive.llap.cache.LowLevelCache.Priority;
import org.apache.hadoop.hive.llap.io.api.impl.LlapIoImpl;

/**
 * Implementation of the algorithm from "On the Existence of a Spectrum of Policies
 * that Subsumes the Least Recently Used (LRU) and Least Frequently Used (LFU) Policies".
 * Additionally, buffer locking has to be handled (locked buffer cannot be evicted).
 */
public class LowLevelLrfuCachePolicy implements LowLevelCachePolicy {
  private final double lambda;
  private final double f(long x) {
    return Math.pow(0.5, lambda * x);
  }
  private static final double F0 = 1; // f(0) is always 1
  private final double touchPriority(long time, long lastAccess, double previous) {
    return F0 + f(time - lastAccess) * previous;
  }
  private final double expirePriority(long time, long lastAccess, double previous) {
    return f(time - lastAccess) * previous;
  }

  private final AtomicLong timer = new AtomicLong(0);
  /**
   * The heap and list. Currently synchronized on the object, which is not good. If this becomes
   * a problem (which it probably will), we can partition the cache policy, or use some better
   * structure. Heap should not be locked while holding the lock on list.
   * As of now, eviction in most cases will only need the list; locking doesn't do anything;
   * unlocking actually places item in evictable cache - unlocking is done after processing,
   * so this most expensive part (and only access to heap in most cases) will not affect it.
   * Perhaps we should use ConcurrentDoubleLinkedList (in public domain).
   * ONLY LIST REMOVAL is allowed under list lock.
   */
  private final LlapCacheableBuffer[] heap;
  private final ReentrantLock listLock = new ReentrantLock();
  private LlapCacheableBuffer listHead, listTail;
  /** Number of elements. */
  private int heapSize = 0;
  private EvictionListener evictionListener;
  private LlapOomDebugDump parentDebugDump;

  public LowLevelLrfuCachePolicy(Configuration conf) {
    long maxSize = HiveConf.getLongVar(conf, ConfVars.LLAP_ORC_CACHE_MAX_SIZE);
    int minBufferSize = HiveConf.getIntVar(conf, ConfVars.LLAP_ORC_CACHE_MIN_ALLOC);
    lambda = HiveConf.getFloatVar(conf, HiveConf.ConfVars.LLAP_LRFU_LAMBDA);
    int maxBuffers = (int)Math.ceil((maxSize * 1.0) / minBufferSize);
    int maxHeapSize = -1;
    if (lambda == 0) {
      maxHeapSize = maxBuffers; // lrfuThreshold is +inf in this case
    } else {
      int lrfuThreshold = (int)((Math.log(1 - Math.pow(0.5, lambda)) / Math.log(0.5)) / lambda);
      maxHeapSize = Math.min(lrfuThreshold, maxBuffers);
    }
    LlapIoImpl.LOG.info("LRFU cache policy with min buffer size {}", minBufferSize
        , " and lambda {}", lambda, " (heap size {} ", maxHeapSize + ")");


    heap = new LlapCacheableBuffer[maxHeapSize];
    listHead = listTail = null;
  }

  @Override
  public void cache(LlapCacheableBuffer buffer, Priority priority) {
    // LRFU cache policy doesn't store locked blocks. When we cache, the block is locked, so
    // we simply do nothing here. The fact that it was never updated will allow us to add it
    // properly on the first notifyUnlock.
    // We'll do is set priority, to account for the inbound one. No lock - not in heap.
    assert buffer.lastUpdate == -1;
    long time = timer.incrementAndGet();
    buffer.priority = F0;
    buffer.lastUpdate = time;
    if (priority == Priority.HIGH) {
      buffer.priority *= 8; // this is arbitrary
    } else {
      assert priority == Priority.NORMAL;
    }
  }

  @Override
  public void notifyLock(LlapCacheableBuffer buffer) {
    // We do not proactively remove locked items from the heap, and opportunistically try to
    // remove from the list (since eviction is mostly from the list). If eviction stumbles upon
    // a locked item in either, it will remove it from cache; when we unlock, we are going to
    // put it back or update it, depending on whether this has happened. This should cause
    // most of the expensive cache update work to happen in unlock, not blocking processing.
    if (buffer.indexInHeap != LlapCacheableBuffer.IN_LIST) return;
    if (!listLock.tryLock()) return;
    removeFromListAndUnlock(buffer);
  }

  @Override
  public void notifyUnlock(LlapCacheableBuffer buffer) {
    long time = timer.incrementAndGet();
    if (DebugUtils.isTraceCachingEnabled()) {
      LlapIoImpl.LOG.info("Touching " + buffer + " at " + time);
    }
    synchronized (heap) {
      // First, update buffer priority - we have just been using it.
      buffer.priority = (buffer.lastUpdate == -1) ? F0
          : touchPriority(time, buffer.lastUpdate, buffer.priority);
      buffer.lastUpdate = time;
      // Then, if the buffer was in the list, remove it.
      if (buffer.indexInHeap == LlapCacheableBuffer.IN_LIST) {
        listLock.lock();
        removeFromListAndUnlock(buffer);
      }
      // The only concurrent change that can happen when we hold the heap lock is list removal;
      // we have just ensured the item is not in the list, so we have a definite state now.
      if (buffer.indexInHeap >= 0) {
        // The buffer has lived in the heap all along. Restore heap property.
        heapifyDownUnderLock(buffer, time);
      } else if (heapSize == heap.length) {
        // The buffer is not in the (full) heap. Demote the top item of the heap into the list.
        LlapCacheableBuffer demoted = heap[0];
        synchronized (listLock) {
          demoted.indexInHeap = LlapCacheableBuffer.IN_LIST;
          demoted.prev = null;
          if (listHead != null) {
            demoted.next = listHead;
            listHead.prev = demoted;
            listHead = demoted;
          } else {
            listHead = listTail = demoted;
            demoted.next = null;
          }
        }
        // Now insert the buffer in its place and restore heap property.
        buffer.indexInHeap = 0;
        heapifyDownUnderLock(buffer, time);
      } else {
        // Heap is not full, add the buffer to the heap and restore heap property up.
        assert heapSize < heap.length : heap.length + " < " + heapSize;
        buffer.indexInHeap = heapSize;
        heapifyUpUnderLock(buffer, time);
        ++heapSize;
      }
    }
  }

  @Override
  public void setEvictionListener(EvictionListener listener) {
    this.evictionListener = listener;
  }

  @Override
  public void setParentDebugDumper(LlapOomDebugDump dumper) {
    this.parentDebugDump = dumper;
  }


  @Override
  public long evictSomeBlocks(long memoryToReserve) {
    long evicted = 0;
    // In normal case, we evict the items from the list.
    LlapCacheableBuffer nextCandidate, firstCandidate;
    listLock.lock();
    try {
      nextCandidate = firstCandidate = listTail;
      while (evicted < memoryToReserve && nextCandidate != null) {
        if (!nextCandidate.invalidate()) {
          // Locked buffer was in the list - just drop it; will be re-added on unlock.
          LlapCacheableBuffer lockedBuffer = nextCandidate;
          if (firstCandidate == nextCandidate) {
            firstCandidate = nextCandidate.prev;
          }
          nextCandidate = nextCandidate.prev;
          removeFromListUnderLock(lockedBuffer);
          continue;
        }
        // Update the state to removed-from-list, so that parallel notifyUnlock doesn't modify us.
        nextCandidate.indexInHeap = LlapCacheableBuffer.NOT_IN_CACHE;
        evicted += nextCandidate.getMemoryUsage();
        nextCandidate = nextCandidate.prev;
      }
      if (firstCandidate != nextCandidate) {
        if (nextCandidate == null) {
          listHead = listTail = null; // We have evicted the entire list.
        } else {
          // Splice the section that we have evicted out of the list.
          // We have already updated the state above so no need to do that again.
          removeFromListUnderLockNoStateUpdate(nextCandidate.next, firstCandidate);
        }
      }
    } finally {
      listLock.unlock();
    }
    while (firstCandidate != nextCandidate) {
      evictionListener.notifyEvicted(firstCandidate);
      firstCandidate = firstCandidate.prev;
    }
    if (evicted >= memoryToReserve) return evicted;
    // This should not happen unless we are evicting a lot at once, or buffers are large (so
    // there's a small number of buffers and they all live in the heap).
    long time = timer.get();
    while (evicted < memoryToReserve) {
      LlapCacheableBuffer buffer = null;
      synchronized (heap) {
        buffer = evictFromHeapUnderLock(time);
      }
      if (buffer == null) return evicted;
      evicted += buffer.getMemoryUsage();
      evictionListener.notifyEvicted(buffer);
    }
    return evicted;
  }

  private void heapifyUpUnderLock(LlapCacheableBuffer buffer, long time) {
    // See heapifyDown comment.
    int ix = buffer.indexInHeap;
    double priority = buffer.priority;
    while (true) {
      if (ix == 0) break; // Buffer is at the top of the heap.
      int parentIx = (ix - 1) >>> 1;
      LlapCacheableBuffer parent = heap[parentIx];
      double parentPri = getHeapifyPriority(parent, time);
      if (priority >= parentPri) break;
      heap[ix] = parent;
      parent.indexInHeap = ix;
      ix = parentIx;
    }
    buffer.indexInHeap = ix;
    heap[ix] = buffer;
  }

  // Note: almost never called (unless buffers are very large or we evict a lot).
  private LlapCacheableBuffer evictFromHeapUnderLock(long time) {
    while (true) {
      if (heapSize == 0) return null;
      LlapCacheableBuffer result = heap[0];
      if (DebugUtils.isTraceCachingEnabled()) {
        LlapIoImpl.LOG.info("Evicting " + result + " at " + time);
      }
      result.indexInHeap = LlapCacheableBuffer.NOT_IN_CACHE;
      --heapSize;
      boolean canEvict = result.invalidate();
      if (heapSize > 0) {
        LlapCacheableBuffer newRoot = heap[heapSize];
        newRoot.indexInHeap = 0;
        if (newRoot.lastUpdate != time) {
          newRoot.priority = expirePriority(time, newRoot.lastUpdate, newRoot.priority);
          newRoot.lastUpdate = time;
        }
        heapifyDownUnderLock(newRoot, time);
      }
      if (canEvict) return result;
      // Otherwise we just removed a locked item from heap; unlock will re-add it, we continue.
    }
  }

  private void heapifyDownUnderLock(LlapCacheableBuffer buffer, long time) {
    // Relative positions of the blocks don't change over time; priorities we expire can only
    // decrease; we only have one block that could have broken heap rule and we always move it
    // down; therefore, we can update priorities of other blocks as we go for part of the heap -
    // we correct any discrepancy w/the parent after expiring priority, and any block we expire
    // the priority for already has lower priority than that of its children.
    // TODO: avoid expiring priorities if times are close? might be needlessly expensive.
    int ix = buffer.indexInHeap;
    double priority = buffer.priority;
    while (true) {
      int newIx = moveMinChildUp(ix, time, priority);
      if (newIx == -1) break;
      ix = newIx;
    }
    buffer.indexInHeap = ix;
    heap[ix] = buffer;
  }

  /**
   * Moves the minimum child of targetPos block up to targetPos; optionally compares priorities
   * and terminates if targetPos element has lesser value than either of its children.
   * @return the index of the child that was moved up; -1 if nothing was moved due to absence
   *         of the children, or a failed priority check.
   */
  private int moveMinChildUp(int targetPos, long time, double comparePri) {
    int leftIx = (targetPos << 1) + 1, rightIx = leftIx + 1;
    if (leftIx >= heapSize) return -1; // Buffer is at the leaf node.
    LlapCacheableBuffer left = heap[leftIx], right = null;
    if (rightIx < heapSize) {
      right = heap[rightIx];
    }
    double leftPri = getHeapifyPriority(left, time), rightPri = getHeapifyPriority(right, time);
    if (comparePri >= 0 && comparePri <= leftPri && comparePri <= rightPri) {
      return -1;
    }
    if (leftPri <= rightPri) { // prefer left, cause right might be missing
      heap[targetPos] = left;
      left.indexInHeap = targetPos;
      return leftIx;
    } else {
      heap[targetPos] = right;
      right.indexInHeap = targetPos;
      return rightIx;
    }
  }

  private double getHeapifyPriority(LlapCacheableBuffer buf, long time) {
    if (buf == null) return Double.MAX_VALUE;
    if (buf.lastUpdate != time && time >= 0) {
      buf.priority = expirePriority(time, buf.lastUpdate, buf.priority);
      buf.lastUpdate = time;
    }
    return buf.priority;
  }

  private void removeFromListAndUnlock(LlapCacheableBuffer buffer) {
    try {
      if (buffer.indexInHeap != LlapCacheableBuffer.IN_LIST) return;
      removeFromListUnderLock(buffer);
    } finally {
      listLock.unlock();
    }
  }

  private void removeFromListUnderLock(LlapCacheableBuffer buffer) {
    if (buffer == listTail) {
      listTail = buffer.prev;
    } else {
      buffer.next.prev = buffer.prev;
    }
    if (buffer == listHead) {
      listHead = buffer.next;
    } else {
      buffer.prev.next = buffer.next;
    }
    buffer.indexInHeap = LlapCacheableBuffer.NOT_IN_CACHE;
  }

  private void removeFromListUnderLockNoStateUpdate(
      LlapCacheableBuffer from, LlapCacheableBuffer to) {
    if (to == listTail) {
      listTail = from.prev;
    } else {
      to.next.prev = from.prev;
    }
    if (from == listHead) {
      listHead = to.next;
    } else {
      from.prev.next = to.next;
    }
  }

  public String debugDumpHeap() {
    StringBuilder result = new StringBuilder("List: ");
    if (listHead == null) {
      result.append("<empty>");
    } else {
      LlapCacheableBuffer listItem = listHead;
      while (listItem != null) {
        result.append(listItem.toStringForCache()).append(" -> ");
        listItem = listItem.next;
      }
    }
    result.append("\nHeap:");
    if (heapSize == 0) {
      result.append(" <empty>\n");
      return result.toString();
    }
    result.append("\n");
    int levels = 32 - Integer.numberOfLeadingZeros(heapSize);
    int ix = 0;
    int spacesCount = heap[0].toStringForCache().length() + 3;
    String full = StringUtils.repeat(" ", spacesCount),
        half = StringUtils.repeat(" ", spacesCount / 2);
    int maxWidth = 1 << (levels - 1);
    for (int i = 0; i < levels; ++i) {
      int width = 1 << i;
      int middleGap = (maxWidth - width) / width;
      for (int j = 0; j < (middleGap >>> 1); ++j) {
        result.append(full);
      }
      if ((middleGap & 1) == 1) {
        result.append(half);
      }
      for (int j = 0; j < width && ix < heapSize; ++j, ++ix) {
        if (j != 0) {
          for (int k = 0; k < middleGap; ++k) {
            result.append(full);
          }
          if (middleGap == 0) {
            result.append(" ");
          }
        }
        if ((j & 1) == 0) {
          result.append("(");
        }
        result.append(heap[ix].toStringForCache());
        if ((j & 1) == 1) {
          result.append(")");
        }
      }
      result.append("\n");
    }
    return result.toString();
  }

  @Override
  public String debugDumpForOom() {
    String result = debugDumpHeap();
    if (parentDebugDump != null) {
      result += "\n" + parentDebugDump.debugDumpForOom();
    }
    return result;
  }
}
