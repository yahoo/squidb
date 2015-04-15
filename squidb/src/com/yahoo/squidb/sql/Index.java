/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.sql;

/**
 * A SQLite Index
 */
public class Index {

    private final String name;
    private final Table table;
    private final boolean unique;
    private final Property<?>[] properties;

    public Index(String name, Table table, boolean unique, Property<?>... properties) {
        this.name = name;
        this.table = table;
        this.unique = unique;
        this.properties = properties;
    }

    /**
     * @return the name of this Index
     */
    public String getName() {
        return name;
    }

    /**
     * @return the {@link Table} on which this Index is created
     */
    public Table getTable() {
        return table;
    }

    /**
     * @return true if this is a unique Index, false otherwise. Unique indexes do not allow duplicate entries.
     */
    public boolean isUnique() {
        return unique;
    }

    /**
     * @return the {@link Property properties} representing columns indexed by this Index
     */
    public Property<?>[] getProperties() {
        return properties;
    }

}
