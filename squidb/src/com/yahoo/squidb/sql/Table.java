/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.sql;

import android.text.TextUtils;

import com.yahoo.squidb.data.AbstractDatabase;
import com.yahoo.squidb.data.TableModel;
import com.yahoo.squidb.sql.Property.PropertyVisitor;

/**
 * A standard SQLite table.
 */
public final class Table extends SqlTable<TableModel> {

    private final String tableConstraint;

    public Table(Class<? extends TableModel> modelClass, Property<?>[] properties, String name) {
        this(modelClass, properties, name, null, null);
    }

    public Table(Class<? extends TableModel> modelClass, Property<?>[] properties, String name,
            String tableConstraint) {
        this(modelClass, properties, name, tableConstraint, null);
    }

    public Table(Class<? extends TableModel> modelClass, Property<?>[] properties, String name, String tableConstraint,
            String alias) {
        super(modelClass, properties, name);
        this.alias = alias;
        this.tableConstraint = tableConstraint;
    }

    private Table(Class<? extends TableModel> modelClass, Property<?>[] properties, String name, String tableConstraint,
            String alias, String databaseName) {
        super(modelClass, properties, name, databaseName);
        this.alias = alias;
        this.tableConstraint = tableConstraint;
    }

    public Table qualifiedFromDatabase(String databaseName) {
        return new Table(modelClass, properties, getExpression(), tableConstraint, alias, databaseName);
    }

    /**
     * Construct an {@link Index} with the given name that indexes the given columns
     *
     * @param name the name for the index
     * @param columns the properties representing the columns to index
     * @return an Index
     */
    public Index index(String name, Property<?>... columns) {
        return new Index(name, this, false, columns);
    }

    /**
     * Construct a unique {@link Index} with the given name that indexes the given columns. Unique indexes do not allow
     * duplicate entries.
     *
     * @param name the name for the index
     * @param columns the properties representing the columns to index
     * @return a unique Index
     */
    public Index uniqueIndex(String name, Property<?>... columns) {
        return new Index(name, this, true, columns);
    }

    @Override
    public Table as(String newAlias) {
        return new Table(modelClass, properties, getExpression(), null, newAlias);
    }

    /**
     * @return the additional table definition information used when creating the table
     */
    public String getTableConstraint() {
        return tableConstraint;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(super.toString());
        sb.append(" ModelClass=").append(modelClass.getSimpleName())
                .append(" TableConstraint=").append(tableConstraint);
        return sb.toString();
    }

    /**
     * Append a CREATE TABLE statement that would create this table and its columns. Users normally should not call
     * this method and instead let {@link AbstractDatabase} build tables automatically.
     */
    public void appendCreateTableSql(StringBuilder sql, PropertyVisitor<Void, StringBuilder> propertyVisitor) {
        sql.append("CREATE TABLE IF NOT EXISTS ").append(getExpression()).append('(').
                append(TableModel.DEFAULT_ID_COLUMN).append(" INTEGER PRIMARY KEY AUTOINCREMENT");
        for (Property<?> property : properties) {
            if (TableModel.DEFAULT_ID_COLUMN.equals(property.getExpression())) {
                continue;
            }
            sql.append(',');
            property.accept(propertyVisitor, sql);
        }
        if (!TextUtils.isEmpty(getTableConstraint())) {
            sql.append(", ").append(getTableConstraint());
        }
        sql.append(')');
    }
}
