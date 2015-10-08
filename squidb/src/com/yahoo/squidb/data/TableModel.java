/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.data;

import com.yahoo.squidb.sql.Property.LongProperty;

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

    /**
     * Utility method to get the identifier of the model, if it exists.
     *
     * @return {@value #NO_ID} if this model was not added to the database
     */
    public long getId() {
        Long id = null;
        String idPropertyName = getIdProperty().getName();
        if (setValues != null && setValues.containsKey(idPropertyName)) {
            id = (Long) setValues.get(idPropertyName);
        } else if (values != null && values.containsKey(idPropertyName)) {
            id = (Long) values.get(idPropertyName);
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
        if (id == NO_ID) {
            clearValue(getIdProperty());
        } else {
            if (setValues == null) {
                setValues = newValuesStorage();
            }
            setValues.put(getIdProperty().getName(), id);
        }
        return this;
    }

    /**
     * @return true if this model has been persisted to the database
     */
    public boolean isSaved() {
        return getId() != NO_ID;
    }

    /**
     * @return a {@link LongProperty to use as the integer primary key}
     */
    public abstract LongProperty getIdProperty();
}
