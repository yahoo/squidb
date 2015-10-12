/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.data;

/**
 * Common interface for helper classes that open the database to implement, e.g.
 * {@link com.yahoo.squidb.data.android.DefaultOpenHelperWrapper} wraps
 * {@link android.database.sqlite.SQLiteOpenHelper SQLiteOpenHelper} and implements this interface.
 */
public interface SQLiteOpenHelperWrapper {

    ISQLiteDatabase openForWriting();

    String getDatabasePath(String databaseName);

    void deleteDatabase(String databaseName);

    void close();

}
