/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.ios;

import com.yahoo.android.sqlite.SQLiteDatabase;
import com.yahoo.android.sqlite.SQLiteStatement;
import com.yahoo.android.sqlite.SQLiteTransactionListener;
import com.yahoo.squidb.data.ICursor;
import com.yahoo.squidb.data.ISQLiteDatabase;
import com.yahoo.squidb.data.ISQLitePreparedStatement;
import com.yahoo.squidb.data.SquidTransactionListener;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Wrapper for the iOS port of SQLiteDatabase that implements the common {@link ISQLiteDatabase} interface.
 */
public class IOSSQLiteDatabaseAdapter implements ISQLiteDatabase {

    private final SQLiteDatabase db;

    public IOSSQLiteDatabaseAdapter(SQLiteDatabase db) {
        if (db == null) {
            throw new NullPointerException("Can't create SQLiteDatabaseAdapter with a null SQLiteDatabase");
        }
        this.db = db;
    }

    private static class SQLiteTransactionListenerAdapter implements SQLiteTransactionListener {

        private final SquidTransactionListener listener;

        private SQLiteTransactionListenerAdapter(@Nonnull SquidTransactionListener listener) {
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
    public void beginTransactionWithListener(@Nonnull SquidTransactionListener listener) {
        db.beginTransactionWithListener(new SQLiteTransactionListenerAdapter(listener));
    }

    @Override
    public void beginTransactionWithListenerNonExclusive(@Nonnull SquidTransactionListener listener) {
        db.beginTransactionWithListenerNonExclusive(new SQLiteTransactionListenerAdapter(listener));
    }

    @Override
    public void endTransaction() {
        db.endTransaction();
    }

    @Override
    public void execSQL(@Nonnull String sql) {
        db.execSQL(sql);
    }

    @Override
    public void execSQL(@Nonnull String sql, @Nullable Object[] bindArgs) {
        db.execSQL(sql, bindArgs);
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
        db.disableWriteAheadLogging();
    }

    @Override
    public boolean enableWriteAheadLogging() {
        return db.enableWriteAheadLogging();
    }

    @Override
    public boolean isWriteAheadLoggingEnabled() {
        return db.isWriteAheadLoggingEnabled();
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
    @Nonnull
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
        db.setForeignKeyConstraintsEnabled(enable);
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
    @Nonnull
    public ICursor rawQuery(@Nonnull String sql, @Nullable Object[] bindArgs) {
        return db.rawQueryWithFactory(new SquidCursorFactory(bindArgs), sql, null, null);
    }

    @Override
    public String simpleQueryForString(@Nonnull String sql, @Nullable Object[] bindArgs) {
        SQLiteStatement statement = null;
        try {
            statement = db.compileStatement(sql);
            SquidCursorFactory.bindArgumentsToProgram(statement, bindArgs);
            return statement.simpleQueryForString();
        } finally {
            if (statement != null) {
                statement.close();
            }
        }
    }

    @Override
    public long simpleQueryForLong(@Nonnull String sql, @Nullable Object[] bindArgs) {
        SQLiteStatement statement = null;
        try {
            statement = db.compileStatement(sql);
            SquidCursorFactory.bindArgumentsToProgram(statement, bindArgs);
            return statement.simpleQueryForLong();
        } finally {
            if (statement != null) {
                statement.close();
            }
        }
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
    public int executeUpdateDelete(@Nonnull String sql, @Nullable Object[] bindArgs) {
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
    public long executeInsert(@Nonnull String sql, @Nullable Object[] bindArgs) {
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
    public void ensureSqlCompiles(@Nonnull String sql) {
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
    @Nonnull
    public ISQLitePreparedStatement prepareStatement(@Nonnull String sql) {
        return new IOSSQLiteStatementAdapter(db.compileStatement(sql));
    }

    @Override
    @Nonnull
    public SQLiteDatabase getWrappedObject() {
        return db;
    }
}
