/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.sql;

class NegationCriterion extends Criterion {

    private final Criterion toNegate;

    NegationCriterion(Criterion toNegate) {
        super(Operator.not);
        if (toNegate == null) {
            throw new IllegalArgumentException("Can't negate a null criterion");
        }
        this.toNegate = toNegate;
    }

    @Override
    protected void populate(SqlBuilder builder, boolean forSqlValidation) {
        builder.sql.append(operator);
        toNegate.appendToSqlBuilder(builder, forSqlValidation);
    }

    @Override
    public Criterion negate() {
        return toNegate;
    }
}
