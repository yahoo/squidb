/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.sql;

import com.yahoo.squidb.data.AbstractDatabase;
import com.yahoo.squidb.data.TableModel;

/**
 * A SQLite virtual table, which is an interface to an external storage or computation engine that appears to be a
 * table but does not actually store information in the database file. Virtual tables are implemented using a module
 * that was registered with SQLite database connection. Android currently supports FTS3 and FTS4 modules.
 */
public class VirtualTable extends SqlTable<TableModel> {

    private final String moduleName;
    private final Field<String> anyColumn;

    public VirtualTable(Class<? extends TableModel> modelClass, Property<?>[] properties, String expression,
            String module) {
        this(modelClass, properties, expression, module, null);
    }

    public VirtualTable(Class<? extends TableModel> modelClass, Property<?>[] properties, String expression,
            String module, String alias) {
        super(modelClass, properties, expression);
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
    public VirtualTable as(String newAlias) {
        return new VirtualTable(modelClass, properties, getExpression(), moduleName, newAlias);
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
        StringBuilder sb = new StringBuilder(super.toString());
        sb.append(" ModelClass=").append(modelClass.getSimpleName())
                .append(" module=").append(moduleName);
        return sb.toString();
    }

    /**
     * Append a CREATE VIRTUAL TABLE statement that would create this table and its columns. Users normally should not
     * call this method and instead let {@link AbstractDatabase} build tables automatically.
     */
    public void appendCreateTableSql(StringBuilder sql) {
        sql.append("CREATE VIRTUAL TABLE IF NOT EXISTS ").append(getExpression()).append(" USING ").append(moduleName)
                .append('(');
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
