/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.sql;

class BinaryCriterion extends Criterion {

    protected final Field<?> field;
    protected final Object value;

    BinaryCriterion(Field<?> expression, Operator operator, Object value) {
        super(operator);
        this.field = expression;
        this.value = value;
    }

    @Override
    protected void populate(SqlBuilder builder, boolean forSqlValidation) {
        beforePopulateOperator(builder, forSqlValidation);
        populateOperator(builder.sql);
        afterPopulateOperator(builder, forSqlValidation);
    }

    protected void beforePopulateOperator(SqlBuilder builder, boolean forSqlValidation) {
        field.appendQualifiedExpression(builder, forSqlValidation);
    }

    protected void populateOperator(StringBuilder sql) {
        sql.append(operator);
    }

    protected void afterPopulateOperator(SqlBuilder builder, boolean forSqlValidation) {
        builder.addValueToSql(value, forSqlValidation);
    }

    @Override
    public Criterion negate() {
        Operator contrary = operator.getContrary();
        if (contrary != null) {
            return constructNegatedCriterion(contrary);
        }
        return super.negate();
    }

    protected BinaryCriterion constructNegatedCriterion(Operator negatedOperator) {
        return new BinaryCriterion(field, negatedOperator, value);
    }
}
