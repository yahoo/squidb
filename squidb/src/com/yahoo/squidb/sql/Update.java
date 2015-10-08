/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.sql;

import com.yahoo.squidb.data.AbstractModel;
import com.yahoo.squidb.data.ValuesStorage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Builder class for a SQLite UPDATE statement
 */
public class Update extends TableStatement {

    private final SqlTable<?> table;
    private ConflictAlgorithm conflictAlgorithm = ConflictAlgorithm.NONE;
    private final Map<String, Object> valuesToUpdate = new HashMap<String, Object>();
    private final List<Criterion> criterions = new ArrayList<Criterion>();

    protected Update(SqlTable<?> table) {
        this.table = table;
    }

    /**
     * Construct a new Update statement on the specified {@link Table} or {@link VirtualTable}
     */
    public static Update table(Table table) {
        return new Update(table);
    }

    /**
     * Construct a new Update statement on the specified {@link View}. Note that updates on a View are only permissible
     * when an INSTEAD OF Trigger is constructed on that View.
     */
    public static Update table(View view) {
        return new Update(view);
    }

    @Override
    public SqlTable<?> getTable() {
        return table;
    }

    /**
     * Set the {@link TableStatement.ConflictAlgorithm} this statement should use if a constraint violation occurs
     *
     * @param conflictAlgorithm the conflictAlgorithm to use
     * @return this Update object, to allow chaining method calls
     */
    public Update onConflict(ConflictAlgorithm conflictAlgorithm) {
        this.conflictAlgorithm = conflictAlgorithm;
        invalidateCompileCache();
        return this;
    }

    /**
     * Adds a WHERE clause to this statement. Calling this method multiple times will combine all the criterions with
     * AND.
     *
     * @param criterion A criterion to use in the where clause
     * @return this Delete object, to allow chaining method calls
     */
    public Update where(Criterion criterion) {
        if (criterion != null) {
            this.criterions.add(criterion);
            invalidateCompileCache();
        }
        return this;
    }

    /**
     * Update the specified column to the value provided
     *
     * @param column the column to set
     * @param value the new value for the column
     * @return this Update object, to allow chaining method calls
     */
    public Update set(Property<?> column, Object value) {
        if (column == null) {
            throw new IllegalArgumentException("column must not be null");
        }
        valuesToUpdate.put(column.getExpression(), value);
        invalidateCompileCache();
        return this;
    }

    /**
     * Update the specified columns to the values provided
     *
     * @param columns the columns to set
     * @param values the new values for the columns
     * @return this Update object, to allow chaining method calls
     */
    public Update set(Property<?>[] columns, Object[] values) {
        if (columns.length != values.length) {
            throw new IllegalArgumentException("You must provide the same number of columns and values");
        }
        for (int i = 0; i < columns.length; i++) {
            set(columns[i], values[i]);
        }
        invalidateCompileCache();
        return this;
    }

    /**
     * Update the specified columns to the values provided
     *
     * @param columns the columns to set
     * @param values the new values for the columns
     * @return this Update object, to allow chaining method calls
     */
    public Update set(List<Property<?>> columns, List<Object> values) {
        final int size = columns.size();
        if (size != values.size()) {
            throw new IllegalArgumentException("You must provide the same number of columns and values");
        }

        for (int i = 0; i < size; i++) {
            set(columns.get(i), values.get(i));
        }
        invalidateCompileCache();
        return this;
    }

    /**
     * Set the columns and values to update based on the specified model object
     *
     * @return this Update object, to allow chaining method calls
     */
    public Update fromTemplate(AbstractModel template) {
        if (!template.isModified()) {
            throw new IllegalArgumentException("Template has no values set to use for update");
        }
        ValuesStorage setValues = template.getSetValues();
        for (Entry<String, Object> entry : setValues.valueSet()) {
            valuesToUpdate.put(entry.getKey(), entry.getValue());
        }
        invalidateCompileCache();
        return this;
    }

    @Override
    void appendToSqlBuilder(SqlBuilder builder, boolean forSqlValidation) {
        assertValues();

        builder.sql.append("UPDATE ");
        visitConflictAlgorithm(builder.sql);
        builder.sql.append(table.getExpression()).append(" SET ");
        visitValues(builder, forSqlValidation);
        visitWhere(builder, forSqlValidation);
    }

    private void assertValues() {
        if (valuesToUpdate.isEmpty()) {
            throw new IllegalStateException("No columns specified for update");
        }
    }

    private void visitConflictAlgorithm(StringBuilder sql) {
        if (ConflictAlgorithm.NONE != conflictAlgorithm) {
            sql.append("OR ").append(conflictAlgorithm).append(" ");
        }
    }

    protected void visitValues(SqlBuilder builder, boolean forSqlValidation) {
        boolean appendComma = false;
        for (String column : valuesToUpdate.keySet()) {
            if (appendComma) {
                builder.sql.append(",");
            }
            appendComma = true;

            builder.sql.append(column).append(" = ");
            Object value = valuesToUpdate.get(column);
            builder.addValueToSql(value, forSqlValidation);
        }
    }

    private void visitWhere(SqlBuilder builder, boolean forSqlValidation) {
        if (criterions.isEmpty()) {
            return;
        }
        builder.sql.append(" WHERE ");
        builder.appendConcatenatedCompilables(criterions, " AND ", forSqlValidation);
    }
}
