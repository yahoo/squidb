/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.sql;

import java.util.List;

class DistinctArgumentFunction<TYPE> extends ArgumentFunction<TYPE> {

    /**
     * DISTINCT functions only allow one argument, so we enforce that here
     */
    public DistinctArgumentFunction(String functionName, Object argument) {
        super(functionName, argument);
    }

    @Override
    protected void appendArgumentList(StringBuilder sql, List<Object> selectionArgsBuilder, Object[] arguments) {
        sql.append("DISTINCT ");
        super.appendArgumentList(sql, selectionArgsBuilder, arguments);
    }
}
