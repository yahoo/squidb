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
    private final List<Criterion> criterions = new ArrayList<>();

    protected Delete(SqlTable<?> table) {
        this.table = table;
    }

    @Override
    public SqlTable<?> getTable() {
        return table;
    }

    /**
     * Construct a new Delete statement on the specified {@link Table} or {@link VirtualTable}
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
        if (criterion != null) {
            this.criterions.add(criterion);
            invalidateCompileCache();
        }
        return this;
    }

    @Override
    void appendToSqlBuilder(SqlBuilder builder, boolean forSqlValidation) {
        builder.sql.append("DELETE FROM ").append(table.getExpression());
        visitWhere(builder, forSqlValidation);
    }

    private void visitWhere(SqlBuilder builder, boolean forSqlValidation) {
        if (criterions.isEmpty()) {
            return;
        }
        builder.sql.append(" WHERE ");
        builder.appendConcatenatedCompilables(criterions, " AND ", forSqlValidation);
    }
}
