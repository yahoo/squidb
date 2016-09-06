/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.data;

/**
 * This interface represents a low-level SQLite prepared statement (non-query). For situations where performance is
 * critical, a prepared statement can significantly improve performance by enabling binding argument directly to the
 * low-level native SQLite object before it is executed and reducing the need to recompile the same SQL over and over
 * again. This interface is analogous to the public interface of android.database.sqlite.SQLiteStatement.
 */
public interface ISQLitePreparedStatement {

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
