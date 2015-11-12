/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.data;

/**
 * This interface represents the general interface for a low-level SQLite access object. The interface is inspired by
 * android.database.sqlite.SQLiteDatabase and many of the methods declared here are taken from that class, although
 * some SquiDB-specific methods have been added and some declared in the Android class are not present in this
 * interface. Classes implementing this interface should strive to keep the same behavioral characteristics of
 * Android's SQLite database, and in fact, all SquiDB implementations of this interface are based on wrappers or forks
 * of the relevant Android classes.
 */
public interface ISQLiteDatabase {

    void beginTransaction();

    void beginTransactionNonExclusive();

    void beginTransactionWithListener(SquidTransactionListener listener);

    void beginTransactionWithListenerNonExclusive(SquidTransactionListener listener);

    void setTransactionSuccessful();

    void endTransaction();

    boolean inTransaction();

    boolean yieldIfContendedSafely();

    boolean yieldIfContendedSafely(long sleepAfterYieldDelay);

    int getVersion();

    void setVersion(int version);

    ICursor rawQuery(String sql, Object[] bindArgs);

    String simpleQueryForString(String sql, Object[] bindArgs);

    long simpleQueryForLong(String sql, Object[] bindArgs);

    long executeInsert(String sql, Object[] bindArgs);

    int executeUpdateDelete(String sql, Object[] bindArgs);

    void execSQL(String sql) throws SQLExceptionWrapper;

    void execSQL(String sql, Object[] bindArgs) throws SQLExceptionWrapper;

    void ensureSqlCompiles(String sql);

    boolean isOpen();

    void close();

    void disableWriteAheadLogging();

    boolean enableWriteAheadLogging();

    boolean isWriteAheadLoggingEnabled();

    long getMaximumSize();

    long getPageSize();

    String getPath();

    boolean isDatabaseIntegrityOk();

    boolean isDbLockedByCurrentThread();

    boolean isReadOnly();

    boolean needUpgrade(int newVersion);

    void setForeignKeyConstraintsEnabled(boolean enable);

    void setMaxSqlCacheSize(int cacheSize);

    void setMaximumSize(long numBytes);

    void setPageSize(long numBytes);

    Object getWrappedObject();

}
