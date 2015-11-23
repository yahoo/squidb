/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.sql;

class BetweenCriterion extends BinaryCriterion {

    private final Object lower;
    private final Object upper;

    BetweenCriterion(Field<?> expression, Operator operator, Object lower, Object upper) {
        super(expression, operator, null);
        this.lower = lower;
        this.upper = upper;
    }

    @Override
    protected void afterPopulateOperator(SqlBuilder builder, boolean forSqlValidation) {
        builder.addValueToSql(lower, forSqlValidation);
        builder.sql.append(" AND ");
        builder.addValueToSql(upper, forSqlValidation);
    }

    @Override
    protected BinaryCriterion constructNegatedCriterion(Operator negatedOperator) {
        return new BetweenCriterion(field, negatedOperator, lower, upper);
    }
}
