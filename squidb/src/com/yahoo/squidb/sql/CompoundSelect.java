/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.sql;

/**
 * A compound operator used in SELECT statements
 *
 * @see <a href="http://www.sqlite.org/lang_select.html#compound">http://www.sqlite.org/lang_select.html#compound</a>
 */
public final class CompoundSelect extends CompilableWithArguments {

    private enum CompoundSelectOperator {
        UNION("UNION"),
        UNION_ALL("UNION ALL"),
        INTERSECT("INTERSECT"),
        EXCEPT("EXCEPT");

        private final String expression;

        CompoundSelectOperator(String expression) {
            this.expression = expression;
        }

        @Override
        public String toString() {
            return expression;
        }
    }

    private final CompoundSelectOperator operator;
    final Query query;

    private CompoundSelect(CompoundSelectOperator operator, Query query) {
        this.operator = operator;
        this.query = query;
    }

    /**
     * The UNION operator returns all rows returned by the left-hand SELECT and all rows returned by the right-hand
     * SELECT. Duplicate rows are removed from the result set.
     */
    public static CompoundSelect union(Query query) {
        return new CompoundSelect(CompoundSelectOperator.UNION, query);
    }

    /**
     * The UNION ALL operator returns all rows returned by the left-hand SELECT and all rows returned by the right-hand
     * SELECT. The result set may contain duplicate rows.
     */
    public static CompoundSelect unionAll(Query query) {
        return new CompoundSelect(CompoundSelectOperator.UNION_ALL, query);
    }

    /**
     * The INTERSECT operator returns the subset of rows returned by the left-hand SELECT that are also returned by the
     * right-hand SELECT. Duplicate rows are removed from the result set.
     */
    public static CompoundSelect intersect(Query query) {
        return new CompoundSelect(CompoundSelectOperator.INTERSECT, query);
    }

    /**
     * The EXCEPT operator returns the subset of rows returned by the left-hand SELECT that are not also returned by
     * the right-hand SELECT. Duplicate rows are removed from the result set.
     */
    public static CompoundSelect except(Query query) {
        return new CompoundSelect(CompoundSelectOperator.EXCEPT, query);
    }

    @Override
    void appendToSqlBuilder(SqlBuilder builder, boolean forSqlValidation) {
        builder.sql.append(operator.toString()).append(" ");
        query.appendToSqlBuilder(builder, forSqlValidation);
    }
}
