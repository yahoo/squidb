/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.sql;

import com.yahoo.squidb.data.TableModel;
import com.yahoo.squidb.utility.VersionCode;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A SQLite virtual table, which is an interface to an external storage or computation engine that appears to be a
 * table but does not actually store information in the database file. Virtual tables are implemented using a module
 * that was registered with SQLite database connection. Android currently supports FTS3 and FTS4 modules.
 */
public class VirtualTable extends Table {

    private static final VersionCode SQLITE_VERSION_IF_NOT_EXISTS = new VersionCode(3, 7, 11, 0);

    private final String moduleName;
    private final Field<String> anyColumn;

    public VirtualTable(@Nonnull Class<? extends TableModel> modelClass, @Nonnull List<Property<?>> properties,
            @Nonnull String name, @Nullable String databaseName, @Nonnull String module) {
        this(modelClass, properties, name, databaseName, module, null);
    }

    private VirtualTable(@Nonnull Class<? extends TableModel> modelClass, @Nonnull List<Property<?>> properties,
            @Nonnull String name, @Nullable String databaseName, @Nonnull String module, @Nullable String alias) {
        super(modelClass, properties, name, databaseName);
        this.moduleName = module;
        this.alias = alias;
        anyColumn = Field.field(expression);
    }

    /**
     * @return the module name used by this virtual table
     */
    public String getModuleName() {
        return moduleName;
    }

    @Override
    @Nonnull
    public VirtualTable qualifiedFromDatabase(@Nonnull String databaseName) {
        VirtualTable result = new VirtualTable(modelClass, properties, getExpression(),
                databaseName, moduleName, alias);
        result.rowidProperty = rowidProperty;
        return result;
    }

    @Override
    @Nonnull
    public VirtualTable as(@Nonnull String newAlias) {
        return (VirtualTable) super.as(newAlias);
    }

    @Override
    @Nonnull
    protected VirtualTable asNewAliasWithProperties(@Nonnull String newAlias, @Nonnull List<Property<?>> newProperties) {
        return new VirtualTable(modelClass, newProperties, getExpression(), qualifier, moduleName, newAlias);
    }

    /**
     * @return a {@link Criterion} that the any column matches the full-text query expression. This is only useful for
     * queries on virtual tables using fts3 or fts4.
     * @see <a href="http://www.sqlite.org/fts3.html#section_3">http://www.sqlite.org/fts3.html#section_3</a>
     */
    @Nonnull
    public Criterion match(@Nullable String value) {
        return new BinaryCriterion(anyColumn, Operator.match, value);
    }

    @Override
    @Nonnull
    public String toString() {
        return super.toString() + " ModelClass=" + modelClass.getSimpleName() + " module=" + moduleName;
    }

    /**
     * Append a CREATE VIRTUAL TABLE statement that would create this table and its columns. Users should not
     * call this method and instead let {@link com.yahoo.squidb.data.SquidDatabase} build tables automatically.
     */
    @Override
    public void appendCreateTableSql(@Nonnull CompileContext compileContext, @Nonnull StringBuilder sql) {
        sql.append("CREATE VIRTUAL TABLE ");
        if (compileContext.getVersionCode().isAtLeast(SQLITE_VERSION_IF_NOT_EXISTS)) {
            sql.append("IF NOT EXISTS ");
        }
        sql.append(getExpression()).append(" USING ").append(moduleName).append('(');
        boolean needComma = false;
        for (Property<?> property : properties) {
            if (TableModel.ROWID.equals(property.getExpression())) {
                continue;
            }
            if (needComma) {
                sql.append(',');
            }
            sql.append(property.getName());
            needComma = true;
        }
        sql.append(')');
    }
}
