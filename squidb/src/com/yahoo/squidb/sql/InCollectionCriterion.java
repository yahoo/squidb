/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.sql;

import java.util.Collection;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

class InCollectionCriterion extends BinaryCriterion {

    private final Collection<?> collection;

    InCollectionCriterion(@Nonnull Field<?> expression, @Nonnull Operator operator, @Nullable Collection<?> value) {
        super(expression, operator, value);
        this.collection = value;
    }

    @Override
    protected void afterPopulateOperator(@Nonnull SqlBuilder builder, boolean forSqlValidation) {
        builder.sql.append("(");
        builder.addCollectionArg(collection);
        builder.sql.append(")");
    }

    @Override
    @Nonnull
    protected BinaryCriterion constructNegatedCriterion(@Nonnull Operator negatedOperator) {
        return new InCollectionCriterion(field, negatedOperator, collection);
    }
}
