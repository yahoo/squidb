/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.sqlitebindings;

import com.yahoo.squidb.android.SquidCursorWrapper;
import com.yahoo.squidb.data.ICursor;
import com.yahoo.squidb.data.ISQLiteDatabase;
import com.yahoo.squidb.data.SQLExceptionWrapper;
import com.yahoo.squidb.data.SquidTransactionListener;

import org.sqlite.database.SQLException;
import org.sqlite.database.sqlite.SQLiteDatabase;
import org.sqlite.database.sqlite.SQLiteStatement;
import org.sqlite.database.sqlite.SQLiteTransactionListener;

public class SQLiteBindingsAdapter implements ISQLiteDatabase {

    private final SQLiteDatabase db;

    public SQLiteBindingsAdapter(SQLiteDatabase db) {
        if (db == null) {
            throw new NullPointerException("Can't create SQLiteDatabaseAdapter with a null SQLiteDatabase");
        }
        this.db = db;
    }

    private static class SQLiteTransactionListenerAdapter implements SQLiteTransactionListener {

        private final SquidTransactionListener listener;

        private SQLiteTransactionListenerAdapter(SquidTransactionListener listener) {
            this.listener = listener;
        }

        @Override
        public void onBegin() {
            listener.onBegin();
        }

        @Override
        public void onCommit() {
            listener.onCommit();
        }

        @Override
        public void onRollback() {
            listener.onRollback();
        }
    }

    @Override
    public void beginTransaction() {
        db.beginTransaction();
    }

    @Override
    public void beginTransactionNonExclusive() {
        db.beginTransactionNonExclusive();
    }

    @Override
    public void beginTransactionWithListener(SquidTransactionListener listener) {
        db.beginTransactionWithListener(new SQLiteTransactionListenerAdapter(listener));
    }

    @Override
    public void beginTransactionWithListenerNonExclusive(SquidTransactionListener listener) {
        db.beginTransactionWithListenerNonExclusive(new SQLiteTransactionListenerAdapter(listener));
    }

    @Override
    public void endTransaction() {
        db.endTransaction();
    }

    @Override
    public void execSQL(String sql) throws SQLExceptionWrapper {
        try {
            db.execSQL(sql);
        } catch (SQLException e) {
            throw new SQLExceptionWrapper(e);
        }
    }

    @Override
    public void execSQL(String sql, Object[] bindArgs) throws SQLExceptionWrapper {
        try {
            db.execSQL(sql, bindArgs);
        } catch (SQLException e) {
            throw new SQLExceptionWrapper(e);
        }
    }

    @Override
    public boolean inTransaction() {
        return db.inTransaction();
    }

    @Override
    public boolean isOpen() {
        return db.isOpen();
    }

    @Override
    public int getVersion() {
        return db.getVersion();
    }

    @Override
    public void setVersion(int version) {
        db.setVersion(version);
    }

    @Override
    public ICursor rawQuery(String sql, Object[] bindArgs) {
        return new SquidCursorWrapper(
                db.rawQueryWithFactory(new SQLiteBindingsCursorFactory(bindArgs), sql, null, null));
    }

    @Override
    public void setTransactionSuccessful() {
        db.setTransactionSuccessful();
    }

    @Override
    public String toString() {
        return db.toString();
    }

    @Override
    public boolean yieldIfContendedSafely() {
        return db.yieldIfContendedSafely();
    }

    @Override
    public void close() {
        db.close();
    }

    @Override
    public int executeUpdateDelete(String sql, Object[] bindArgs) {
        SQLiteStatement statement = null;
        try {
            statement = db.compileStatement(sql);
            SQLiteBindingsCursorFactory.bindArgumentsToProgram(statement, bindArgs);
            return statement.executeUpdateDelete();
        } finally {
            if (statement != null) {
                statement.close();
            }
        }
    }

    @Override
    public long executeInsert(String sql, Object[] bindArgs) {
        SQLiteStatement statement = null;
        try {
            statement = db.compileStatement(sql);
            SQLiteBindingsCursorFactory.bindArgumentsToProgram(statement, bindArgs);
            return statement.executeInsert();
        } finally {
            if (statement != null) {
                statement.close();
            }
        }
    }

    @Override
    public void ensureSqlCompiles(String sql) {
        SQLiteStatement statement = null;
        try {
            statement = db.compileStatement(sql);
        } finally {
            if (statement != null) {
                statement.close();
            }
        }
    }

    @Override
    public SQLiteDatabase getWrappedObject() {
        return db;
    }
}
