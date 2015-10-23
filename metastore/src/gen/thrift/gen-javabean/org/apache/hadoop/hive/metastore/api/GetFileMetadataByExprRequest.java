/**
 * Autogenerated by Thrift Compiler (0.9.2)
 *
 * DO NOT EDIT UNLESS YOU ARE SURE THAT YOU KNOW WHAT YOU ARE DOING
 *  @generated
 */
package org.apache.hadoop.hive.metastore.api;

import org.apache.thrift.scheme.IScheme;
import org.apache.thrift.scheme.SchemeFactory;
import org.apache.thrift.scheme.StandardScheme;

import org.apache.thrift.scheme.TupleScheme;
import org.apache.thrift.protocol.TTupleProtocol;
import org.apache.thrift.protocol.TProtocolException;
import org.apache.thrift.EncodingUtils;
import org.apache.thrift.TException;
import org.apache.thrift.async.AsyncMethodCallback;
import org.apache.thrift.server.AbstractNonblockingServer.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.EnumMap;
import java.util.Set;
import java.util.HashSet;
import java.util.EnumSet;
import java.util.Collections;
import java.util.BitSet;
import java.nio.ByteBuffer;
import java.util.Arrays;
import javax.annotation.Generated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"cast", "rawtypes", "serial", "unchecked"})
@Generated(value = "Autogenerated by Thrift Compiler (0.9.2)", date = "2015-10-21")
public class GetFileMetadataByExprRequest implements org.apache.thrift.TBase<GetFileMetadataByExprRequest, GetFileMetadataByExprRequest._Fields>, java.io.Serializable, Cloneable, Comparable<GetFileMetadataByExprRequest> {
  private static final org.apache.thrift.protocol.TStruct STRUCT_DESC = new org.apache.thrift.protocol.TStruct("GetFileMetadataByExprRequest");

  private static final org.apache.thrift.protocol.TField FILE_IDS_FIELD_DESC = new org.apache.thrift.protocol.TField("fileIds", org.apache.thrift.protocol.TType.LIST, (short)1);
  private static final org.apache.thrift.protocol.TField EXPR_FIELD_DESC = new org.apache.thrift.protocol.TField("expr", org.apache.thrift.protocol.TType.STRING, (short)2);
  private static final org.apache.thrift.protocol.TField DO_GET_FOOTERS_FIELD_DESC = new org.apache.thrift.protocol.TField("doGetFooters", org.apache.thrift.protocol.TType.BOOL, (short)3);
  private static final org.apache.thrift.protocol.TField TYPE_FIELD_DESC = new org.apache.thrift.protocol.TField("type", org.apache.thrift.protocol.TType.I32, (short)4);

  private static final Map<Class<? extends IScheme>, SchemeFactory> schemes = new HashMap<Class<? extends IScheme>, SchemeFactory>();
  static {
    schemes.put(StandardScheme.class, new GetFileMetadataByExprRequestStandardSchemeFactory());
    schemes.put(TupleScheme.class, new GetFileMetadataByExprRequestTupleSchemeFactory());
  }

  private List<Long> fileIds; // required
  private ByteBuffer expr; // required
  private boolean doGetFooters; // optional
  private FileMetadataExprType type; // optional

  /** The set of fields this struct contains, along with convenience methods for finding and manipulating them. */
  public enum _Fields implements org.apache.thrift.TFieldIdEnum {
    FILE_IDS((short)1, "fileIds"),
    EXPR((short)2, "expr"),
    DO_GET_FOOTERS((short)3, "doGetFooters"),
    /**
     * 
     * @see FileMetadataExprType
     */
    TYPE((short)4, "type");

    private static final Map<String, _Fields> byName = new HashMap<String, _Fields>();

    static {
      for (_Fields field : EnumSet.allOf(_Fields.class)) {
        byName.put(field.getFieldName(), field);
      }
    }

    /**
     * Find the _Fields constant that matches fieldId, or null if its not found.
     */
    public static _Fields findByThriftId(int fieldId) {
      switch(fieldId) {
        case 1: // FILE_IDS
          return FILE_IDS;
        case 2: // EXPR
          return EXPR;
        case 3: // DO_GET_FOOTERS
          return DO_GET_FOOTERS;
        case 4: // TYPE
          return TYPE;
        default:
          return null;
      }
    }

    /**
     * Find the _Fields constant that matches fieldId, throwing an exception
     * if it is not found.
     */
    public static _Fields findByThriftIdOrThrow(int fieldId) {
      _Fields fields = findByThriftId(fieldId);
      if (fields == null) throw new IllegalArgumentException("Field " + fieldId + " doesn't exist!");
      return fields;
    }

    /**
     * Find the _Fields constant that matches name, or null if its not found.
     */
    public static _Fields findByName(String name) {
      return byName.get(name);
    }

    private final short _thriftId;
    private final String _fieldName;

    _Fields(short thriftId, String fieldName) {
      _thriftId = thriftId;
      _fieldName = fieldName;
    }

    public short getThriftFieldId() {
      return _thriftId;
    }

    public String getFieldName() {
      return _fieldName;
    }
  }

  // isset id assignments
  private static final int __DOGETFOOTERS_ISSET_ID = 0;
  private byte __isset_bitfield = 0;
  private static final _Fields optionals[] = {_Fields.DO_GET_FOOTERS,_Fields.TYPE};
  public static final Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> metaDataMap;
  static {
    Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> tmpMap = new EnumMap<_Fields, org.apache.thrift.meta_data.FieldMetaData>(_Fields.class);
    tmpMap.put(_Fields.FILE_IDS, new org.apache.thrift.meta_data.FieldMetaData("fileIds", org.apache.thrift.TFieldRequirementType.REQUIRED, 
        new org.apache.thrift.meta_data.ListMetaData(org.apache.thrift.protocol.TType.LIST, 
            new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.I64))));
    tmpMap.put(_Fields.EXPR, new org.apache.thrift.meta_data.FieldMetaData("expr", org.apache.thrift.TFieldRequirementType.REQUIRED, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING        , true)));
    tmpMap.put(_Fields.DO_GET_FOOTERS, new org.apache.thrift.meta_data.FieldMetaData("doGetFooters", org.apache.thrift.TFieldRequirementType.OPTIONAL, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.BOOL)));
    tmpMap.put(_Fields.TYPE, new org.apache.thrift.meta_data.FieldMetaData("type", org.apache.thrift.TFieldRequirementType.OPTIONAL, 
        new org.apache.thrift.meta_data.EnumMetaData(org.apache.thrift.protocol.TType.ENUM, FileMetadataExprType.class)));
    metaDataMap = Collections.unmodifiableMap(tmpMap);
    org.apache.thrift.meta_data.FieldMetaData.addStructMetaDataMap(GetFileMetadataByExprRequest.class, metaDataMap);
  }

  public GetFileMetadataByExprRequest() {
  }

  public GetFileMetadataByExprRequest(
    List<Long> fileIds,
    ByteBuffer expr)
  {
    this();
    this.fileIds = fileIds;
    this.expr = org.apache.thrift.TBaseHelper.copyBinary(expr);
  }

  /**
   * Performs a deep copy on <i>other</i>.
   */
  public GetFileMetadataByExprRequest(GetFileMetadataByExprRequest other) {
    __isset_bitfield = other.__isset_bitfield;
    if (other.isSetFileIds()) {
      List<Long> __this__fileIds = new ArrayList<Long>(other.fileIds);
      this.fileIds = __this__fileIds;
    }
    if (other.isSetExpr()) {
      this.expr = org.apache.thrift.TBaseHelper.copyBinary(other.expr);
    }
    this.doGetFooters = other.doGetFooters;
    if (other.isSetType()) {
      this.type = other.type;
    }
  }

  public GetFileMetadataByExprRequest deepCopy() {
    return new GetFileMetadataByExprRequest(this);
  }

  @Override
  public void clear() {
    this.fileIds = null;
    this.expr = null;
    setDoGetFootersIsSet(false);
    this.doGetFooters = false;
    this.type = null;
  }

  public int getFileIdsSize() {
    return (this.fileIds == null) ? 0 : this.fileIds.size();
  }

  public java.util.Iterator<Long> getFileIdsIterator() {
    return (this.fileIds == null) ? null : this.fileIds.iterator();
  }

  public void addToFileIds(long elem) {
    if (this.fileIds == null) {
      this.fileIds = new ArrayList<Long>();
    }
    this.fileIds.add(elem);
  }

  public List<Long> getFileIds() {
    return this.fileIds;
  }

  public void setFileIds(List<Long> fileIds) {
    this.fileIds = fileIds;
  }

  public void unsetFileIds() {
    this.fileIds = null;
  }

  /** Returns true if field fileIds is set (has been assigned a value) and false otherwise */
  public boolean isSetFileIds() {
    return this.fileIds != null;
  }

  public void setFileIdsIsSet(boolean value) {
    if (!value) {
      this.fileIds = null;
    }
  }

  public byte[] getExpr() {
    setExpr(org.apache.thrift.TBaseHelper.rightSize(expr));
    return expr == null ? null : expr.array();
  }

  public ByteBuffer bufferForExpr() {
    return org.apache.thrift.TBaseHelper.copyBinary(expr);
  }

  public void setExpr(byte[] expr) {
    this.expr = expr == null ? (ByteBuffer)null : ByteBuffer.wrap(Arrays.copyOf(expr, expr.length));
  }

  public void setExpr(ByteBuffer expr) {
    this.expr = org.apache.thrift.TBaseHelper.copyBinary(expr);
  }

  public void unsetExpr() {
    this.expr = null;
  }

  /** Returns true if field expr is set (has been assigned a value) and false otherwise */
  public boolean isSetExpr() {
    return this.expr != null;
  }

  public void setExprIsSet(boolean value) {
    if (!value) {
      this.expr = null;
    }
  }

  public boolean isDoGetFooters() {
    return this.doGetFooters;
  }

  public void setDoGetFooters(boolean doGetFooters) {
    this.doGetFooters = doGetFooters;
    setDoGetFootersIsSet(true);
  }

  public void unsetDoGetFooters() {
    __isset_bitfield = EncodingUtils.clearBit(__isset_bitfield, __DOGETFOOTERS_ISSET_ID);
  }

  /** Returns true if field doGetFooters is set (has been assigned a value) and false otherwise */
  public boolean isSetDoGetFooters() {
    return EncodingUtils.testBit(__isset_bitfield, __DOGETFOOTERS_ISSET_ID);
  }

  public void setDoGetFootersIsSet(boolean value) {
    __isset_bitfield = EncodingUtils.setBit(__isset_bitfield, __DOGETFOOTERS_ISSET_ID, value);
  }

  /**
   * 
   * @see FileMetadataExprType
   */
  public FileMetadataExprType getType() {
    return this.type;
  }

  /**
   * 
   * @see FileMetadataExprType
   */
  public void setType(FileMetadataExprType type) {
    this.type = type;
  }

  public void unsetType() {
    this.type = null;
  }

  /** Returns true if field type is set (has been assigned a value) and false otherwise */
  public boolean isSetType() {
    return this.type != null;
  }

  public void setTypeIsSet(boolean value) {
    if (!value) {
      this.type = null;
    }
  }

  public void setFieldValue(_Fields field, Object value) {
    switch (field) {
    case FILE_IDS:
      if (value == null) {
        unsetFileIds();
      } else {
        setFileIds((List<Long>)value);
      }
      break;

    case EXPR:
      if (value == null) {
        unsetExpr();
      } else {
        setExpr((ByteBuffer)value);
      }
      break;

    case DO_GET_FOOTERS:
      if (value == null) {
        unsetDoGetFooters();
      } else {
        setDoGetFooters((Boolean)value);
      }
      break;

    case TYPE:
      if (value == null) {
        unsetType();
      } else {
        setType((FileMetadataExprType)value);
      }
      break;

    }
  }

  public Object getFieldValue(_Fields field) {
    switch (field) {
    case FILE_IDS:
      return getFileIds();

    case EXPR:
      return getExpr();

    case DO_GET_FOOTERS:
      return Boolean.valueOf(isDoGetFooters());

    case TYPE:
      return getType();

    }
    throw new IllegalStateException();
  }

  /** Returns true if field corresponding to fieldID is set (has been assigned a value) and false otherwise */
  public boolean isSet(_Fields field) {
    if (field == null) {
      throw new IllegalArgumentException();
    }

    switch (field) {
    case FILE_IDS:
      return isSetFileIds();
    case EXPR:
      return isSetExpr();
    case DO_GET_FOOTERS:
      return isSetDoGetFooters();
    case TYPE:
      return isSetType();
    }
    throw new IllegalStateException();
  }

  @Override
  public boolean equals(Object that) {
    if (that == null)
      return false;
    if (that instanceof GetFileMetadataByExprRequest)
      return this.equals((GetFileMetadataByExprRequest)that);
    return false;
  }

  public boolean equals(GetFileMetadataByExprRequest that) {
    if (that == null)
      return false;

    boolean this_present_fileIds = true && this.isSetFileIds();
    boolean that_present_fileIds = true && that.isSetFileIds();
    if (this_present_fileIds || that_present_fileIds) {
      if (!(this_present_fileIds && that_present_fileIds))
        return false;
      if (!this.fileIds.equals(that.fileIds))
        return false;
    }

    boolean this_present_expr = true && this.isSetExpr();
    boolean that_present_expr = true && that.isSetExpr();
    if (this_present_expr || that_present_expr) {
      if (!(this_present_expr && that_present_expr))
        return false;
      if (!this.expr.equals(that.expr))
        return false;
    }

    boolean this_present_doGetFooters = true && this.isSetDoGetFooters();
    boolean that_present_doGetFooters = true && that.isSetDoGetFooters();
    if (this_present_doGetFooters || that_present_doGetFooters) {
      if (!(this_present_doGetFooters && that_present_doGetFooters))
        return false;
      if (this.doGetFooters != that.doGetFooters)
        return false;
    }

    boolean this_present_type = true && this.isSetType();
    boolean that_present_type = true && that.isSetType();
    if (this_present_type || that_present_type) {
      if (!(this_present_type && that_present_type))
        return false;
      if (!this.type.equals(that.type))
        return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    List<Object> list = new ArrayList<Object>();

    boolean present_fileIds = true && (isSetFileIds());
    list.add(present_fileIds);
    if (present_fileIds)
      list.add(fileIds);

    boolean present_expr = true && (isSetExpr());
    list.add(present_expr);
    if (present_expr)
      list.add(expr);

    boolean present_doGetFooters = true && (isSetDoGetFooters());
    list.add(present_doGetFooters);
    if (present_doGetFooters)
      list.add(doGetFooters);

    boolean present_type = true && (isSetType());
    list.add(present_type);
    if (present_type)
      list.add(type.getValue());

    return list.hashCode();
  }

  @Override
  public int compareTo(GetFileMetadataByExprRequest other) {
    if (!getClass().equals(other.getClass())) {
      return getClass().getName().compareTo(other.getClass().getName());
    }

    int lastComparison = 0;

    lastComparison = Boolean.valueOf(isSetFileIds()).compareTo(other.isSetFileIds());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetFileIds()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.fileIds, other.fileIds);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetExpr()).compareTo(other.isSetExpr());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetExpr()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.expr, other.expr);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetDoGetFooters()).compareTo(other.isSetDoGetFooters());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetDoGetFooters()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.doGetFooters, other.doGetFooters);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetType()).compareTo(other.isSetType());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetType()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.type, other.type);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    return 0;
  }

  public _Fields fieldForId(int fieldId) {
    return _Fields.findByThriftId(fieldId);
  }

  public void read(org.apache.thrift.protocol.TProtocol iprot) throws org.apache.thrift.TException {
    schemes.get(iprot.getScheme()).getScheme().read(iprot, this);
  }

  public void write(org.apache.thrift.protocol.TProtocol oprot) throws org.apache.thrift.TException {
    schemes.get(oprot.getScheme()).getScheme().write(oprot, this);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("GetFileMetadataByExprRequest(");
    boolean first = true;

    sb.append("fileIds:");
    if (this.fileIds == null) {
      sb.append("null");
    } else {
      sb.append(this.fileIds);
    }
    first = false;
    if (!first) sb.append(", ");
    sb.append("expr:");
    if (this.expr == null) {
      sb.append("null");
    } else {
      org.apache.thrift.TBaseHelper.toString(this.expr, sb);
    }
    first = false;
    if (isSetDoGetFooters()) {
      if (!first) sb.append(", ");
      sb.append("doGetFooters:");
      sb.append(this.doGetFooters);
      first = false;
    }
    if (isSetType()) {
      if (!first) sb.append(", ");
      sb.append("type:");
      if (this.type == null) {
        sb.append("null");
      } else {
        sb.append(this.type);
      }
      first = false;
    }
    sb.append(")");
    return sb.toString();
  }

  public void validate() throws org.apache.thrift.TException {
    // check for required fields
    if (!isSetFileIds()) {
      throw new org.apache.thrift.protocol.TProtocolException("Required field 'fileIds' is unset! Struct:" + toString());
    }

    if (!isSetExpr()) {
      throw new org.apache.thrift.protocol.TProtocolException("Required field 'expr' is unset! Struct:" + toString());
    }

    // check for sub-struct validity
  }

  private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {
    try {
      write(new org.apache.thrift.protocol.TCompactProtocol(new org.apache.thrift.transport.TIOStreamTransport(out)));
    } catch (org.apache.thrift.TException te) {
      throw new java.io.IOException(te);
    }
  }

  private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, ClassNotFoundException {
    try {
      // it doesn't seem like you should have to do this, but java serialization is wacky, and doesn't call the default constructor.
      __isset_bitfield = 0;
      read(new org.apache.thrift.protocol.TCompactProtocol(new org.apache.thrift.transport.TIOStreamTransport(in)));
    } catch (org.apache.thrift.TException te) {
      throw new java.io.IOException(te);
    }
  }

  private static class GetFileMetadataByExprRequestStandardSchemeFactory implements SchemeFactory {
    public GetFileMetadataByExprRequestStandardScheme getScheme() {
      return new GetFileMetadataByExprRequestStandardScheme();
    }
  }

  private static class GetFileMetadataByExprRequestStandardScheme extends StandardScheme<GetFileMetadataByExprRequest> {

    public void read(org.apache.thrift.protocol.TProtocol iprot, GetFileMetadataByExprRequest struct) throws org.apache.thrift.TException {
      org.apache.thrift.protocol.TField schemeField;
      iprot.readStructBegin();
      while (true)
      {
        schemeField = iprot.readFieldBegin();
        if (schemeField.type == org.apache.thrift.protocol.TType.STOP) { 
          break;
        }
        switch (schemeField.id) {
          case 1: // FILE_IDS
            if (schemeField.type == org.apache.thrift.protocol.TType.LIST) {
              {
                org.apache.thrift.protocol.TList _list534 = iprot.readListBegin();
                struct.fileIds = new ArrayList<Long>(_list534.size);
                long _elem535;
                for (int _i536 = 0; _i536 < _list534.size; ++_i536)
                {
                  _elem535 = iprot.readI64();
                  struct.fileIds.add(_elem535);
                }
                iprot.readListEnd();
              }
              struct.setFileIdsIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 2: // EXPR
            if (schemeField.type == org.apache.thrift.protocol.TType.STRING) {
              struct.expr = iprot.readBinary();
              struct.setExprIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 3: // DO_GET_FOOTERS
            if (schemeField.type == org.apache.thrift.protocol.TType.BOOL) {
              struct.doGetFooters = iprot.readBool();
              struct.setDoGetFootersIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 4: // TYPE
            if (schemeField.type == org.apache.thrift.protocol.TType.I32) {
              struct.type = org.apache.hadoop.hive.metastore.api.FileMetadataExprType.findByValue(iprot.readI32());
              struct.setTypeIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          default:
            org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
        }
        iprot.readFieldEnd();
      }
      iprot.readStructEnd();
      struct.validate();
    }

    public void write(org.apache.thrift.protocol.TProtocol oprot, GetFileMetadataByExprRequest struct) throws org.apache.thrift.TException {
      struct.validate();

      oprot.writeStructBegin(STRUCT_DESC);
      if (struct.fileIds != null) {
        oprot.writeFieldBegin(FILE_IDS_FIELD_DESC);
        {
          oprot.writeListBegin(new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.I64, struct.fileIds.size()));
          for (long _iter537 : struct.fileIds)
          {
            oprot.writeI64(_iter537);
          }
          oprot.writeListEnd();
        }
        oprot.writeFieldEnd();
      }
      if (struct.expr != null) {
        oprot.writeFieldBegin(EXPR_FIELD_DESC);
        oprot.writeBinary(struct.expr);
        oprot.writeFieldEnd();
      }
      if (struct.isSetDoGetFooters()) {
        oprot.writeFieldBegin(DO_GET_FOOTERS_FIELD_DESC);
        oprot.writeBool(struct.doGetFooters);
        oprot.writeFieldEnd();
      }
      if (struct.type != null) {
        if (struct.isSetType()) {
          oprot.writeFieldBegin(TYPE_FIELD_DESC);
          oprot.writeI32(struct.type.getValue());
          oprot.writeFieldEnd();
        }
      }
      oprot.writeFieldStop();
      oprot.writeStructEnd();
    }

  }

  private static class GetFileMetadataByExprRequestTupleSchemeFactory implements SchemeFactory {
    public GetFileMetadataByExprRequestTupleScheme getScheme() {
      return new GetFileMetadataByExprRequestTupleScheme();
    }
  }

  private static class GetFileMetadataByExprRequestTupleScheme extends TupleScheme<GetFileMetadataByExprRequest> {

    @Override
    public void write(org.apache.thrift.protocol.TProtocol prot, GetFileMetadataByExprRequest struct) throws org.apache.thrift.TException {
      TTupleProtocol oprot = (TTupleProtocol) prot;
      {
        oprot.writeI32(struct.fileIds.size());
        for (long _iter538 : struct.fileIds)
        {
          oprot.writeI64(_iter538);
        }
      }
      oprot.writeBinary(struct.expr);
      BitSet optionals = new BitSet();
      if (struct.isSetDoGetFooters()) {
        optionals.set(0);
      }
      if (struct.isSetType()) {
        optionals.set(1);
      }
      oprot.writeBitSet(optionals, 2);
      if (struct.isSetDoGetFooters()) {
        oprot.writeBool(struct.doGetFooters);
      }
      if (struct.isSetType()) {
        oprot.writeI32(struct.type.getValue());
      }
    }

    @Override
    public void read(org.apache.thrift.protocol.TProtocol prot, GetFileMetadataByExprRequest struct) throws org.apache.thrift.TException {
      TTupleProtocol iprot = (TTupleProtocol) prot;
      {
        org.apache.thrift.protocol.TList _list539 = new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.I64, iprot.readI32());
        struct.fileIds = new ArrayList<Long>(_list539.size);
        long _elem540;
        for (int _i541 = 0; _i541 < _list539.size; ++_i541)
        {
          _elem540 = iprot.readI64();
          struct.fileIds.add(_elem540);
        }
      }
      struct.setFileIdsIsSet(true);
      struct.expr = iprot.readBinary();
      struct.setExprIsSet(true);
      BitSet incoming = iprot.readBitSet(2);
      if (incoming.get(0)) {
        struct.doGetFooters = iprot.readBool();
        struct.setDoGetFootersIsSet(true);
      }
      if (incoming.get(1)) {
        struct.type = org.apache.hadoop.hive.metastore.api.FileMetadataExprType.findByValue(iprot.readI32());
        struct.setTypeIsSet(true);
      }
    }
  }

}

