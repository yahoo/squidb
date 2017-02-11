/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.sql;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

class BinaryCriterion extends Criterion {

    protected final Field<?> field;
    protected final Object value;

    BinaryCriterion(@Nonnull Field<?> expression, @Nonnull Operator operator, @Nullable Object value) {
        super(operator);
        this.field = expression;
        this.value = value;
    }

    @Override
    protected void populate(@Nonnull SqlBuilder builder, boolean forSqlValidation) {
        beforePopulateOperator(builder, forSqlValidation);
        populateOperator(builder.sql);
        afterPopulateOperator(builder, forSqlValidation);
    }

    protected void beforePopulateOperator(@Nonnull SqlBuilder builder, boolean forSqlValidation) {
        field.appendQualifiedExpression(builder, forSqlValidation);
    }

    protected void populateOperator(@Nonnull StringBuilder sql) {
        sql.append(operator);
    }

    protected void afterPopulateOperator(@Nonnull SqlBuilder builder, boolean forSqlValidation) {
        builder.addValueToSql(value, forSqlValidation);
    }

    @Override
    @Nonnull
    public Criterion negate() {
        Operator contrary = operator.getContrary();
        if (contrary != null) {
            return constructNegatedCriterion(contrary);
        }
        return super.negate();
    }

    @Nonnull
    protected BinaryCriterion constructNegatedCriterion(@Nonnull Operator negatedOperator) {
        return new BinaryCriterion(field, negatedOperator, value);
    }
}
