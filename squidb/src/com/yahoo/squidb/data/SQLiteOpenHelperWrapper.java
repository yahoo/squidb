/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.data;

/**
 * Common interface for helper classes that open the database to implement, e.g.
 * {@link com.yahoo.squidb.android.AndroidOpenHelperWrapper} wraps
 * {@link android.database.sqlite.SQLiteOpenHelper SQLiteOpenHelper} and implements this interface.
 */
public interface SQLiteOpenHelperWrapper {

    ISQLiteDatabase openForWriting();

    String getDatabasePath();

    void deleteDatabase();

    void close();

}
