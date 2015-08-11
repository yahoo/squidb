/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.sql;

import com.yahoo.squidb.utility.VersionCode;

abstract class CompilableWithArguments {

    @Override
    public String toString() {
        return toRawSql(VersionCode.LATEST);
    }

    public final String toRawSql(VersionCode sqliteVersion) {
        return buildSql(sqliteVersion, false, false).getSqlString();
    }

    protected final SqlBuilder buildSql(VersionCode sqliteVersion, boolean withBoundArguments,
            boolean forSqlValidation) {
        SqlBuilder builder = new SqlBuilder(sqliteVersion, withBoundArguments);
        appendToSqlBuilder(builder, forSqlValidation);
        return builder;
    }

    abstract void appendToSqlBuilder(SqlBuilder builder, boolean forSqlValidation);

}
