package com.yahoo.squidb.sql;

import com.yahoo.squidb.data.AbstractModel;

/**
 * Struct representing a pair of a model class and a table name associated with that model class.
 */
public final class TableModelName {

    public final Class<? extends AbstractModel> modelClass;
    public final String tableName;

    public TableModelName(Class<? extends AbstractModel> modelClass, String tableName) {
        this.modelClass = modelClass;
        this.tableName = tableName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        TableModelName that = (TableModelName) o;

        if (modelClass != null ? !modelClass.equals(that.modelClass) : that.modelClass != null) {
            return false;
        }
        return tableName != null ? tableName.equals(that.tableName) : that.tableName == null;

    }

    @Override
    public int hashCode() {
        int result = modelClass != null ? modelClass.hashCode() : 0;
        result = 31 * result + (tableName != null ? tableName.hashCode() : 0);
        return result;
    }
}
