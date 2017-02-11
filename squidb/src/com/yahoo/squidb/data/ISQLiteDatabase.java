/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.data;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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

    void beginTransactionWithListener(@Nonnull SquidTransactionListener listener);

    void beginTransactionWithListenerNonExclusive(@Nonnull SquidTransactionListener listener);

    void setTransactionSuccessful();

    void endTransaction();

    boolean inTransaction();

    boolean yieldIfContendedSafely();

    boolean yieldIfContendedSafely(long sleepAfterYieldDelay);

    int getVersion();

    void setVersion(int version);

    @Nonnull
    ICursor rawQuery(@Nonnull String sql, @Nullable Object[] bindArgs);

    @Nullable
    String simpleQueryForString(@Nonnull String sql, @Nullable Object[] bindArgs);

    long simpleQueryForLong(@Nonnull String sql, @Nullable Object[] bindArgs);

    long executeInsert(@Nonnull String sql, @Nullable Object[] bindArgs);

    int executeUpdateDelete(@Nonnull String sql, @Nullable Object[] bindArgs);

    void execSQL(@Nonnull String sql);

    void execSQL(@Nonnull String sql, @Nullable Object[] bindArgs);

    void ensureSqlCompiles(@Nonnull String sql);

    @Nonnull
    ISQLitePreparedStatement prepareStatement(@Nonnull String sql);

    boolean isOpen();

    void close();

    void disableWriteAheadLogging();

    boolean enableWriteAheadLogging();

    boolean isWriteAheadLoggingEnabled();

    long getMaximumSize();

    long getPageSize();

    @Nonnull
    String getPath();

    boolean isDatabaseIntegrityOk();

    boolean isDbLockedByCurrentThread();

    boolean isReadOnly();

    boolean needUpgrade(int newVersion);

    void setForeignKeyConstraintsEnabled(boolean enable);

    void setMaxSqlCacheSize(int cacheSize);

    void setMaximumSize(long numBytes);

    void setPageSize(long numBytes);

    @Nonnull
    Object getWrappedObject();

}
