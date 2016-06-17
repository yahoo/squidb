/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.sql;

import com.yahoo.squidb.utility.VersionCode;

/**
 * A statement that operates on a {@link SqlTable}
 */
public abstract class TableStatement extends CompilableWithArguments implements SqlStatement {

    /**
     * Algorithms used by SQLite when a constraint violation occurs during an insert or update operation
     *
     * @see <a href="http://www.sqlite.org/lang_conflict.html">http://www.sqlite.org/lang_conflict.html</a>
     */
    public enum ConflictAlgorithm {
        /**
         * No conflict algorithm specified.
         */
        NONE,
        /**
         * Aborts the command and reverts any changes made within the same transaction up to this point.
         */
        ROLLBACK,
        /**
         * Aborts the command and reverts any changes made by this command up to this point. Changes made by other
         * commands in the same transaction as this command and which have already completed are preserved.
         */
        ABORT,
        /**
         * Aborts the command, but any changes to the database that the command made prior to encountering the
         * constraint violation are preserved.
         */
        FAIL,
        /**
         * When a constraint violation occurs, the one row that contains the constraint violation is not inserted or
         * changed. Other rows before and after the row that contained the constraint violation continue to be inserted
         * or updated normally.
         */
        IGNORE,
        /**
         * When a UNIQUE constraint violation occurs, the pre-existing row that caused the constraint violation is
         * removed prior to inserting or updating the current row.
         */
        REPLACE
    }

    private CompiledArgumentResolver compiledArgumentResolver = null;

    @Override
    public final synchronized CompiledStatement compile(VersionCode sqliteVersion) {
        if (compiledArgumentResolver == null) {
            SqlBuilder builder = buildSql(sqliteVersion, true, false);
            compiledArgumentResolver = new CompiledArgumentResolver(builder);
        }
        return compiledArgumentResolver.resolveToCompiledStatement();
    }

    public final String sqlForValidation(VersionCode sqliteVersion) {
        SqlBuilder builder = buildSql(sqliteVersion, true, true);
        return new CompiledArgumentResolver(builder).resolveToCompiledStatement().sql;
    }

    public abstract SqlTable<?> getTable();

    /**
     * Invalidate the compile cache for this statement
     */
    protected final synchronized void invalidateCompileCache() {
        compiledArgumentResolver = null;
    }
}
