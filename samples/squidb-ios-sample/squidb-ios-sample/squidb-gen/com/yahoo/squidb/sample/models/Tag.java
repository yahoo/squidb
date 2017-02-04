// Generated code -- do not modify!
package com.yahoo.squidb.sample.models;

import com.yahoo.squidb.data.SquidCursor;
import com.yahoo.squidb.data.TableModel;
import com.yahoo.squidb.data.ValuesStorage;
import com.yahoo.squidb.sql.Property;
import com.yahoo.squidb.sql.Table;
import com.yahoo.squidb.sql.TableModelName;
import java.lang.Long;
import java.lang.Override;
import java.lang.String;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This class was generated from the model spec at {@link com.yahoo.squidb.sample.models.TagSpec}
 */
public class Tag extends TableModel {
	private static final List<Property<?>> PROPERTIES_INTERNAL = new ArrayList<>(3);

	public static final List<Property<?>> PROPERTIES = Collections.unmodifiableList(PROPERTIES_INTERNAL);

	public static final Table TABLE = new Table(Tag.class, PROPERTIES, "tags", null, "FOREIGN KEY(taskId) references tasks(_id) ON DELETE CASCADE");

	public static final TableModelName TABLE_MODEL_NAME = new TableModelName(Tag.class, TABLE.getName());

	public static final Property.LongProperty ID = new Property.LongProperty(TABLE_MODEL_NAME, "_id", "PRIMARY KEY AUTOINCREMENT");

	public static final Property.StringProperty TAG = new Property.StringProperty(TABLE_MODEL_NAME, "tag", "NOT NULL");

	public static final Property.LongProperty TASK_ID = new Property.LongProperty(TABLE_MODEL_NAME, "taskId", "NOT NULL");

	protected static final ValuesStorage defaultValues = new Tag().newValuesStorage();

	static {
		PROPERTIES_INTERNAL.add(ID);
		PROPERTIES_INTERNAL.add(TAG);
		PROPERTIES_INTERNAL.add(TASK_ID);
	}
	static {
		TABLE.setRowIdProperty(ID);
	}

	public Tag() {
		super();
	}

	public Tag(SquidCursor<Tag> cursor) {
		this();
		readPropertiesFromCursor(cursor);
	}

	public Tag(ValuesStorage values) {
		this(values, PROPERTIES);
	}

	public Tag(ValuesStorage values, Property<?>... withProperties) {
		this();
		readPropertiesFromValuesStorage(values, withProperties);
	}

	public Tag(ValuesStorage values, List<Property<?>> withProperties) {
		this();
		readPropertiesFromValuesStorage(values, withProperties);
	}

	@Override
	public ValuesStorage getDefaultValues() {
		return defaultValues;
	}

	@Override
	public Property.LongProperty getRowIdProperty() {
		return ID;
	}

	/**
	 * This getter is an alias for getRowId(), as the underlying column is an INTEGER PRIMARY KEY
	 */
	public long getId() {
		return super.getRowId();
	}

	/**
	 * This setter is an alias for setRowId(), as the underlying column is an INTEGER PRIMARY KEY
	 */
	public Tag setId(long id) {
		super.setRowId(id);
		return this;
	}

	public String getTag() {
		return get(TAG);
	}

	public Tag setTag(String tag) {
		set(TAG, tag);
		return this;
	}

	public Long getTaskId() {
		return get(TASK_ID);
	}

	public Tag setTaskId(Long taskId) {
		set(TASK_ID, taskId);
		return this;
	}

	@Override
	public Tag setRowId(long rowid) {
		super.setRowId(rowid);
		return this;
	}

	@Override
	public Tag clone() {
		return (Tag) super.clone();
	}
}
