/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.sql;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

class CaseInsensitiveEqualsCriterion extends BinaryCriterion {

    CaseInsensitiveEqualsCriterion(@Nonnull Field<?> expression, @Nonnull Operator operator, @Nullable Object value) {
        super(expression, operator, value);
    }

    @Override
    protected void afterPopulateOperator(@Nonnull SqlBuilder builder, boolean forSqlValidation) {
        super.afterPopulateOperator(builder, forSqlValidation);
        builder.sql.append(" COLLATE NOCASE ");
    }

    @Override
    @Nonnull
    protected BinaryCriterion constructNegatedCriterion(@Nonnull Operator negatedOperator) {
        return new CaseInsensitiveEqualsCriterion(field, negatedOperator, value);
    }
}
