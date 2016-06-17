/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.ios;

import com.yahoo.android.sqlite.DatabaseUtils;
import com.yahoo.android.sqlite.SQLiteCursor;
import com.yahoo.android.sqlite.SQLiteCursorDriver;
import com.yahoo.android.sqlite.SQLiteDatabase;
import com.yahoo.android.sqlite.SQLiteProgram;
import com.yahoo.android.sqlite.SQLiteQuery;
import com.yahoo.squidb.data.ICursor;
import com.yahoo.squidb.sql.SqlUtils;

/**
 * A custom cursor factory that ensures query arguments are bound as their native types, rather than as strings. The
 * {@link com.yahoo.squidb.data.SquidDatabase SquidDatabase} documentation notes why this is important.
 *
 * @see com.yahoo.squidb.data.SquidDatabase SquidDatabase
 */
public class SquidCursorFactory implements SQLiteDatabase.CursorFactory {

    private final Object[] sqlArgs;

    public SquidCursorFactory(Object[] sqlArgs) {
        this.sqlArgs = sqlArgs;
    }

    @Override
    public ICursor newCursor(SQLiteDatabase db, SQLiteCursorDriver masterQuery, String editTable, SQLiteQuery query) {
        bindArgumentsToProgram(query, sqlArgs);
        return new SQLiteCursor(masterQuery, editTable, query);
    }

    public static void bindArgumentsToProgram(SQLiteProgram program, Object[] sqlArgs) {
        if (sqlArgs == null) {
            return;
        }
        for (int i = 1; i <= sqlArgs.length; i++) {
            Object arg = SqlUtils.resolveArgReferences(sqlArgs[i - 1]);
            DatabaseUtils.bindObjectToProgram(program, i, arg);
        }
    }
}
