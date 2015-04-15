/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.data;

import android.content.ContentValues;

import com.yahoo.squidb.sql.Property.LongProperty;
import com.yahoo.squidb.sql.SqlTable;

/**
 * Represents a row in a SQLite table. Each model has an ID property that references the rowid in the table. This value
 * can be retrieved by calling {@link #getId()}. Conventionally, the presence of an ID other than {@link #NO_ID}
 * signifies that this item exists in the table; calling {@link #isSaved()} performs this check for you.
 */
public abstract class TableModel extends AbstractModel {

    /** Default name for the primary key id column */
    public static final String DEFAULT_ID_COLUMN = "_id";

    /** SQLite internal rowid column name */
    public static final String ROWID = "rowid";

    /** sentinel for objects without an id */
    public static final long NO_ID = 0;

    /** id property common to all table based models */
    protected static final String ID_PROPERTY_NAME = DEFAULT_ID_COLUMN;

    /** id field common to all table based models */
    public static final LongProperty ID_PROPERTY = new LongProperty((SqlTable<?>) null, ROWID,
            ID_PROPERTY_NAME, null);

    /**
     * Utility method to get the identifier of the model, if it exists.
     *
     * @return {@value #NO_ID} if this model was not added to the database
     */
    public long getId() {
        Long id = null;
        if (setValues != null && setValues.containsKey(ID_PROPERTY_NAME)) {
            id = setValues.getAsLong(ID_PROPERTY_NAME);
        } else if (values != null && values.containsKey(ID_PROPERTY_NAME)) {
            id = values.getAsLong(ID_PROPERTY_NAME);
        }

        if (id != null) {
            return id;
        }
        return NO_ID;
    }

    /**
     * @param id the new ID for this model
     * @return this model instance, to allow chaining calls
     */
    public TableModel setId(long id) {
        if (setValues == null) {
            setValues = new ContentValues();
        }

        if (id == NO_ID) {
            clearValue(ID_PROPERTY);
        } else {
            setValues.put(ID_PROPERTY_NAME, id);
        }
        return this;
    }

    /**
     * @return true if this model has been persisted to the database
     */
    public boolean isSaved() {
        return getId() != NO_ID;
    }

}
