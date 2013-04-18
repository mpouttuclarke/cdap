/**
 * Autogenerated by Thrift Compiler (0.8.0)
 *
 * DO NOT EDIT UNLESS YOU ARE SURE THAT YOU KNOW WHAT YOU ARE DOING
 *  @generated
 */
package com.continuuity.data.operation.executor.remote.stubs;

import org.apache.thrift.scheme.IScheme;
import org.apache.thrift.scheme.SchemeFactory;
import org.apache.thrift.scheme.StandardScheme;

import org.apache.thrift.scheme.TupleScheme;
import org.apache.thrift.protocol.TTupleProtocol;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TQueueConsumer implements org.apache.thrift.TBase<TQueueConsumer, TQueueConsumer._Fields>, java.io.Serializable, Cloneable {
  private static final org.apache.thrift.protocol.TStruct STRUCT_DESC = new org.apache.thrift.protocol.TStruct("TQueueConsumer");

  private static final org.apache.thrift.protocol.TField INSTANCE_ID_FIELD_DESC = new org.apache.thrift.protocol.TField("instanceId", org.apache.thrift.protocol.TType.I32, (short)1);
  private static final org.apache.thrift.protocol.TField GROUP_ID_FIELD_DESC = new org.apache.thrift.protocol.TField("groupId", org.apache.thrift.protocol.TType.I64, (short)2);
  private static final org.apache.thrift.protocol.TField GROUP_SIZE_FIELD_DESC = new org.apache.thrift.protocol.TField("groupSize", org.apache.thrift.protocol.TType.I32, (short)3);
  private static final org.apache.thrift.protocol.TField GROUP_NAME_FIELD_DESC = new org.apache.thrift.protocol.TField("groupName", org.apache.thrift.protocol.TType.STRING, (short)4);
  private static final org.apache.thrift.protocol.TField PARTITIONING_KEY_FIELD_DESC = new org.apache.thrift.protocol.TField("partitioningKey", org.apache.thrift.protocol.TType.STRING, (short)5);
  private static final org.apache.thrift.protocol.TField QUEUE_CONFIG_FIELD_DESC = new org.apache.thrift.protocol.TField("queueConfig", org.apache.thrift.protocol.TType.STRUCT, (short)6);
  private static final org.apache.thrift.protocol.TField IS_STATEFUL_FIELD_DESC = new org.apache.thrift.protocol.TField("isStateful", org.apache.thrift.protocol.TType.BOOL, (short)7);
  private static final org.apache.thrift.protocol.TField QUEUE_STATE_FIELD_DESC = new org.apache.thrift.protocol.TField("queueState", org.apache.thrift.protocol.TType.STRING, (short)8);

  private static final Map<Class<? extends IScheme>, SchemeFactory> schemes = new HashMap<Class<? extends IScheme>, SchemeFactory>();
  static {
    schemes.put(StandardScheme.class, new TQueueConsumerStandardSchemeFactory());
    schemes.put(TupleScheme.class, new TQueueConsumerTupleSchemeFactory());
  }

  public int instanceId; // required
  public long groupId; // required
  public int groupSize; // required
  public String groupName; // optional
  public String partitioningKey; // optional
  public TQueueConfig queueConfig; // optional
  public boolean isStateful; // required
  public ByteBuffer queueState; // optional

  /** The set of fields this struct contains, along with convenience methods for finding and manipulating them. */
  public enum _Fields implements org.apache.thrift.TFieldIdEnum {
    INSTANCE_ID((short)1, "instanceId"),
    GROUP_ID((short)2, "groupId"),
    GROUP_SIZE((short)3, "groupSize"),
    GROUP_NAME((short)4, "groupName"),
    PARTITIONING_KEY((short)5, "partitioningKey"),
    QUEUE_CONFIG((short)6, "queueConfig"),
    IS_STATEFUL((short)7, "isStateful"),
    QUEUE_STATE((short)8, "queueState");

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
        case 1: // INSTANCE_ID
          return INSTANCE_ID;
        case 2: // GROUP_ID
          return GROUP_ID;
        case 3: // GROUP_SIZE
          return GROUP_SIZE;
        case 4: // GROUP_NAME
          return GROUP_NAME;
        case 5: // PARTITIONING_KEY
          return PARTITIONING_KEY;
        case 6: // QUEUE_CONFIG
          return QUEUE_CONFIG;
        case 7: // IS_STATEFUL
          return IS_STATEFUL;
        case 8: // QUEUE_STATE
          return QUEUE_STATE;
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
  private static final int __INSTANCEID_ISSET_ID = 0;
  private static final int __GROUPID_ISSET_ID = 1;
  private static final int __GROUPSIZE_ISSET_ID = 2;
  private static final int __ISSTATEFUL_ISSET_ID = 3;
  private BitSet __isset_bit_vector = new BitSet(4);
  private _Fields optionals[] = {_Fields.GROUP_NAME,_Fields.PARTITIONING_KEY,_Fields.QUEUE_CONFIG,_Fields.QUEUE_STATE};
  public static final Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> metaDataMap;
  static {
    Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> tmpMap = new EnumMap<_Fields, org.apache.thrift.meta_data.FieldMetaData>(_Fields.class);
    tmpMap.put(_Fields.INSTANCE_ID, new org.apache.thrift.meta_data.FieldMetaData("instanceId", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.I32)));
    tmpMap.put(_Fields.GROUP_ID, new org.apache.thrift.meta_data.FieldMetaData("groupId", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.I64)));
    tmpMap.put(_Fields.GROUP_SIZE, new org.apache.thrift.meta_data.FieldMetaData("groupSize", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.I32)));
    tmpMap.put(_Fields.GROUP_NAME, new org.apache.thrift.meta_data.FieldMetaData("groupName", org.apache.thrift.TFieldRequirementType.OPTIONAL, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING)));
    tmpMap.put(_Fields.PARTITIONING_KEY, new org.apache.thrift.meta_data.FieldMetaData("partitioningKey", org.apache.thrift.TFieldRequirementType.OPTIONAL, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING)));
    tmpMap.put(_Fields.QUEUE_CONFIG, new org.apache.thrift.meta_data.FieldMetaData("queueConfig", org.apache.thrift.TFieldRequirementType.OPTIONAL, 
        new org.apache.thrift.meta_data.StructMetaData(org.apache.thrift.protocol.TType.STRUCT, TQueueConfig.class)));
    tmpMap.put(_Fields.IS_STATEFUL, new org.apache.thrift.meta_data.FieldMetaData("isStateful", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.BOOL)));
    tmpMap.put(_Fields.QUEUE_STATE, new org.apache.thrift.meta_data.FieldMetaData("queueState", org.apache.thrift.TFieldRequirementType.OPTIONAL, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING        , true)));
    metaDataMap = Collections.unmodifiableMap(tmpMap);
    org.apache.thrift.meta_data.FieldMetaData.addStructMetaDataMap(TQueueConsumer.class, metaDataMap);
  }

  public TQueueConsumer() {
  }

  public TQueueConsumer(
    int instanceId,
    long groupId,
    int groupSize,
    boolean isStateful)
  {
    this();
    this.instanceId = instanceId;
    setInstanceIdIsSet(true);
    this.groupId = groupId;
    setGroupIdIsSet(true);
    this.groupSize = groupSize;
    setGroupSizeIsSet(true);
    this.isStateful = isStateful;
    setIsStatefulIsSet(true);
  }

  /**
   * Performs a deep copy on <i>other</i>.
   */
  public TQueueConsumer(TQueueConsumer other) {
    __isset_bit_vector.clear();
    __isset_bit_vector.or(other.__isset_bit_vector);
    this.instanceId = other.instanceId;
    this.groupId = other.groupId;
    this.groupSize = other.groupSize;
    if (other.isSetGroupName()) {
      this.groupName = other.groupName;
    }
    if (other.isSetPartitioningKey()) {
      this.partitioningKey = other.partitioningKey;
    }
    if (other.isSetQueueConfig()) {
      this.queueConfig = new TQueueConfig(other.queueConfig);
    }
    this.isStateful = other.isStateful;
    if (other.isSetQueueState()) {
      this.queueState = org.apache.thrift.TBaseHelper.copyBinary(other.queueState);
;
    }
  }

  public TQueueConsumer deepCopy() {
    return new TQueueConsumer(this);
  }

  @Override
  public void clear() {
    setInstanceIdIsSet(false);
    this.instanceId = 0;
    setGroupIdIsSet(false);
    this.groupId = 0;
    setGroupSizeIsSet(false);
    this.groupSize = 0;
    this.groupName = null;
    this.partitioningKey = null;
    this.queueConfig = null;
    setIsStatefulIsSet(false);
    this.isStateful = false;
    this.queueState = null;
  }

  public int getInstanceId() {
    return this.instanceId;
  }

  public TQueueConsumer setInstanceId(int instanceId) {
    this.instanceId = instanceId;
    setInstanceIdIsSet(true);
    return this;
  }

  public void unsetInstanceId() {
    __isset_bit_vector.clear(__INSTANCEID_ISSET_ID);
  }

  /** Returns true if field instanceId is set (has been assigned a value) and false otherwise */
  public boolean isSetInstanceId() {
    return __isset_bit_vector.get(__INSTANCEID_ISSET_ID);
  }

  public void setInstanceIdIsSet(boolean value) {
    __isset_bit_vector.set(__INSTANCEID_ISSET_ID, value);
  }

  public long getGroupId() {
    return this.groupId;
  }

  public TQueueConsumer setGroupId(long groupId) {
    this.groupId = groupId;
    setGroupIdIsSet(true);
    return this;
  }

  public void unsetGroupId() {
    __isset_bit_vector.clear(__GROUPID_ISSET_ID);
  }

  /** Returns true if field groupId is set (has been assigned a value) and false otherwise */
  public boolean isSetGroupId() {
    return __isset_bit_vector.get(__GROUPID_ISSET_ID);
  }

  public void setGroupIdIsSet(boolean value) {
    __isset_bit_vector.set(__GROUPID_ISSET_ID, value);
  }

  public int getGroupSize() {
    return this.groupSize;
  }

  public TQueueConsumer setGroupSize(int groupSize) {
    this.groupSize = groupSize;
    setGroupSizeIsSet(true);
    return this;
  }

  public void unsetGroupSize() {
    __isset_bit_vector.clear(__GROUPSIZE_ISSET_ID);
  }

  /** Returns true if field groupSize is set (has been assigned a value) and false otherwise */
  public boolean isSetGroupSize() {
    return __isset_bit_vector.get(__GROUPSIZE_ISSET_ID);
  }

  public void setGroupSizeIsSet(boolean value) {
    __isset_bit_vector.set(__GROUPSIZE_ISSET_ID, value);
  }

  public String getGroupName() {
    return this.groupName;
  }

  public TQueueConsumer setGroupName(String groupName) {
    this.groupName = groupName;
    return this;
  }

  public void unsetGroupName() {
    this.groupName = null;
  }

  /** Returns true if field groupName is set (has been assigned a value) and false otherwise */
  public boolean isSetGroupName() {
    return this.groupName != null;
  }

  public void setGroupNameIsSet(boolean value) {
    if (!value) {
      this.groupName = null;
    }
  }

  public String getPartitioningKey() {
    return this.partitioningKey;
  }

  public TQueueConsumer setPartitioningKey(String partitioningKey) {
    this.partitioningKey = partitioningKey;
    return this;
  }

  public void unsetPartitioningKey() {
    this.partitioningKey = null;
  }

  /** Returns true if field partitioningKey is set (has been assigned a value) and false otherwise */
  public boolean isSetPartitioningKey() {
    return this.partitioningKey != null;
  }

  public void setPartitioningKeyIsSet(boolean value) {
    if (!value) {
      this.partitioningKey = null;
    }
  }

  public TQueueConfig getQueueConfig() {
    return this.queueConfig;
  }

  public TQueueConsumer setQueueConfig(TQueueConfig queueConfig) {
    this.queueConfig = queueConfig;
    return this;
  }

  public void unsetQueueConfig() {
    this.queueConfig = null;
  }

  /** Returns true if field queueConfig is set (has been assigned a value) and false otherwise */
  public boolean isSetQueueConfig() {
    return this.queueConfig != null;
  }

  public void setQueueConfigIsSet(boolean value) {
    if (!value) {
      this.queueConfig = null;
    }
  }

  public boolean isIsStateful() {
    return this.isStateful;
  }

  public TQueueConsumer setIsStateful(boolean isStateful) {
    this.isStateful = isStateful;
    setIsStatefulIsSet(true);
    return this;
  }

  public void unsetIsStateful() {
    __isset_bit_vector.clear(__ISSTATEFUL_ISSET_ID);
  }

  /** Returns true if field isStateful is set (has been assigned a value) and false otherwise */
  public boolean isSetIsStateful() {
    return __isset_bit_vector.get(__ISSTATEFUL_ISSET_ID);
  }

  public void setIsStatefulIsSet(boolean value) {
    __isset_bit_vector.set(__ISSTATEFUL_ISSET_ID, value);
  }

  public byte[] getQueueState() {
    setQueueState(org.apache.thrift.TBaseHelper.rightSize(queueState));
    return queueState == null ? null : queueState.array();
  }

  public ByteBuffer bufferForQueueState() {
    return queueState;
  }

  public TQueueConsumer setQueueState(byte[] queueState) {
    setQueueState(queueState == null ? (ByteBuffer)null : ByteBuffer.wrap(queueState));
    return this;
  }

  public TQueueConsumer setQueueState(ByteBuffer queueState) {
    this.queueState = queueState;
    return this;
  }

  public void unsetQueueState() {
    this.queueState = null;
  }

  /** Returns true if field queueState is set (has been assigned a value) and false otherwise */
  public boolean isSetQueueState() {
    return this.queueState != null;
  }

  public void setQueueStateIsSet(boolean value) {
    if (!value) {
      this.queueState = null;
    }
  }

  public void setFieldValue(_Fields field, Object value) {
    switch (field) {
    case INSTANCE_ID:
      if (value == null) {
        unsetInstanceId();
      } else {
        setInstanceId((Integer)value);
      }
      break;

    case GROUP_ID:
      if (value == null) {
        unsetGroupId();
      } else {
        setGroupId((Long)value);
      }
      break;

    case GROUP_SIZE:
      if (value == null) {
        unsetGroupSize();
      } else {
        setGroupSize((Integer)value);
      }
      break;

    case GROUP_NAME:
      if (value == null) {
        unsetGroupName();
      } else {
        setGroupName((String)value);
      }
      break;

    case PARTITIONING_KEY:
      if (value == null) {
        unsetPartitioningKey();
      } else {
        setPartitioningKey((String)value);
      }
      break;

    case QUEUE_CONFIG:
      if (value == null) {
        unsetQueueConfig();
      } else {
        setQueueConfig((TQueueConfig)value);
      }
      break;

    case IS_STATEFUL:
      if (value == null) {
        unsetIsStateful();
      } else {
        setIsStateful((Boolean)value);
      }
      break;

    case QUEUE_STATE:
      if (value == null) {
        unsetQueueState();
      } else {
        setQueueState((ByteBuffer)value);
      }
      break;

    }
  }

  public Object getFieldValue(_Fields field) {
    switch (field) {
    case INSTANCE_ID:
      return Integer.valueOf(getInstanceId());

    case GROUP_ID:
      return Long.valueOf(getGroupId());

    case GROUP_SIZE:
      return Integer.valueOf(getGroupSize());

    case GROUP_NAME:
      return getGroupName();

    case PARTITIONING_KEY:
      return getPartitioningKey();

    case QUEUE_CONFIG:
      return getQueueConfig();

    case IS_STATEFUL:
      return Boolean.valueOf(isIsStateful());

    case QUEUE_STATE:
      return getQueueState();

    }
    throw new IllegalStateException();
  }

  /** Returns true if field corresponding to fieldID is set (has been assigned a value) and false otherwise */
  public boolean isSet(_Fields field) {
    if (field == null) {
      throw new IllegalArgumentException();
    }

    switch (field) {
    case INSTANCE_ID:
      return isSetInstanceId();
    case GROUP_ID:
      return isSetGroupId();
    case GROUP_SIZE:
      return isSetGroupSize();
    case GROUP_NAME:
      return isSetGroupName();
    case PARTITIONING_KEY:
      return isSetPartitioningKey();
    case QUEUE_CONFIG:
      return isSetQueueConfig();
    case IS_STATEFUL:
      return isSetIsStateful();
    case QUEUE_STATE:
      return isSetQueueState();
    }
    throw new IllegalStateException();
  }

  @Override
  public boolean equals(Object that) {
    if (that == null)
      return false;
    if (that instanceof TQueueConsumer)
      return this.equals((TQueueConsumer)that);
    return false;
  }

  public boolean equals(TQueueConsumer that) {
    if (that == null)
      return false;

    boolean this_present_instanceId = true;
    boolean that_present_instanceId = true;
    if (this_present_instanceId || that_present_instanceId) {
      if (!(this_present_instanceId && that_present_instanceId))
        return false;
      if (this.instanceId != that.instanceId)
        return false;
    }

    boolean this_present_groupId = true;
    boolean that_present_groupId = true;
    if (this_present_groupId || that_present_groupId) {
      if (!(this_present_groupId && that_present_groupId))
        return false;
      if (this.groupId != that.groupId)
        return false;
    }

    boolean this_present_groupSize = true;
    boolean that_present_groupSize = true;
    if (this_present_groupSize || that_present_groupSize) {
      if (!(this_present_groupSize && that_present_groupSize))
        return false;
      if (this.groupSize != that.groupSize)
        return false;
    }

    boolean this_present_groupName = true && this.isSetGroupName();
    boolean that_present_groupName = true && that.isSetGroupName();
    if (this_present_groupName || that_present_groupName) {
      if (!(this_present_groupName && that_present_groupName))
        return false;
      if (!this.groupName.equals(that.groupName))
        return false;
    }

    boolean this_present_partitioningKey = true && this.isSetPartitioningKey();
    boolean that_present_partitioningKey = true && that.isSetPartitioningKey();
    if (this_present_partitioningKey || that_present_partitioningKey) {
      if (!(this_present_partitioningKey && that_present_partitioningKey))
        return false;
      if (!this.partitioningKey.equals(that.partitioningKey))
        return false;
    }

    boolean this_present_queueConfig = true && this.isSetQueueConfig();
    boolean that_present_queueConfig = true && that.isSetQueueConfig();
    if (this_present_queueConfig || that_present_queueConfig) {
      if (!(this_present_queueConfig && that_present_queueConfig))
        return false;
      if (!this.queueConfig.equals(that.queueConfig))
        return false;
    }

    boolean this_present_isStateful = true;
    boolean that_present_isStateful = true;
    if (this_present_isStateful || that_present_isStateful) {
      if (!(this_present_isStateful && that_present_isStateful))
        return false;
      if (this.isStateful != that.isStateful)
        return false;
    }

    boolean this_present_queueState = true && this.isSetQueueState();
    boolean that_present_queueState = true && that.isSetQueueState();
    if (this_present_queueState || that_present_queueState) {
      if (!(this_present_queueState && that_present_queueState))
        return false;
      if (!this.queueState.equals(that.queueState))
        return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    return 0;
  }

  public int compareTo(TQueueConsumer other) {
    if (!getClass().equals(other.getClass())) {
      return getClass().getName().compareTo(other.getClass().getName());
    }

    int lastComparison = 0;
    TQueueConsumer typedOther = (TQueueConsumer)other;

    lastComparison = Boolean.valueOf(isSetInstanceId()).compareTo(typedOther.isSetInstanceId());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetInstanceId()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.instanceId, typedOther.instanceId);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetGroupId()).compareTo(typedOther.isSetGroupId());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetGroupId()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.groupId, typedOther.groupId);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetGroupSize()).compareTo(typedOther.isSetGroupSize());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetGroupSize()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.groupSize, typedOther.groupSize);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetGroupName()).compareTo(typedOther.isSetGroupName());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetGroupName()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.groupName, typedOther.groupName);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetPartitioningKey()).compareTo(typedOther.isSetPartitioningKey());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetPartitioningKey()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.partitioningKey, typedOther.partitioningKey);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetQueueConfig()).compareTo(typedOther.isSetQueueConfig());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetQueueConfig()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.queueConfig, typedOther.queueConfig);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetIsStateful()).compareTo(typedOther.isSetIsStateful());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetIsStateful()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.isStateful, typedOther.isStateful);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetQueueState()).compareTo(typedOther.isSetQueueState());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetQueueState()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.queueState, typedOther.queueState);
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
    StringBuilder sb = new StringBuilder("TQueueConsumer(");
    boolean first = true;

    sb.append("instanceId:");
    sb.append(this.instanceId);
    first = false;
    if (!first) sb.append(", ");
    sb.append("groupId:");
    sb.append(this.groupId);
    first = false;
    if (!first) sb.append(", ");
    sb.append("groupSize:");
    sb.append(this.groupSize);
    first = false;
    if (isSetGroupName()) {
      if (!first) sb.append(", ");
      sb.append("groupName:");
      if (this.groupName == null) {
        sb.append("null");
      } else {
        sb.append(this.groupName);
      }
      first = false;
    }
    if (isSetPartitioningKey()) {
      if (!first) sb.append(", ");
      sb.append("partitioningKey:");
      if (this.partitioningKey == null) {
        sb.append("null");
      } else {
        sb.append(this.partitioningKey);
      }
      first = false;
    }
    if (isSetQueueConfig()) {
      if (!first) sb.append(", ");
      sb.append("queueConfig:");
      if (this.queueConfig == null) {
        sb.append("null");
      } else {
        sb.append(this.queueConfig);
      }
      first = false;
    }
    if (!first) sb.append(", ");
    sb.append("isStateful:");
    sb.append(this.isStateful);
    first = false;
    if (isSetQueueState()) {
      if (!first) sb.append(", ");
      sb.append("queueState:");
      if (this.queueState == null) {
        sb.append("null");
      } else {
        org.apache.thrift.TBaseHelper.toString(this.queueState, sb);
      }
      first = false;
    }
    sb.append(")");
    return sb.toString();
  }

  public void validate() throws org.apache.thrift.TException {
    // check for required fields
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
      __isset_bit_vector = new BitSet(1);
      read(new org.apache.thrift.protocol.TCompactProtocol(new org.apache.thrift.transport.TIOStreamTransport(in)));
    } catch (org.apache.thrift.TException te) {
      throw new java.io.IOException(te);
    }
  }

  private static class TQueueConsumerStandardSchemeFactory implements SchemeFactory {
    public TQueueConsumerStandardScheme getScheme() {
      return new TQueueConsumerStandardScheme();
    }
  }

  private static class TQueueConsumerStandardScheme extends StandardScheme<TQueueConsumer> {

    public void read(org.apache.thrift.protocol.TProtocol iprot, TQueueConsumer struct) throws org.apache.thrift.TException {
      org.apache.thrift.protocol.TField schemeField;
      iprot.readStructBegin();
      while (true)
      {
        schemeField = iprot.readFieldBegin();
        if (schemeField.type == org.apache.thrift.protocol.TType.STOP) { 
          break;
        }
        switch (schemeField.id) {
          case 1: // INSTANCE_ID
            if (schemeField.type == org.apache.thrift.protocol.TType.I32) {
              struct.instanceId = iprot.readI32();
              struct.setInstanceIdIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 2: // GROUP_ID
            if (schemeField.type == org.apache.thrift.protocol.TType.I64) {
              struct.groupId = iprot.readI64();
              struct.setGroupIdIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 3: // GROUP_SIZE
            if (schemeField.type == org.apache.thrift.protocol.TType.I32) {
              struct.groupSize = iprot.readI32();
              struct.setGroupSizeIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 4: // GROUP_NAME
            if (schemeField.type == org.apache.thrift.protocol.TType.STRING) {
              struct.groupName = iprot.readString();
              struct.setGroupNameIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 5: // PARTITIONING_KEY
            if (schemeField.type == org.apache.thrift.protocol.TType.STRING) {
              struct.partitioningKey = iprot.readString();
              struct.setPartitioningKeyIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 6: // QUEUE_CONFIG
            if (schemeField.type == org.apache.thrift.protocol.TType.STRUCT) {
              struct.queueConfig = new TQueueConfig();
              struct.queueConfig.read(iprot);
              struct.setQueueConfigIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 7: // IS_STATEFUL
            if (schemeField.type == org.apache.thrift.protocol.TType.BOOL) {
              struct.isStateful = iprot.readBool();
              struct.setIsStatefulIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 8: // QUEUE_STATE
            if (schemeField.type == org.apache.thrift.protocol.TType.STRING) {
              struct.queueState = iprot.readBinary();
              struct.setQueueStateIsSet(true);
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

      // check for required fields of primitive type, which can't be checked in the validate method
      struct.validate();
    }

    public void write(org.apache.thrift.protocol.TProtocol oprot, TQueueConsumer struct) throws org.apache.thrift.TException {
      struct.validate();

      oprot.writeStructBegin(STRUCT_DESC);
      oprot.writeFieldBegin(INSTANCE_ID_FIELD_DESC);
      oprot.writeI32(struct.instanceId);
      oprot.writeFieldEnd();
      oprot.writeFieldBegin(GROUP_ID_FIELD_DESC);
      oprot.writeI64(struct.groupId);
      oprot.writeFieldEnd();
      oprot.writeFieldBegin(GROUP_SIZE_FIELD_DESC);
      oprot.writeI32(struct.groupSize);
      oprot.writeFieldEnd();
      if (struct.groupName != null) {
        if (struct.isSetGroupName()) {
          oprot.writeFieldBegin(GROUP_NAME_FIELD_DESC);
          oprot.writeString(struct.groupName);
          oprot.writeFieldEnd();
        }
      }
      if (struct.partitioningKey != null) {
        if (struct.isSetPartitioningKey()) {
          oprot.writeFieldBegin(PARTITIONING_KEY_FIELD_DESC);
          oprot.writeString(struct.partitioningKey);
          oprot.writeFieldEnd();
        }
      }
      if (struct.queueConfig != null) {
        if (struct.isSetQueueConfig()) {
          oprot.writeFieldBegin(QUEUE_CONFIG_FIELD_DESC);
          struct.queueConfig.write(oprot);
          oprot.writeFieldEnd();
        }
      }
      oprot.writeFieldBegin(IS_STATEFUL_FIELD_DESC);
      oprot.writeBool(struct.isStateful);
      oprot.writeFieldEnd();
      if (struct.queueState != null) {
        if (struct.isSetQueueState()) {
          oprot.writeFieldBegin(QUEUE_STATE_FIELD_DESC);
          oprot.writeBinary(struct.queueState);
          oprot.writeFieldEnd();
        }
      }
      oprot.writeFieldStop();
      oprot.writeStructEnd();
    }

  }

  private static class TQueueConsumerTupleSchemeFactory implements SchemeFactory {
    public TQueueConsumerTupleScheme getScheme() {
      return new TQueueConsumerTupleScheme();
    }
  }

  private static class TQueueConsumerTupleScheme extends TupleScheme<TQueueConsumer> {

    @Override
    public void write(org.apache.thrift.protocol.TProtocol prot, TQueueConsumer struct) throws org.apache.thrift.TException {
      TTupleProtocol oprot = (TTupleProtocol) prot;
      BitSet optionals = new BitSet();
      if (struct.isSetInstanceId()) {
        optionals.set(0);
      }
      if (struct.isSetGroupId()) {
        optionals.set(1);
      }
      if (struct.isSetGroupSize()) {
        optionals.set(2);
      }
      if (struct.isSetGroupName()) {
        optionals.set(3);
      }
      if (struct.isSetPartitioningKey()) {
        optionals.set(4);
      }
      if (struct.isSetQueueConfig()) {
        optionals.set(5);
      }
      if (struct.isSetIsStateful()) {
        optionals.set(6);
      }
      if (struct.isSetQueueState()) {
        optionals.set(7);
      }
      oprot.writeBitSet(optionals, 8);
      if (struct.isSetInstanceId()) {
        oprot.writeI32(struct.instanceId);
      }
      if (struct.isSetGroupId()) {
        oprot.writeI64(struct.groupId);
      }
      if (struct.isSetGroupSize()) {
        oprot.writeI32(struct.groupSize);
      }
      if (struct.isSetGroupName()) {
        oprot.writeString(struct.groupName);
      }
      if (struct.isSetPartitioningKey()) {
        oprot.writeString(struct.partitioningKey);
      }
      if (struct.isSetQueueConfig()) {
        struct.queueConfig.write(oprot);
      }
      if (struct.isSetIsStateful()) {
        oprot.writeBool(struct.isStateful);
      }
      if (struct.isSetQueueState()) {
        oprot.writeBinary(struct.queueState);
      }
    }

    @Override
    public void read(org.apache.thrift.protocol.TProtocol prot, TQueueConsumer struct) throws org.apache.thrift.TException {
      TTupleProtocol iprot = (TTupleProtocol) prot;
      BitSet incoming = iprot.readBitSet(8);
      if (incoming.get(0)) {
        struct.instanceId = iprot.readI32();
        struct.setInstanceIdIsSet(true);
      }
      if (incoming.get(1)) {
        struct.groupId = iprot.readI64();
        struct.setGroupIdIsSet(true);
      }
      if (incoming.get(2)) {
        struct.groupSize = iprot.readI32();
        struct.setGroupSizeIsSet(true);
      }
      if (incoming.get(3)) {
        struct.groupName = iprot.readString();
        struct.setGroupNameIsSet(true);
      }
      if (incoming.get(4)) {
        struct.partitioningKey = iprot.readString();
        struct.setPartitioningKeyIsSet(true);
      }
      if (incoming.get(5)) {
        struct.queueConfig = new TQueueConfig();
        struct.queueConfig.read(iprot);
        struct.setQueueConfigIsSet(true);
      }
      if (incoming.get(6)) {
        struct.isStateful = iprot.readBool();
        struct.setIsStatefulIsSet(true);
      }
      if (incoming.get(7)) {
        struct.queueState = iprot.readBinary();
        struct.setQueueStateIsSet(true);
      }
    }
  }

}

