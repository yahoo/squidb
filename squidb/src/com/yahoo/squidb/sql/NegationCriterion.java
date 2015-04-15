/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.sql;

import java.util.List;

class NegationCriterion extends Criterion {

    private final Criterion toNegate;

    NegationCriterion(Criterion toNegate) {
        super(Operator.not);
        this.toNegate = toNegate;
    }

    @Override
    protected void populate(StringBuilder sql, List<Object> selectionArgsBuilder) {
        sql.append(operator);
        toNegate.appendCompiledStringWithArguments(sql, selectionArgsBuilder);
    }

    @Override
    public Criterion negate() {
        return toNegate;
    }
}
