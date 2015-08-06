/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.sql;

abstract class CompilableWithArguments {

    @Override
    public String toString() {
        return toRawSql();
    }

    public final String toRawSql() {
        return buildSql(false, false).getSqlString();
    }

    protected final SqlBuilder buildSql(boolean withBoundArguments, boolean forSqlValidation) {
        SqlBuilder builder = new SqlBuilder(withBoundArguments);
        appendToSqlBuilder(builder, forSqlValidation);
        return builder;
    }

    abstract void appendToSqlBuilder(SqlBuilder builder, boolean forSqlValidation);

}
