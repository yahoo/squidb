/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.sql;

import javax.annotation.Nonnull;

class QueryFunction<T> extends Function<T> {

    private final Query query;

    QueryFunction(@Nonnull Query query) {
        this.query = query;
    }

    @Override
    protected void appendFunctionExpression(@Nonnull SqlBuilder builder, boolean forSqlValidation) {
        builder.addValueToSql(query, forSqlValidation);
    }
}
