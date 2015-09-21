/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.sql;

enum MathOperator {
    PLUS(" + "), MINUS(" - "), MULT(" * "), DIVIDE(" / "), MODULO(" % "), BITWISE_AND(" & "), BITWISE_OR(" | ");

    private final String value;

    MathOperator(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
