/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.sql;

import java.util.List;

class QueryFunction<T> extends Function<T> {

    private final Query query;

    QueryFunction(Query query) {
        this.query = query;
    }

    @Override
    protected void appendFunctionExpression(StringBuilder sql, List<Object> selectionArgsBuilder) {
        SqlUtils.addToSqlString(sql, selectionArgsBuilder, query);
    }
}
