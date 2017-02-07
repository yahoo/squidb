/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.sql;

import javax.annotation.Nonnull;

class DistinctArgumentFunction<TYPE> extends ArgumentFunction<TYPE> {

    /**
     * DISTINCT functions only allow one argument, so we enforce that here
     */
    public DistinctArgumentFunction(@Nonnull String functionName, @Nonnull Object argument) {
        super(functionName, argument);
    }

    @Override
    protected void appendArgumentList(@Nonnull SqlBuilder builder, @Nonnull Object[] arguments, boolean forSqlValidation) {
        builder.sql.append("DISTINCT ");
        super.appendArgumentList(builder, arguments, forSqlValidation);
    }
}
