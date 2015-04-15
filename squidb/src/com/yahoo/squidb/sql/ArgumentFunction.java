/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.sql;

import java.util.List;

class ArgumentFunction<TYPE> extends Function<TYPE> {

    private final String functionName;
    private final Object[] arguments;

    public ArgumentFunction(String functionName, Object... arguments) {
        super();
        this.functionName = functionName;
        this.arguments = arguments;
    }

    @Override
    protected void appendFunctionExpression(StringBuilder sql, List<Object> selectionArgsBuilder) {
        sql.append(functionName).append("(");
        appendArgumentList(sql, selectionArgsBuilder, arguments);
        sql.append(")");
    }

    protected void appendArgumentList(StringBuilder sql, List<Object> selectionArgsBuilder, Object[] arguments) {
        for (int i = 0; i < arguments.length; i++) {
            if (i > 0) {
                sql.append(separator());
            }
            SqlUtils.addToSqlString(sql, selectionArgsBuilder, arguments[i]);
        }
    }

    protected String separator() {
        return ", ";
    }

}
