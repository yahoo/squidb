/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.sql;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

class BetweenCriterion extends BinaryCriterion {

    private final Object lower;
    private final Object upper;

    BetweenCriterion(@Nonnull Field<?> expression, @Nonnull Operator operator,
            @Nullable Object lower, @Nullable Object upper) {
        super(expression, operator, null);
        this.lower = lower;
        this.upper = upper;
    }

    @Override
    protected void afterPopulateOperator(@Nonnull SqlBuilder builder, boolean forSqlValidation) {
        builder.addValueToSql(lower, forSqlValidation);
        builder.sql.append(" AND ");
        builder.addValueToSql(upper, forSqlValidation);
    }

    @Override
    @Nonnull
    protected BinaryCriterion constructNegatedCriterion(@Nonnull Operator negatedOperator) {
        return new BetweenCriterion(field, negatedOperator, lower, upper);
    }
}
