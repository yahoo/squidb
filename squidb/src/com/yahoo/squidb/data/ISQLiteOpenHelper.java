/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.data;

/**
 * Common interface for helper classes that open the database to implement, e.g.
 * com.yahoo.squidb.android.AndroidOpenHelper in the squidb-android module wraps
 * android.database.sqlite.SQLiteOpenHelper and implements this interface.
 */
public interface ISQLiteOpenHelper {

    ISQLiteDatabase openForWriting();

    String getDatabasePath();

    boolean deleteDatabase();

    void close();

}
