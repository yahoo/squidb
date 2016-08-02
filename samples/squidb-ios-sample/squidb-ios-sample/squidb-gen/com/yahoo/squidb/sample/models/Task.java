package com.yahoo.squidb.sample.models;

import com.yahoo.squidb.data.SquidCursor;
import com.yahoo.squidb.data.TableModel;
import com.yahoo.squidb.data.ValuesStorage;
import com.yahoo.squidb.sql.Property;
import com.yahoo.squidb.sql.Property.IntegerProperty;
import com.yahoo.squidb.sql.Property.LongProperty;
import com.yahoo.squidb.sql.Property.StringProperty;
import com.yahoo.squidb.sql.Table;
import com.yahoo.squidb.sql.TableModelName;
import java.util.Map;

/**
 * This class was generated from the model spec at {@link com.yahoo.squidb.sample.models.TaskSpec}
 */
// Generated code -- do not modify!
public class Task extends TableModel {

    // --- allocate properties array
    public static final Property<?>[] PROPERTIES = new Property<?>[5];

    // --- table declaration
    public static final Table TABLE = new Table(Task.class, PROPERTIES, "tasks", null);
    public static final TableModelName TABLE_MODEL_NAME = new TableModelName(Task.class, TABLE.getName());

    // --- property declarations
    public static final LongProperty ID = new LongProperty(TABLE_MODEL_NAME, "_id", "PRIMARY KEY AUTOINCREMENT");
    static {
        TABLE.setRowIdProperty(ID);
    };

    @Override
    public LongProperty getRowIdProperty() {
        return ID;
    }


    public static final StringProperty TITLE = new StringProperty(TABLE_MODEL_NAME, "title", "NOT NULL");

    public static final LongProperty COMPLETION_DATE = new LongProperty(TABLE_MODEL_NAME, "completionDate", "DEFAULT 0");

    public static final LongProperty DUE_DATE = new LongProperty(TABLE_MODEL_NAME, "dueDate", "DEFAULT 0");

    public static final IntegerProperty PRIORITY = new IntegerProperty(TABLE_MODEL_NAME, "priority", "DEFAULT 0");

    static {
        PROPERTIES[0] = ID;
        PROPERTIES[1] = TITLE;
        PROPERTIES[2] = COMPLETION_DATE;
        PROPERTIES[3] = DUE_DATE;
        PROPERTIES[4] = PRIORITY;
    }

    // --- default values
    protected static final ValuesStorage defaultValues = new Task().newValuesStorage();
    static {
        // --- put property defaults
        defaultValues.put(COMPLETION_DATE.getName(), 0L);
        defaultValues.put(DUE_DATE.getName(), 0L);
        defaultValues.put(PRIORITY.getName(), 0);
    }

    @Override
    public ValuesStorage getDefaultValues() {
        return defaultValues;
    }

    // --- constants

    // --- default constructors
    public Task() {
        super();
    }

    public Task(SquidCursor<Task> cursor) {
        this();
        readPropertiesFromCursor(cursor);
    }

    public Task(Map<String, Object> values) {
        this(values, PROPERTIES);
    }

    public Task(Map<String, Object> values, Property<?>... withProperties) {
        this();
        readPropertiesFromMap(values, withProperties);
    }

    @Override
    public Task clone() {
        return (Task) super.clone();
    }

    // --- getters and setters
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

    public boolean isCompleted() {
        return TaskSpec.isCompleted(this);
    }

    @Override
    public Task setRowId(long rowid) {
        super.setRowId(rowid);
        return this;
    }

}
