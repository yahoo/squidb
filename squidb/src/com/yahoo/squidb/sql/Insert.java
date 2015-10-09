/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.sql;

import com.yahoo.squidb.data.ValuesStorage;
import com.yahoo.squidb.utility.SquidUtilities;
import com.yahoo.squidb.utility.VersionCode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Builder class for a SQLite INSERT statement
 */
public class Insert extends TableStatement {

    private final SqlTable<?> table;
    private ConflictAlgorithm conflictAlgorithm = ConflictAlgorithm.NONE;
    private final List<String> columns = new ArrayList<String>();
    private final List<List<Object>> valuesToInsert = new ArrayList<List<Object>>();
    private Query query;
    private boolean defaultValues;

    private Insert(SqlTable<?> table) {
        this.table = table;
    }

    /**
     * Construct a new Insert statement on the specified {@link Table} or {@link VirtualTable}
     */
    public static Insert into(Table table) {
        return new Insert(table);
    }

    /**
     * Construct a new Insert statement on the specified {@link View}. Note that inserts into a View are only
     * permissible when an INSTEAD OF {@link com.yahoo.squidb.sql.Trigger} is constructed on that View.
     */
    public static Insert into(View view) {
        return new Insert(view);
    }

    @Override
    public SqlTable<?> getTable() {
        return table;
    }

    /**
     * Specify columns to insert into
     *
     * @param columns the columns to insert into
     * @return this Insert object, to allow chaining method calls
     */
    public Insert columns(Property<?>... columns) {
        for (Property<?> column : columns) {
            this.columns.add(column.getExpression());
        }
        defaultValues = false;
        invalidateCompileCache();
        return this;
    }

    public Insert columns(String... columnNames) {
        SquidUtilities.addAll(this.columns, columnNames);
        defaultValues = false;
        invalidateCompileCache();
        return this;
    }

    /**
     * Specify a set of values to insert. The number of values must equal the number of columns specified and the order
     * must match the order of the columns.
     * <p>
     * On Android JellyBean (API 16) and later, you can call this method multiple times to insert multiple rows.
     * Otherwise, calling this method more than once will throw an exception.
     *
     * @param values the values to insert
     * @return this Insert object, to allow chaining method calls
     * @throws UnsupportedOperationException if called multiple times on Android versions lower than 16 (JellyBean)
     */
    public Insert values(Object... values) {
        valuesToInsert.add(Arrays.asList(values));
        query = null;
        defaultValues = false;
        invalidateCompileCache();
        return this;
    }

    /**
     * Insert the results of the specified query into the table
     *
     * @param select the Query to execute
     * @return this Insert object, to allow chaining method calls
     */
    public Insert select(Query select) {
        this.query = select;
        valuesToInsert.clear();
        defaultValues = false;
        invalidateCompileCache();
        return this;
    }

    /**
     * Sets columns and values to insert based on the contents of the {@link ValuesStorage}
     *
     * @param values a ValuesStorage where keys are column names and values are values to insert
     * @return this Insert object, to allow chaining method calls
     */
    public Insert fromValues(ValuesStorage values) {
        List<Object> valuesToInsert = new ArrayList<Object>();
        for (Map.Entry<String, Object> entry : values.valueSet()) {
            this.columns.add(entry.getKey());
            valuesToInsert.add(entry.getValue());
        }
        this.valuesToInsert.add(valuesToInsert);
        invalidateCompileCache();
        return this;
    }

    /**
     * Insert default values for the columns into the table
     *
     * @return this Insert object, to allow chaining method calls
     */
    public Insert defaultValues() {
        defaultValues = true;
        columns.clear();
        valuesToInsert.clear();
        query = null;
        invalidateCompileCache();
        return this;
    }

    /**
     * @return the number of rows to be inserted (i.e. the number of values sets provided)
     */
    public int getNumRows() {
        return valuesToInsert.size();
    }

    /**
     * Specify a conflict algorithm for this statement
     *
     * @param conflictAlgorithm the {@link TableStatement.ConflictAlgorithm} to use
     * @return this Insert object, to allow chaining method calls
     */
    public Insert onConflict(ConflictAlgorithm conflictAlgorithm) {
        this.conflictAlgorithm = conflictAlgorithm;
        invalidateCompileCache();
        return this;
    }

    @Override
    void appendToSqlBuilder(SqlBuilder builder, boolean forSqlValidation) {
        assertValues();

        builder.sql.append("INSERT ");
        visitConflictAlgorithm(builder.sql);
        builder.sql.append("INTO ").append(table.getExpression()).append(" ");
        visitColumns(builder.sql);

        if (!valuesToInsert.isEmpty()) {
            visitValues(builder, forSqlValidation);
        } else if (query != null) {
            visitQuery(builder, forSqlValidation);
        } else {
            builder.sql.append("DEFAULT VALUES");
        }
    }

    private void assertValues() {
        if (!valuesToInsert.isEmpty()) {
            if (columns.isEmpty()) {
                throw new IllegalStateException("No columns were specified to insert into.");
            }
            assertValueSetSizes(columns.size());
        } else if (query != null) {
            if (columns.size() != query.getFields().size()) {
                throw new IllegalStateException("Number of properties being selected must match the number of columns "
                        + "specified.");
            }
        } else if (!defaultValues) {
            throw new IllegalStateException("No values to insert were specified.");
        }
    }

    private void assertValueSetSizes(int size) {
        for (List<Object> valueSet : valuesToInsert) {
            if (valueSet.size() != size) {
                throw new IllegalStateException("Number of terms in each values set must match the number of columns "
                        + "specified.");
            }
        }
    }

    private void visitConflictAlgorithm(StringBuilder sql) {
        if (ConflictAlgorithm.NONE != conflictAlgorithm) {
            sql.append("OR ").append(conflictAlgorithm).append(" ");
        }
    }

    private void visitColumns(StringBuilder sql) {
        if (columns.isEmpty()) {
            return;
        }
        sql.append("(");
        for (String column : columns) {
            sql.append(column).append(",");
        }
        sql.deleteCharAt(sql.length() - 1);
        sql.append(") ");
    }

    private void visitQuery(SqlBuilder builder, boolean forSqlValidation) {
        query.appendToSqlBuilder(builder, forSqlValidation);
    }

    private void visitValues(SqlBuilder builder, boolean forSqlValidation) {
        if (builder.sqliteVersion.isLessThan(VersionCode.V3_7_11) && valuesToInsert.size() > 1) {
            throw new UnsupportedOperationException("Can't insert with multiple sets of values below "
                    + "SQLite version 3.7.11");
        }
        builder.sql.append("VALUES ");
        for (List<Object> valuesList : valuesToInsert) {
            if (valuesList.isEmpty()) {
                continue;
            }

            builder.sql.append("(");
            for (Object value : valuesList) {
                builder.addValueToSql(value, forSqlValidation);
                builder.sql.append(",");
            }
            builder.sql.deleteCharAt(builder.sql.length() - 1);
            builder.sql.append("),");
        }
        builder.sql.deleteCharAt(builder.sql.length() - 1);
    }
}
