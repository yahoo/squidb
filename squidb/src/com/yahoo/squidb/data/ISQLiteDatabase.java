/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.data;

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
