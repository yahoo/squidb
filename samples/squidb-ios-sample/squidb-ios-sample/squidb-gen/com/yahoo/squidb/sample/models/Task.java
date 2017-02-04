// Generated code -- do not modify!
package com.yahoo.squidb.sample.models;

import com.yahoo.squidb.data.SquidCursor;
import com.yahoo.squidb.data.TableModel;
import com.yahoo.squidb.data.ValuesStorage;
import com.yahoo.squidb.sql.Property;
import com.yahoo.squidb.sql.Table;
import com.yahoo.squidb.sql.TableModelName;
import java.lang.Integer;
import java.lang.Long;
import java.lang.Override;
import java.lang.String;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This class was generated from the model spec at {@link com.yahoo.squidb.sample.models.TaskSpec}
 */
public class Task extends TableModel {
	private static final List<Property<?>> PROPERTIES_INTERNAL = new ArrayList<>(5);

	public static final List<Property<?>> PROPERTIES = Collections.unmodifiableList(PROPERTIES_INTERNAL);

	public static final Table TABLE = new Table(Task.class, PROPERTIES, "tasks", null, null);

	public static final TableModelName TABLE_MODEL_NAME = new TableModelName(Task.class, TABLE.getName());

	public static final Property.LongProperty ID = new Property.LongProperty(TABLE_MODEL_NAME, "_id", "PRIMARY KEY AUTOINCREMENT");

	public static final Property.StringProperty TITLE = new Property.StringProperty(TABLE_MODEL_NAME, "title", "NOT NULL");

	public static final Property.LongProperty COMPLETION_DATE = new Property.LongProperty(TABLE_MODEL_NAME, "completionDate", "DEFAULT 0");

	public static final Property.LongProperty DUE_DATE = new Property.LongProperty(TABLE_MODEL_NAME, "dueDate", "DEFAULT 0");

	public static final Property.IntegerProperty PRIORITY = new Property.IntegerProperty(TABLE_MODEL_NAME, "priority", "DEFAULT 0");

	protected static final ValuesStorage defaultValues = new Task().newValuesStorage();

	static {
		PROPERTIES_INTERNAL.add(ID);
		PROPERTIES_INTERNAL.add(TITLE);
		PROPERTIES_INTERNAL.add(COMPLETION_DATE);
		PROPERTIES_INTERNAL.add(DUE_DATE);
		PROPERTIES_INTERNAL.add(PRIORITY);
	}
	static {
		defaultValues.put(COMPLETION_DATE.getName(), 0L);
		defaultValues.put(DUE_DATE.getName(), 0L);
		defaultValues.put(PRIORITY.getName(), 0);
	}
	static {
		TABLE.setRowIdProperty(ID);
	}

	public Task() {
		super();
	}

	public Task(SquidCursor<Task> cursor) {
		this();
		readPropertiesFromCursor(cursor);
	}

	public Task(ValuesStorage values) {
		this(values, PROPERTIES);
	}

	public Task(ValuesStorage values, Property<?>... withProperties) {
		this();
		readPropertiesFromValuesStorage(values, withProperties);
	}

	public Task(ValuesStorage values, List<Property<?>> withProperties) {
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
	public Task setId(long id) {
		super.setRowId(id);
		return this;
	}

	public String getTitle() {
		return get(TITLE);
	}

	public Task setTitle(String title) {
		set(TITLE, title);
		return this;
	}

	public Long getCompletionDate() {
		return get(COMPLETION_DATE);
	}

	public Task setCompletionDate(Long completionDate) {
		set(COMPLETION_DATE, completionDate);
		return this;
	}

	public Long getDueDate() {
		return get(DUE_DATE);
	}

	public Task setDueDate(Long dueDate) {
		set(DUE_DATE, dueDate);
		return this;
	}

	public Integer getPriority() {
		return get(PRIORITY);
	}

	public Task setPriority(Integer priority) {
		set(PRIORITY, priority);
		return this;
	}

	@Override
	public Task setRowId(long rowid) {
		super.setRowId(rowid);
		return this;
	}

	@Override
	public Task clone() {
		return (Task) super.clone();
	}

	public boolean isCompleted() {
		return TaskSpec.isCompleted(this);
	}
}
