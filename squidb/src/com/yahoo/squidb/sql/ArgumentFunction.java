/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.sql;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

class ArgumentFunction<TYPE> extends Function<TYPE> {

    private final String functionName;
    private final Object[] arguments;

    ArgumentFunction(@Nonnull String functionName, @Nonnull Object... arguments) {
        super();
        this.functionName = functionName;
        this.arguments = arguments;
    }

    @Override
    protected void appendFunctionExpression(@Nonnull SqlBuilder builder, boolean forSqlValidation) {
        builder.sql.append(functionName).append("(");
        appendArgumentList(builder, arguments, forSqlValidation);
        builder.sql.append(")");
    }

    protected void appendArgumentList(@Nonnull SqlBuilder builder, @Nonnull Object[] arguments,
            boolean forSqlValidation) {
        for (int i = 0; i < arguments.length; i++) {
            if (i > 0) {
                builder.sql.append(separator());
            }
            builder.addValueToSql(arguments[i], forSqlValidation);
        }
    }

    @Nullable
    protected String separator() {
        return ", ";
    }

}
