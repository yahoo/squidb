/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.sql;

class LikeCriterion extends BinaryCriterion {

    private final char escape;

    LikeCriterion(Field<?> expression, Operator operator, Object value, char escape) {
        super(expression, operator, value);
        this.escape = escape;
    }

    @Override
    protected void afterPopulateOperator(SqlBuilder builder, boolean forSqlValidation) {
        super.afterPopulateOperator(builder, forSqlValidation);
        if (escape != '\0') {
            builder.sql.append(" ESCAPE ").append(SqlUtils.sanitizeStringAsLiteral(Character.toString(escape)));
        }
    }

    @Override
    protected BinaryCriterion constructNegatedCriterion(Operator negatedOperator) {
        return new LikeCriterion(field, negatedOperator, value, escape);
    }
}
