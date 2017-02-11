/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.sql;

import javax.annotation.Nonnull;

class NegationCriterion extends Criterion {

    private final Criterion toNegate;

    NegationCriterion(@Nonnull Criterion toNegate) {
        super(Operator.not);
        if (toNegate == null) {
            throw new IllegalArgumentException("Can't negate a null criterion");
        }
        this.toNegate = toNegate;
    }

    @Override
    protected void populate(@Nonnull SqlBuilder builder, boolean forSqlValidation) {
        builder.sql.append(operator);
        toNegate.appendToSqlBuilder(builder, forSqlValidation);
    }

    @Override
    @Nonnull
    public Criterion negate() {
        return toNegate;
    }
}
