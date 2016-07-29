package com.yahoo.squidb.sample.models;

import com.yahoo.squidb.data.SquidCursor;
import com.yahoo.squidb.data.TableModel;
import com.yahoo.squidb.data.ValuesStorage;
import com.yahoo.squidb.sql.Property;
import com.yahoo.squidb.sql.Property.LongProperty;
import com.yahoo.squidb.sql.Property.StringProperty;
import com.yahoo.squidb.sql.Table;
import com.yahoo.squidb.sql.TableModelName;
import java.util.Map;

/**
 * This class was generated from the model spec at {@link com.yahoo.squidb.sample.models.TagSpec}
 */
// Generated code -- do not modify!
public class Tag extends TableModel {

    // --- allocate properties array
    public static final Property<?>[] PROPERTIES = new Property<?>[3];

    // --- table declaration
    public static final Table TABLE = new Table(Tag.class, PROPERTIES, "tags", null, "FOREIGN KEY(taskId) references tasks(_id) ON DELETE CASCADE");
    public static final TableModelName TABLE_MODEL_NAME = new TableModelName(Tag.class, TABLE.getName());

    // --- property declarations
    public static final LongProperty ID = new LongProperty(TABLE_MODEL_NAME, "_id", "PRIMARY KEY AUTOINCREMENT");
    static {
        TABLE.setRowIdProperty(ID);
    };

    @Override
    public LongProperty getRowIdProperty() {
        return ID;
    }


    public static final StringProperty TAG = new StringProperty(TABLE_MODEL_NAME, "tag", "NOT NULL");

    public static final LongProperty TASK_ID = new LongProperty(TABLE_MODEL_NAME, "taskId", "NOT NULL");

    static {
        PROPERTIES[0] = ID;
        PROPERTIES[1] = TAG;
        PROPERTIES[2] = TASK_ID;
    }

    // --- default values
    protected static final ValuesStorage defaultValues = new Tag().newValuesStorage();
    static {
        // --- put property defaults
    }

    @Override
    public ValuesStorage getDefaultValues() {
        return defaultValues;
    }

    // --- constants

    // --- default constructors
    public Tag() {
        super();
    }

    public Tag(SquidCursor<Tag> cursor) {
        this();
        readPropertiesFromCursor(cursor);
    }

    public Tag(Map<String, Object> values) {
        this(values, PROPERTIES);
    }

    public Tag(Map<String, Object> values, Property<?>... withProperties) {
        this();
        readPropertiesFromMap(values, withProperties);
    }

    @Override
    public Tag clone() {
        return (Tag) super.clone();
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

}
