/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.sql;

class MathFunction<TYPE> extends ArgumentFunction<TYPE> {

    private final MathOperator operator;

    MathFunction(MathOperator operator, Object... args) {
        super("", args);
        this.operator = operator;
    }

    @Override
    protected String separator() {
        return operator.toString();
    }
}
