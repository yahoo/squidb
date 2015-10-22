/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.android;

import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.database.sqlite.SQLiteTransactionListener;
import android.os.Build;

import com.yahoo.squidb.data.ICursor;
import com.yahoo.squidb.data.ISQLiteDatabase;
import com.yahoo.squidb.data.SQLExceptionWrapper;
import com.yahoo.squidb.data.SquidTransactionListener;

/**
 * Wrapper for the default Android {@link SQLiteDatabase} that implements the common {@link ISQLiteDatabase}
 * interface.
 */
public class SQLiteDatabaseAdapter implements ISQLiteDatabase {

    private final SQLiteDatabase db;

    public SQLiteDatabaseAdapter(SQLiteDatabase db) {
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
    public void close() {
        db.close();
    }

    @Override
    public void disableWriteAheadLogging() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            db.disableWriteAheadLogging();
        } else {
            throw new UnsupportedOperationException("disableWriteAheadLogging not supported on API < 16");
        }
    }

    @Override
    public boolean enableWriteAheadLogging() {
        return db.enableWriteAheadLogging();
    }

    @Override
    public boolean isWriteAheadLoggingEnabled() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            return db.isWriteAheadLoggingEnabled();
        } else {
            throw new UnsupportedOperationException("isWriteAheadLoggingEnabled not supported on API < 16");
        }
    }

    @Override
    public long getMaximumSize() {
        return db.getMaximumSize();
    }

    @Override
    public long getPageSize() {
        return db.getPageSize();
    }

    @Override
    public String getPath() {
        return db.getPath();
    }

    @Override
    public boolean isDatabaseIntegrityOk() {
        return db.isDatabaseIntegrityOk();
    }

    @Override
    public boolean isDbLockedByCurrentThread() {
        return db.isDbLockedByCurrentThread();
    }

    @Override
    public boolean isReadOnly() {
        return db.isReadOnly();
    }

    @Override
    public boolean needUpgrade(int newVersion) {
        return db.needUpgrade(newVersion);
    }

    @Override
    public void setForeignKeyConstraintsEnabled(boolean enable) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            db.setForeignKeyConstraintsEnabled(enable);
        } else {
            throw new UnsupportedOperationException("setForeignKeyConstraintsEnabled not supported on API < 16");
        }
    }

    @Override
    public void setMaxSqlCacheSize(int cacheSize) {
        db.setMaxSqlCacheSize(cacheSize);
    }

    @Override
    public void setMaximumSize(long numBytes) {
        db.setMaximumSize(numBytes);
    }

    @Override
    public void setPageSize(long numBytes) {
        db.setPageSize(numBytes);
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
        return new SquidCursorWrapper(db.rawQueryWithFactory(new SquidCursorFactory(bindArgs), sql, null, null));
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
    public boolean yieldIfContendedSafely(long sleepAfterYieldDelay) {
        return db.yieldIfContendedSafely(sleepAfterYieldDelay);
    }

    @Override
    public int executeUpdateDelete(String sql, Object[] bindArgs) {
        SQLiteStatement statement = null;
        try {
            statement = db.compileStatement(sql);
            SquidCursorFactory.bindArgumentsToProgram(statement, bindArgs);
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
            SquidCursorFactory.bindArgumentsToProgram(statement, bindArgs);
            return statement.executeInsert();
        } finally {
            if (statement != null) {
                statement.close();
            }
        }
    }

    @Override
    public void ensureSqlCompiles(String sql) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            SQLiteStatement statement = null;
            try {
                statement = db.compileStatement(sql);
            } finally {
                if (statement != null) {
                    statement.close();
                }
            }
        } else {
            Cursor c = db.rawQuery(sql, null);
            if (c != null) {
                c.close();
            }
        }
    }

    @Override
    public SQLiteDatabase getWrappedObject() {
        return db;
    }
}
