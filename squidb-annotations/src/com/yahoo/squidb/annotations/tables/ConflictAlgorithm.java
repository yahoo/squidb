/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.annotations.tables;

/**
 * Algorithms used by SQLite when a constraint violation occurs during an insert or update operation.
 * <p>
 * Note: Users should take care not to confuse this enum definition with TableStatement.ConflictAlgorithm in SquiDB
 * core. This enum is used for compile-time annotations, while TableStatement.ConflictAlgorithm is used at runtime
 * and in the public APIs of SquiDB core.
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
