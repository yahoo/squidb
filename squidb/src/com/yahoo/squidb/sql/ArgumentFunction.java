/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.sql;

class ArgumentFunction<TYPE> extends Function<TYPE> {

    private final String functionName;
    private final Object[] arguments;

    ArgumentFunction(String functionName, Object... arguments) {
        super();
        this.functionName = functionName;
        this.arguments = arguments;
    }

    @Override
    protected void appendFunctionExpression(SqlBuilder builder, boolean forSqlValidation) {
        builder.sql.append(functionName).append("(");
        appendArgumentList(builder, arguments, forSqlValidation);
        builder.sql.append(")");
    }

    protected void appendArgumentList(SqlBuilder builder, Object[] arguments, boolean forSqlValidation) {
        for (int i = 0; i < arguments.length; i++) {
            if (i > 0) {
                builder.sql.append(separator());
            }
            builder.addValueToSql(arguments[i], forSqlValidation);
        }
    }

    protected String separator() {
        return ", ";
    }

}
