/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.sql;

import com.yahoo.squidb.data.TableModel;
import com.yahoo.squidb.sql.Property.LongProperty;
import com.yahoo.squidb.utility.VersionCode;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A standard SQLite table.
 */
public class Table extends SqlTable<TableModel> {

    private final String tableConstraint;
    protected LongProperty rowidProperty;

    public Table(@Nonnull Class<? extends TableModel> modelClass, @Nonnull List<Property<?>> properties,
            @Nonnull String name) {
        this(modelClass, properties, name, null);
    }

    public Table(@Nonnull Class<? extends TableModel> modelClass, @Nonnull List<Property<?>> properties,
            @Nonnull String name, @Nullable String databaseName) {
        this(modelClass, properties, name, databaseName, null, null);
    }

    public Table(@Nonnull Class<? extends TableModel> modelClass, @Nonnull List<Property<?>> properties,
            @Nonnull String name, @Nullable String databaseName, @Nullable String tableConstraint) {
        this(modelClass, properties, name, databaseName, tableConstraint, null);
    }

    private Table(@Nonnull Class<? extends TableModel> modelClass, @Nonnull List<Property<?>> properties,
            @Nonnull String name, @Nullable String databaseName, @Nullable String tableConstraint,
            @Nullable String alias) {
        super(modelClass, properties, name, databaseName);
        this.tableConstraint = tableConstraint;
        this.alias = alias;
    }

    @Nonnull
    public Table qualifiedFromDatabase(@Nonnull String databaseName) {
        Table result = new Table(modelClass, properties, getExpression(), databaseName, tableConstraint, alias);
        result.rowidProperty = rowidProperty;
        return result;
    }

    @Override
    @Nonnull
    public Table as(@Nonnull String newAlias) {
        Table result = (Table) super.as(newAlias);
        result.rowidProperty = rowidProperty == null ? null : result.qualifyProperty(rowidProperty);
        return result;
    }

    @Override
    @Nonnull
    protected Table asNewAliasWithProperties(@Nonnull String newAlias, @Nonnull List<Property<?>> newProperties) {
        return new Table(modelClass, newProperties, getExpression(), qualifier, tableConstraint, newAlias);
    }

    /**
     * Construct an {@link Index} with the given name that indexes the given columns
     *
     * @param name the name for the index
     * @param columns the properties representing the columns to index
     * @return an Index
     */
    @Nonnull
    public Index index(@Nonnull String name, @Nonnull Property<?>... columns) {
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
    @Nonnull
    public Index uniqueIndex(@Nonnull String name, @Nonnull Property<?>... columns) {
        return new Index(name, this, true, columns);
    }

    /**
     * @return the additional table definition information used when creating the table
     */
    @Nullable
    public String getTableConstraint() {
        return tableConstraint;
    }

    @Override
    @Nonnull
    public String toString() {
        return super.toString() + " ModelClass=" + modelClass.getSimpleName() + " TableConstraint=" + tableConstraint;
    }

    private static final ColumnDefinitionVisitor columnDefVisitor = new ColumnDefinitionVisitor();

    /**
     * Append a CREATE TABLE statement that would create this table and its columns. Users should not call
     * this method and instead let {@link com.yahoo.squidb.data.SquidDatabase} build tables automatically.
     */
    public void appendCreateTableSql(@Nonnull CompileContext compileContext, @Nonnull StringBuilder sql) {
        sql.append("CREATE TABLE IF NOT EXISTS ").append(getExpression()).append('(');
        boolean needsComma = false;
        for (Property<?> property : properties) {
            if (TableModel.ROWID.equals(property.getExpression())) {
                continue;
            }
            if (needsComma) {
                sql.append(", ");
            }
            property.accept(columnDefVisitor, sql);
            needsComma = true;
        }
        if (!SqlUtils.isEmpty(getTableConstraint())) {
            sql.append(", ").append(getTableConstraint());
        }
        sql.append(')');
    }

    /**
     * @param compileContext a {@link CompileContext} for generating the <code>CREATE TABLE</code> statement. This
     * should be a context holding the version code of the SQLite build being targeted by the user. A default context
     * for a given SQLite version can be constructed using
     * {@link CompileContext#defaultContextForVersionCode(VersionCode)}, or a context can be built manually using
     * {@link com.yahoo.squidb.sql.CompileContext.Builder}
     * @return the <code>CREATE TABLE</code> statement for creating this table. Users should generally not need to call
     * this method directly unless they are not working with a SquidDatabase instance and wish to create tables
     * manually.
     */
    @Nonnull
    public String getCreateTableSql(@Nonnull CompileContext compileContext) {
        StringBuilder sql = new StringBuilder(SqlStatement.STRING_BUILDER_INITIAL_CAPACITY);
        appendCreateTableSql(compileContext, sql);
        return sql.toString();
    }

    /**
     * Sets the primary key column for this table. Do not call this method! Exposed only so that it can be set
     * when initializing a model class.
     *
     * @param rowidProperty a LongProperty representing the table's primary key id column
     */
    public void setRowIdProperty(@Nonnull LongProperty rowidProperty) {
        if (this.rowidProperty != null) {
            throw new UnsupportedOperationException("Can't call setRowIdProperty on a Table more than once");
        }
        this.rowidProperty = rowidProperty;
    }

    /**
     * @return the property representing the table's rowid column (or a integer primary key rowid alias if one exists)
     */
    @Nonnull
    public LongProperty getRowIdProperty() {
        if (rowidProperty == null) {
            throw new UnsupportedOperationException("Table " + getExpression() + " has no rowid property defined");
        }
        return rowidProperty;
    }
}
