/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.sql;

/**
 * A compiled SQLite statement
 */
public class CompiledStatement {

    /** The raw SQL of this statement. May contain '?' placeholders for SQL arguments. */
    public final String sql;
    /** Arguments that will bind to replaceable characters in the statement. */
    public final Object[] sqlArgs;

    public CompiledStatement(String sql, Object[] args) {
        this.sql = sql;
        this.sqlArgs = args;
    }
}
