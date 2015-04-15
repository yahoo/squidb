/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.sql;

import java.util.ArrayList;
import java.util.List;

/**
 * Builder class for a SQLite DELETE statement
 */
public class Delete extends TableStatement {

    private final SqlTable<?> table;
    private final List<Criterion> criterions = new ArrayList<Criterion>();

    protected Delete(SqlTable<?> table) {
        this.table = table;
    }

    @Override
    public SqlTable<?> getTable() {
        return table;
    }

    /**
     * Construct a new Delete statement on the specified {@link Table}
     */
    public static Delete from(Table table) {
        return new Delete(table);
    }

    /**
     * Construct a new Delete statement on the specified {@link View}. Note that deletes on a View are only permissible
     * when an INSTEAD OF {@link com.yahoo.squidb.sql.Trigger} is constructed on that View.
     */
    public static Delete from(View view) {
        return new Delete(view);
    }

    /**
     * Adds a WHERE clause to this statement. Calling this method multiple times will combine all the {@link Criterion
     * criterions} with AND.
     *
     * @param criterion A criterion to use in the where clause
     * @return this Delete object, to allow chaining method calls
     */
    public Delete where(Criterion criterion) {
        this.criterions.add(criterion);
        invalidateCompileCache();
        return this;
    }

    @Override
    protected void appendCompiledStringWithArguments(StringBuilder sql, List<Object> deleteArgsBuilder) {
        sql.append("DELETE FROM ").append(table.getExpression());
        visitWhere(sql, deleteArgsBuilder);
    }

    private void visitWhere(StringBuilder sql, List<Object> deleteArgsBuilder) {
        if (criterions.isEmpty()) {
            return;
        }
        sql.append(" WHERE ");
        SqlUtils.appendConcatenatedCompilables(criterions, sql, deleteArgsBuilder, " AND ");
    }
}
