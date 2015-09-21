/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.data.adapter;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.database.sqlite.SQLiteTransactionListener;
import android.os.Build;
import android.util.Pair;

import java.util.List;
import java.util.Locale;

/**
 * Wrapper for the default Android {@link SQLiteDatabase} that implements the common {@link SQLiteDatabaseWrapper}
 * interface.
 */
public class SQLiteDatabaseAdapter implements SQLiteDatabaseWrapper {

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
    public int delete(String table, String whereClause, String[] whereArgs) {
        return db.delete(table, whereClause, whereArgs);
    }

    @Override
    public void disableWriteAheadLogging() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            db.disableWriteAheadLogging();
        } else {
            throw new UnsupportedOperationException("disableWriteAheadLogging() is not supported on API < 16");
        }
    }

    @Override
    public boolean enableWriteAheadLogging() {
        return db.enableWriteAheadLogging();
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
    public List<Pair<String, String>> getAttachedDbs() {
        return db.getAttachedDbs();
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
    public int getVersion() {
        return db.getVersion();
    }

    @Override
    public boolean inTransaction() {
        return db.inTransaction();
    }

    @Override
    public long insert(String table, String nullColumnHack, ContentValues values) {
        return db.insert(table, nullColumnHack, values);
    }

    @Override
    public long insertOrThrow(String table, String nullColumnHack, ContentValues values) {
        return db.insertOrThrow(table, nullColumnHack, values);
    }

    @Override
    public long insertWithOnConflict(String table, String nullColumnHack, ContentValues initialValues,
            int conflictAlgorithm) {
        return db.insertWithOnConflict(table, nullColumnHack, initialValues, conflictAlgorithm);
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
    @Deprecated
    public boolean isDbLockedByOtherThreads() {
        return db.isDbLockedByOtherThreads();
    }

    @Override
    public boolean isOpen() {
        return db.isOpen();
    }

    @Override
    public boolean isReadOnly() {
        return db.isReadOnly();
    }

    @Override
    public boolean isWriteAheadLoggingEnabled() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            return db.isWriteAheadLoggingEnabled();
        } else {
            throw new UnsupportedOperationException("isWriteAheadLoggingEnabled() is not supported on API < 16");
        }
    }

    @Override
    public boolean needUpgrade(int newVersion) {
        return db.needUpgrade(newVersion);
    }

    @Override
    public Cursor rawQuery(String sql, Object[] bindArgs) {
        return db.rawQueryWithFactory(new SquidCursorFactory(bindArgs), sql, null, null);
    }

    @Override
    public long replace(String table, String nullColumnHack, ContentValues initialValues) {
        return db.replace(table, nullColumnHack, initialValues);
    }

    @Override
    public long replaceOrThrow(String table, String nullColumnHack, ContentValues initialValues) {
        return db.replaceOrThrow(table, nullColumnHack, initialValues);
    }

    @Override
    public void setForeignKeyConstraintsEnabled(boolean enable) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            db.setForeignKeyConstraintsEnabled(enable);
        } else {
            throw new UnsupportedOperationException("setForeignKeyConstraintsEnabled() is not supported on API < 16");
        }
    }

    @Override
    public void setLocale(Locale locale) {
        db.setLocale(locale);
    }

    @Override
    @Deprecated
    public void setLockingEnabled(boolean lockingEnabled) {
        db.setLockingEnabled(lockingEnabled);
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
    public void setTransactionSuccessful() {
        db.setTransactionSuccessful();
    }

    @Override
    public void setVersion(int version) {
        db.setVersion(version);
    }

    @Override
    public String toString() {
        return db.toString();
    }

    @Override
    public int update(String table, ContentValues values, String whereClause, String[] whereArgs) {
        return db.update(table, values, whereClause, whereArgs);
    }

    @Override
    public int updateWithOnConflict(String table, ContentValues values, String whereClause, String[] whereArgs,
            int conflictAlgorithm) {
        return db.updateWithOnConflict(table, values, whereClause, whereArgs, conflictAlgorithm);
    }

    @Override
    public boolean yieldIfContendedSafely(long sleepAfterYieldDelay) {
        return db.yieldIfContendedSafely(sleepAfterYieldDelay);
    }

    @Override
    public boolean yieldIfContendedSafely() {
        return db.yieldIfContendedSafely();
    }

    @Override
    public void acquireReference() {
        db.acquireReference();
    }

    @Override
    public void close() {
        db.close();
    }

    @Override
    public void releaseReference() {
        db.releaseReference();
    }

    @Override
    @Deprecated
    public void releaseReferenceFromContainer() {
        db.releaseReferenceFromContainer();
    }

    @Override
    public String simpleQueryForString(String sql, Object[] bindArgs) {
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
    public SQLiteDatabase getWrappedDatabase() {
        return db;
    }
}
