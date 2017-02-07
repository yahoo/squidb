/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.sql;

import javax.annotation.Nonnull;

class RawFunction<TYPE> extends Function<TYPE> {

    public RawFunction(@Nonnull String expression) {
        super(expression);
    }

    @Override
    protected void appendFunctionExpression(@Nonnull SqlBuilder builder, boolean forSqlValidation) {
        builder.sql.append(expression);
    }

    @Override
    @Nonnull
    public String getExpression() {
        return expression;
    }
}
