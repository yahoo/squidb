/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.data.adapter;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.database.sqlite.SQLiteTransactionListener;
import android.util.Pair;

import java.util.List;
import java.util.Locale;

/**
 * This interface declares all the public methods of {@link android.database.sqlite.SQLiteDatabase} so that wrapper
 * classes can implement a common interface. For example, by default SquiDB can wrap the Android SQLiteDatabase, but
 * for custom SQLite builds, it will wrap the SQLite Android binding's version of the same class. Both wrappers will
 * implement this interface so they can be used interchangeably.
 */
public interface SQLiteDatabaseWrapper {

    /**
     * @see SQLiteDatabase#beginTransaction()
     */
    void beginTransaction();

    /**
     * @see SQLiteDatabase#beginTransactionNonExclusive()
     */
    void beginTransactionNonExclusive();

    /**
     * @param listener the transaction listener (as defined by SquiDB)
     * @see SQLiteDatabase#beginTransactionWithListener(SQLiteTransactionListener)
     */
    void beginTransactionWithListener(SquidTransactionListener listener);

    /**
     * @param listener the transaction listener (as defined by SquiDB)
     * @see SQLiteDatabase#beginTransactionWithListenerNonExclusive(SQLiteTransactionListener)
     */
    void beginTransactionWithListenerNonExclusive(SquidTransactionListener listener);

    /**
     * @see SQLiteDatabase#delete(String, String, String[])
     */
    int delete(String table, String whereClause, String[] whereArgs);

    /**
     * @see SQLiteDatabase#disableWriteAheadLogging()
     */
    void disableWriteAheadLogging();

    /**
     * @see SQLiteDatabase#enableWriteAheadLogging()
     */
    boolean enableWriteAheadLogging();

    /**
     * @see SQLiteDatabase#endTransaction()
     */
    void endTransaction();

    /**
     * @see SQLiteDatabase#execSQL(String)
     */
    void execSQL(String sql) throws SQLExceptionWrapper;

    /**
     * @see SQLiteDatabase#execSQL(String, Object[])
     */
    void execSQL(String sql, Object[] bindArgs) throws SQLExceptionWrapper;

    /**
     * @see SQLiteDatabase#getAttachedDbs()
     */
    List<Pair<String, String>> getAttachedDbs();

    /**
     * @see SQLiteDatabase#getMaximumSize()
     */
    long getMaximumSize();

    /**
     * @see SQLiteDatabase#getPageSize()
     */
    long getPageSize();

    /**
     * @see SQLiteDatabase#getPath()
     */
    String getPath();

    /**
     * @see SQLiteDatabase#getVersion()
     */
    int getVersion();

    /**
     * @see SQLiteDatabase#inTransaction()
     */
    boolean inTransaction();

    /**
     * @see SQLiteDatabase#insert(String, String, ContentValues)
     */
    long insert(String table, String nullColumnHack, ContentValues values);

    /**
     * @see SQLiteDatabase#insertOrThrow(String, String, ContentValues)
     */
    long insertOrThrow(String table, String nullColumnHack, ContentValues values);

    /**
     * @see SQLiteDatabase#insertWithOnConflict(String, String, ContentValues, int)
     */
    long insertWithOnConflict(String table, String nullColumnHack, ContentValues initialValues, int conflictAlgorithm);

    /**
     * @see SQLiteDatabase#isDatabaseIntegrityOk()
     */
    boolean isDatabaseIntegrityOk();

    /**
     * @see SQLiteDatabase#isDbLockedByCurrentThread()
     */
    boolean isDbLockedByCurrentThread();

    /**
     * @see SQLiteDatabase#isDbLockedByOtherThreads()
     */
    @Deprecated
    boolean isDbLockedByOtherThreads();

    /**
     * @see SQLiteDatabase#isOpen()
     */
    boolean isOpen();

    /**
     * @see SQLiteDatabase#isReadOnly()
     */
    boolean isReadOnly();

    /**
     * @see SQLiteDatabase#isWriteAheadLoggingEnabled()
     */
    boolean isWriteAheadLoggingEnabled();

    /**
     * @see SQLiteDatabase#needUpgrade(int)
     */
    boolean needUpgrade(int newVersion);

    /**
     * Note: rawQuery is one of the few methods that is not directly wrapped. This is because SquiDB requires providing
     * a custom CursorFactory to bind arguments as their native types, but CursorFactory is a class that may be
     * namespaced differently for custom SQLite builds/bindings. For this reason, we declare only a version of rawQuery
     * that is compatible with SquidDatabase's version of it, and require implementers to bind the arguments natively
     * themselves.
     *
     * @see com.yahoo.squidb.data.SquidDatabase#rawQuery(String, Object[])
     */
    Cursor rawQuery(String sql, Object[] bindArgs);

    /**
     * @see SQLiteDatabase#replace(String, String, ContentValues)
     */
    long replace(String table, String nullColumnHack, ContentValues initialValues);

    /**
     * @see SQLiteDatabase#replaceOrThrow(String, String, ContentValues)
     */
    long replaceOrThrow(String table, String nullColumnHack, ContentValues initialValues);

    /**
     * @see SQLiteDatabase#setForeignKeyConstraintsEnabled(boolean)
     */
    void setForeignKeyConstraintsEnabled(boolean enable);

    /**
     * @see SQLiteDatabase#setLocale(Locale)
     */
    void setLocale(Locale locale);

    /**
     * @see SQLiteDatabase#setLockingEnabled(boolean)
     */
    @Deprecated
    void setLockingEnabled(boolean lockingEnabled);

    /**
     * @see SQLiteDatabase#setMaxSqlCacheSize(int)
     */
    void setMaxSqlCacheSize(int cacheSize);

    /**
     * @see SQLiteDatabase#setMaximumSize(long)
     */
    void setMaximumSize(long numBytes);

    /**
     * @see SQLiteDatabase#setPageSize(long)
     */
    void setPageSize(long numBytes);

    /**
     * @see SQLiteDatabase#setTransactionSuccessful()
     */
    void setTransactionSuccessful();

    /**
     * @see SQLiteDatabase#setVersion(int)
     */
    void setVersion(int version);

    /**
     * @see SQLiteDatabase#toString()
     */
    String toString();

    /**
     * @see SQLiteDatabase#update(String, ContentValues, String, String[])
     */
    int update(String table, ContentValues values, String whereClause, String[] whereArgs);

    /**
     * @see SQLiteDatabase#updateWithOnConflict(String, ContentValues, String, String[], int)
     */
    int updateWithOnConflict(String table, ContentValues values, String whereClause, String[] whereArgs,
            int conflictAlgorithm);

    /**
     * @see SQLiteDatabase#yieldIfContendedSafely(long)
     */
    boolean yieldIfContendedSafely(long sleepAfterYieldDelay);

    /**
     * @see SQLiteDatabase#yieldIfContendedSafely(long)
     */
    boolean yieldIfContendedSafely();

    // From SQLiteCloseable

    /**
     * @see SQLiteDatabase#acquireReference()
     */
    void acquireReference();

    /**
     * @see SQLiteDatabase#close()
     */
    void close();

    /**
     * @see SQLiteDatabase#releaseReference()
     */
    void releaseReference();

    /**
     * @see SQLiteDatabase#releaseReferenceFromContainer()
     */
    @Deprecated
    void releaseReferenceFromContainer();

    /**
     * Implementers compile the SQL into the equivalent of {@link SQLiteStatement} and call simpleQueryForString();
     *
     * @see SQLiteStatement#simpleQueryForString()
     */
    String simpleQueryForString(String sql, Object[] bindArgs);

    /**
     * * Implementers compile the SQL into the equivalent of {@link SQLiteStatement} and call executeUpdateDelete();
     *
     * @see SQLiteStatement#executeUpdateDelete()
     */
    int executeUpdateDelete(String sql, Object[] bindArgs);

    /**
     * * Implementers compile the SQL into the equivalent of {@link SQLiteStatement} and call executeInsert();
     *
     * @see SQLiteStatement#executeInsert()
     */
    long executeInsert(String sql, Object[] bindArgs);

    /**
     * Implementers should throw an exception if the provided SQL is not syntactically valid
     */
    void ensureSqlCompiles(String sql);

    /**
     * @return the wrapped database instance, e.g. an instance of {@link SQLiteDatabase}. Callers will need to check
     * the type of this object and cast it if they intend to use it directly.
     */
    Object getWrappedDatabase();

}
