package com.yahoo.squidb.sample.models;

import com.yahoo.squidb.data.SquidCursor;
import com.yahoo.squidb.data.TableModel;
import com.yahoo.squidb.data.ValuesStorage;
import com.yahoo.squidb.sql.Property;
import com.yahoo.squidb.sql.Property.LongProperty;
import com.yahoo.squidb.sql.Property.StringProperty;
import com.yahoo.squidb.sql.Table;
import java.util.Map;

// Generated code -- do not modify!
// This class was generated from the model spec at com.yahoo.squidb.sample.models.TagSpec
public class Tag extends TableModel {

    // --- constants

    // --- allocate properties array
    public static final Property<?>[] PROPERTIES = new Property<?>[3];

    // --- table declaration
    public static final Table TABLE = new Table(Tag.class, PROPERTIES, "tags", null, "FOREIGN KEY(taskId) references tasks(_id) ON DELETE CASCADE");

    // --- property declarations
    public static final LongProperty ID = new LongProperty(TABLE, TableModel.DEFAULT_ID_COLUMN, "PRIMARY KEY AUTOINCREMENT");
    static {
        TABLE.setIdProperty(ID);
    };

    public static final StringProperty TAG = new StringProperty(TABLE, "tag", "NOT NULL");

    public static final LongProperty TASK_ID = new LongProperty(TABLE, "taskId", "NOT NULL");

    @Override
    public LongProperty getIdProperty() {
        return ID;
    }

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
    public Tag setId(long id) {
        super.setId(id);
        return this;
    }

}
