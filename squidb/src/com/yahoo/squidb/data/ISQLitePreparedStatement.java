/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.data;

/**
 * This interface represents a low-level SQLite prepared statement. For situations where performance is
 * critical, a prepared statement can significantly improve performance by enabling binding argument directly to the
 * low-level native SQLite object before it is executed and reducing the need to recompile the same SQL over and over
 * again. This interface is analogous to the public interface of android.database.sqlite.SQLiteStatement. Instances
 * of this class are tied to the database that compiled them -- you cannot use an instance compiled with one database
 * to execute the statement against a different database.
 * <p>
 * The statement must be either a non-query statement (INSERT/UPDATE/DELETE), or if it is a query it must return only
 * a 1x1 result (for use with {@link #simpleQueryForLong()} or {@link #simpleQueryForString()}).
 * <p>
 * Note that in all the bind methods, the indexes are 1-based instead of 0-based, to mimic the behavior of the SQLite
 * C API.
 */
public interface ISQLitePreparedStatement {

    /**
     * Close the prepared statement. This should be called to release resources after the statement is done being used.
     */
    void close();

    void bindNull(int index);

    void bindLong(int index, long value);

    void bindDouble(int index, double value);

    void bindString(int index, String value);

    void bindBlob(int index, byte[] value);

    void clearBindings();

    void execute();

    int executeUpdateDelete();

    long executeInsert();

    long simpleQueryForLong();

    String simpleQueryForString();

}
