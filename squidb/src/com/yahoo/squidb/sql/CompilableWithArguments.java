/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.sql;

import com.yahoo.squidb.utility.VersionCode;

import javax.annotation.Nonnull;

abstract class CompilableWithArguments {

    // A version code suitable for converting a SQL object to a string for debugging or comparison purposes
    static final VersionCode VERSION_FOR_TO_STRING = new VersionCode(3, 15, 0, 0);

    @Override
    @Nonnull
    public String toString() {
        return toRawSql(CompileContext.defaultContextForVersionCode(VERSION_FOR_TO_STRING));
    }

    @Nonnull
    public final String toRawSql(@Nonnull CompileContext compileContext) {
        return buildSql(compileContext, false, false).getSqlString();
    }

    @Nonnull
    protected final SqlBuilder buildSql(@Nonnull CompileContext compileContext, boolean withBoundArguments,
            boolean forSqlValidation) {
        SqlBuilder builder = new SqlBuilder(compileContext, withBoundArguments);
        appendToSqlBuilder(builder, forSqlValidation);
        return builder;
    }

    abstract void appendToSqlBuilder(@Nonnull SqlBuilder builder, boolean forSqlValidation);

}
