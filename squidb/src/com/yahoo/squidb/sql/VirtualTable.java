/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.sql;

import com.yahoo.squidb.data.TableModel;
import com.yahoo.squidb.sql.Property.PropertyVisitor;
import com.yahoo.squidb.utility.VersionCode;

/**
 * A SQLite virtual table, which is an interface to an external storage or computation engine that appears to be a
 * table but does not actually store information in the database file. Virtual tables are implemented using a module
 * that was registered with SQLite database connection. Android currently supports FTS3 and FTS4 modules.
 */
public class VirtualTable extends Table {

    private static final VersionCode SQLITE_VERSION_IF_NOT_EXISTS = new VersionCode(3, 7, 11, 0);

    private final String moduleName;
    private final Field<String> anyColumn;

    public VirtualTable(Class<? extends TableModel> modelClass, Property<?>[] properties, String name,
            String databaseName, String module) {
        this(modelClass, properties, name, databaseName, module, null);
    }

    private VirtualTable(Class<? extends TableModel> modelClass, Property<?>[] properties, String name,
            String databaseName, String module, String alias) {
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
    public VirtualTable qualifiedFromDatabase(String databaseName) {
        VirtualTable result = new VirtualTable(modelClass, properties, getExpression(),
                databaseName, moduleName, alias);
        result.rowidProperty = rowidProperty;
        return result;
    }

    @Override
    public VirtualTable as(String newAlias) {
        return (VirtualTable) super.as(newAlias);
    }

    @Override
    protected VirtualTable asNewAliasWithPropertiesArray(String newAlias, Property<?>[] newProperties) {
        return new VirtualTable(modelClass, newProperties, getExpression(), qualifier, moduleName, newAlias);
    }

    /**
     * @return a {@link Criterion} that the any column matches the full-text query expression. This is only useful for
     * queries on virtual tables using fts3 or fts4.
     * @see <a href="http://www.sqlite.org/fts3.html#section_3">http://www.sqlite.org/fts3.html#section_3</a>
     */
    public Criterion match(String value) {
        return new BinaryCriterion(anyColumn, Operator.match, value);
    }

    @Override
    public String toString() {
        return super.toString() + " ModelClass=" + modelClass.getSimpleName() + " module=" + moduleName;
    }

    /**
     * Append a CREATE VIRTUAL TABLE statement that would create this table and its columns. Users should not
     * call this method and instead let {@link com.yahoo.squidb.data.SquidDatabase} build tables automatically.
     */
    @Override
    public void appendCreateTableSql(CompileContext compileContext, StringBuilder sql,
            PropertyVisitor<Void, StringBuilder> propertyVisitor) {
        sql.append("CREATE VIRTUAL TABLE ");
        if (compileContext != null && compileContext.getVersionCode().isAtLeast(SQLITE_VERSION_IF_NOT_EXISTS)) {
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
