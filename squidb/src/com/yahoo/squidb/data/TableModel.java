/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.data;

import com.yahoo.squidb.sql.Property.LongProperty;

/**
 * Represents a row in a SQLite table. Each model has an ID property that references the rowid in the table. This value
 * can be retrieved by calling {@link #getRowId()} ()}. Conventionally, the presence of an ID other than {@link #NO_ID}
 * signifies that this item exists in the table; calling {@link #isSaved()} performs this check for you.
 */
public abstract class TableModel extends AbstractModel {

    /**
     * Default name for the primary key id column. This value has been deprecated and will be removed in a future
     * version of SquiDB.
     */
    @Deprecated
    public static final String DEFAULT_ID_COLUMN = "_id";

    /** SQLite internal rowid column name */
    public static final String ROWID = "rowid";

    /** sentinel for objects without an id */
    public static final long NO_ID = 0;

    /**
     * Utility method to get the rowid of the model, if it exists.
     *
     * @return {@value #NO_ID} if this model was not added to the database
     */
    public long getRowId() {
        Long id = null;
        String idPropertyName = getRowIdProperty().getName();
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
     * Deprecated alias for {@link #getRowId()}
     */
    @Deprecated
    public long getId() {
        return getRowId();
    }

    /**
     * @param rowid the new rowid for this model
     * @return this model instance, to allow chaining calls
     */
    public TableModel setRowId(long rowid) {
        if (rowid == NO_ID) {
            clearValue(getRowIdProperty());
        } else {
            if (setValues == null) {
                setValues = newValuesStorage();
            }
            setValues.put(getRowIdProperty().getName(), rowid);
        }
        return this;
    }

    /**
     * Deprecated alias for {@link #setRowId(long)}
     */
    @Deprecated
    public TableModel setId(long id) {
        return setRowId(id);
    }

    /**
     * @return true if this model has been persisted to the database
     */
    public boolean isSaved() {
        return getRowId() != NO_ID;
    }

    /**
     * @return a {@link LongProperty representing the rowid of the table}
     */
    public abstract LongProperty getRowIdProperty();

    /**
     * Deprecated alias for {@link #getRowIdProperty()}
     */
    @Deprecated
    public LongProperty getIdProperty() {
        return getRowIdProperty();
    }
}
