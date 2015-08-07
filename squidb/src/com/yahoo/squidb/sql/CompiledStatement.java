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
    /** Flag that will be set to true if the statement should be compiled again with additional parentheses to guard
     * against malicious SQL */
    public final boolean needsValidation;

    public CompiledStatement(String sql, Object[] args, boolean needsValidation) {
        this.sql = sql;
        this.sqlArgs = args;
        this.needsValidation = needsValidation;
    }
}
