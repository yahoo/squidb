/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.sql;

import java.util.List;

class BinaryCriterion extends Criterion {

    protected final Field<?> field;
    protected final Object value;

    BinaryCriterion(Field<?> expression, Operator operator, Object value) {
        super(operator);
        this.field = expression;
        this.value = value;
    }

    @Override
    protected void populate(StringBuilder sql, List<Object> selectionArgsBuilder) {
        beforePopulateOperator(sql, selectionArgsBuilder);
        populateOperator(sql);
        afterPopulateOperator(sql, selectionArgsBuilder);
    }

    protected void beforePopulateOperator(StringBuilder sql, List<Object> selectionArgsBuilder) {
        field.appendQualifiedExpression(sql, selectionArgsBuilder);
    }

    protected void populateOperator(StringBuilder sql) {
        sql.append(operator);
    }

    protected void afterPopulateOperator(StringBuilder sql, List<Object> selectionArgsBuilder) {
        SqlUtils.addToSqlString(sql, selectionArgsBuilder, value);
    }

    @Override
    public Criterion negate() {
        Operator contrary = operator.getContrary();
        if (contrary != null) {
            return new BinaryCriterion(field, contrary, value);
        }
        return super.negate();
    }
}
