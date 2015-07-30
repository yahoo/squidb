/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.utility;

import android.annotation.SuppressLint;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteCursor;
import android.database.sqlite.SQLiteCursorDriver;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteProgram;
import android.database.sqlite.SQLiteQuery;
import android.os.Build;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A custom cursor factory that ensures query arguments are bound as their native types, rather than as strings. The
 * {@link com.yahoo.squidb.data.AbstractDatabase AbstractDatabase} documentation notes why this is important.
 *
 * @see com.yahoo.squidb.data.AbstractDatabase
 */
public class SquidCursorFactory implements CursorFactory {

    private final Object[] sqlArgs;

    public SquidCursorFactory(Object[] sqlArgs) {
        this.sqlArgs = sqlArgs;
    }

    @Override
    @SuppressWarnings("deprecation")
    @SuppressLint("NewApi")
    public Cursor newCursor(SQLiteDatabase db, SQLiteCursorDriver masterQuery, String editTable, SQLiteQuery query) {
        bindArgumentsToProgram(query, sqlArgs);

        if (Build.VERSION.SDK_INT < 11) {
            return new SQLiteCursor(db, masterQuery, editTable, query);
        } else {
            return new SQLiteCursor(masterQuery, editTable, query);
        }
    }


    public static void bindArgumentsToProgram(SQLiteProgram program, Object[] sqlArgs) {
        if (sqlArgs == null) {
            return;
        }
        for (int i = 1; i <= sqlArgs.length; i++) {
            Object arg = sqlArgs[i - 1];
            if (arg instanceof AtomicBoolean) { // Not a subclass of Number so DatabaseUtils won't handle it
                arg = ((AtomicBoolean) arg).get() ? 1 : 0;
            }
            DatabaseUtils.bindObjectToProgram(program, i, arg);
        }
    }
}
