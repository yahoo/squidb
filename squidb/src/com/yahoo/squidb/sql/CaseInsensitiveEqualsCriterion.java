/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.sql;

class CaseInsensitiveEqualsCriterion extends BinaryCriterion {

    CaseInsensitiveEqualsCriterion(Field<?> expression, Operator operator, Object value) {
        super(expression, operator, value);
    }

    @Override
    protected void afterPopulateOperator(SqlBuilder builder, boolean forSqlValidation) {
        super.afterPopulateOperator(builder, forSqlValidation);
        builder.sql.append(" COLLATE NOCASE ");
    }

    @Override
    protected BinaryCriterion constructNegatedCriterion(Operator negatedOperator) {
        return new CaseInsensitiveEqualsCriterion(field, negatedOperator, value);
    }
}
