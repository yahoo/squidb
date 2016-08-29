/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.sqlitebindings;

import android.database.Cursor;

import org.sqlite.database.sqlite.SQLiteCursor;
import org.sqlite.database.sqlite.SQLiteCursorDriver;
import org.sqlite.database.sqlite.SQLiteDatabase;
import org.sqlite.database.sqlite.SQLiteDatabase.CursorFactory;
import org.sqlite.database.sqlite.SQLiteProgram;
import org.sqlite.database.sqlite.SQLiteQuery;

/**
 * A custom cursor factory that ensures query arguments are bound as their native types, rather than as strings. The
 * {@link com.yahoo.squidb.data.SquidDatabase SquidDatabase} documentation notes why this is important.
 *
 * @see com.yahoo.squidb.data.SquidDatabase SquidDatabase
 */
public class SQLiteBindingsCursorFactory implements CursorFactory {

    private final Object[] sqlArgs;

    public SQLiteBindingsCursorFactory(Object[] sqlArgs) {
        this.sqlArgs = sqlArgs;
    }

    @Override
    public Cursor newCursor(SQLiteDatabase db, SQLiteCursorDriver masterQuery, String editTable, SQLiteQuery query) {
        bindArgumentsToProgram(query, sqlArgs);
        return new SQLiteCursor(masterQuery, editTable, query);
    }

    public static void bindArgumentsToProgram(SQLiteProgram program, Object[] sqlArgs) {
        if (sqlArgs == null) {
            return;
        }
        for (int i = 1; i <= sqlArgs.length; i++) {
            bindObjectToProgram(program, i, sqlArgs[i - 1]);
        }
    }

    // borrowed from android.database.DatabaseUtils
    public static void bindObjectToProgram(SQLiteProgram prog, int index, Object value) {
        if (value == null) {
            prog.bindNull(index);
        } else if (value instanceof Double || value instanceof Float) {
            prog.bindDouble(index, ((Number) value).doubleValue());
        } else if (value instanceof Number) {
            prog.bindLong(index, ((Number) value).longValue());
        } else if (value instanceof Boolean) {
            Boolean bool = (Boolean) value;
            if (bool) {
                prog.bindLong(index, 1);
            } else {
                prog.bindLong(index, 0);
            }
        } else if (value instanceof byte[]) {
            prog.bindBlob(index, (byte[]) value);
        } else {
            prog.bindString(index, value.toString());
        }
    }
}
