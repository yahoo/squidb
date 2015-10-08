/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.sql;

import com.yahoo.squidb.data.TableModel;
import com.yahoo.squidb.sql.Property.LongProperty;
import com.yahoo.squidb.sql.Property.PropertyVisitor;
import com.yahoo.squidb.utility.VersionCode;

/**
 * A standard SQLite table.
 */
public class Table extends SqlTable<TableModel> {

    private final String tableConstraint;
    private LongProperty idProperty;

    public Table(Class<? extends TableModel> modelClass, Property<?>[] properties, String name) {
        this(modelClass, properties, name, null);
    }

    public Table(Class<? extends TableModel> modelClass, Property<?>[] properties, String name,
            String databaseName) {
        this(modelClass, properties, name, databaseName, null, null);
    }

    public Table(Class<? extends TableModel> modelClass, Property<?>[] properties, String name, String databaseName,
            String tableConstraint) {
        this(modelClass, properties, name, databaseName, tableConstraint, null);
    }

    private Table(Class<? extends TableModel> modelClass, Property<?>[] properties, String name, String databaseName,
            String tableConstraint, String alias) {
        super(modelClass, properties, name, databaseName);
        this.tableConstraint = tableConstraint;
        this.alias = alias;
    }

    public Table qualifiedFromDatabase(String databaseName) {
        return new Table(modelClass, properties, getExpression(), databaseName, tableConstraint, alias);
    }

    @Override
    public Table as(String newAlias) {
        return new Table(modelClass, properties, getExpression(), qualifier, tableConstraint, newAlias);
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

    /**
     * @return the additional table definition information used when creating the table
     */
    public String getTableConstraint() {
        return tableConstraint;
    }

    @Override
    public String toString() {
        return super.toString() + " ModelClass=" + modelClass.getSimpleName() + " TableConstraint=" + tableConstraint;
    }

    /**
     * Append a CREATE TABLE statement that would create this table and its columns. Users normally should not call
     * this method and instead let {@link com.yahoo.squidb.data.SquidDatabase} build tables automatically.
     */
    public void appendCreateTableSql(VersionCode sqliteVersion, StringBuilder sql,
            PropertyVisitor<Void, StringBuilder> propertyVisitor) {
        sql.append("CREATE TABLE IF NOT EXISTS ").append(getExpression()).append('(');
        boolean needsComma = false;
        for (Property<?> property : properties) {
            if (needsComma) {
                sql.append(", ");
            }
            property.accept(propertyVisitor, sql);
            needsComma = true;
        }
        if (!SqlUtils.isEmpty(getTableConstraint())) {
            sql.append(", ").append(getTableConstraint());
        }
        sql.append(')');
    }

    /**
     * Sets the primary key column for this table. Do not call this method! Exposed only so that it can be set
     * when initializing a model class.
     *
     * @param idProperty a LongProperty representing the table's primary key id column
     */
    public void setIdProperty(LongProperty idProperty) {
        if (this.idProperty != null) {
            throw new UnsupportedOperationException("Can't call setIdProperty on a Table more than once");
        }
        this.idProperty = idProperty;
    }

    /**
     * @return the property representing the table's primary key id column
     */
    public LongProperty getIdProperty() {
        if (idProperty == null) {
            throw new UnsupportedOperationException("Table " + getExpression() + " has no id property defined");
        }
        return idProperty;
    }
}
