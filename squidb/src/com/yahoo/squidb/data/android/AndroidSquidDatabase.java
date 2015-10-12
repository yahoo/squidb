/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.data.android;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import com.yahoo.squidb.data.SquidDatabase;
import com.yahoo.squidb.data.TableModel;
import com.yahoo.squidb.sql.Criterion;
import com.yahoo.squidb.sql.TableStatement;

import java.util.Collection;

/**
 * An Android-specific extension of SquidDatabase
 *
 * Methods that use String arrays for where clause arguments ({@link #update(String, ContentValues, String, String[])
 * update}, {@link #updateWithOnConflict(String, ContentValues, String, String[], int) updateWithOnConflict}, and
 * {@link #delete(String, String, String[]) delete}) are wrappers around Android's {@link SQLiteDatabase} methods.
 * However, Android's default behavior of binding all arguments as strings can have unexpected bugs, particularly when
 * working with SQLite functions. For example:
 *
 * <pre>
 * select * from t where _id = '1'; // Returns the first row
 * select * from t where abs(_id) = '1'; // Always returns empty set
 * </pre>
 *
 * For this reason, these methods are protected rather than public. You can choose to expose them in your database
 * subclass if you wish, but we recommend that you instead use the typesafe, public, model-bases methods, such as
 * {@link #update(Criterion, TableModel)}, {@link #updateWithOnConflict(Criterion, TableModel,
 * TableStatement.ConflictAlgorithm)}, {@link #delete(Class, long)}, and {@link #deleteWhere(Class, Criterion)}.
 * <p>
 */
public abstract class AndroidSquidDatabase extends SquidDatabase {

    public AndroidSquidDatabase(Context context) {
        super(context);
    }

    /**
     * @see SQLiteDatabase#insert(String table, String nullColumnHack, ContentValues values)
     */
    protected long insert(String table, String nullColumnHack, ContentValues values) {
        acquireNonExclusiveLock();
        try {
            return getDatabase().insert(table, nullColumnHack, values);
        } finally {
            releaseNonExclusiveLock();
        }
    }

    /**
     * @see SQLiteDatabase#insertOrThrow(String table, String nullColumnHack, ContentValues values)
     */
    protected long insertOrThrow(String table, String nullColumnHack, ContentValues values) {
        acquireNonExclusiveLock();
        try {
            return getDatabase().insertOrThrow(table, nullColumnHack, values);
        } finally {
            releaseNonExclusiveLock();
        }
    }

    /**
     * @see SQLiteDatabase#insertWithOnConflict(String, String, android.content.ContentValues, int)
     */
    protected long insertWithOnConflict(String table, String nullColumnHack, ContentValues values,
            int conflictAlgorithm) {
        acquireNonExclusiveLock();
        try {
            return getDatabase().insertWithOnConflict(table, nullColumnHack, values, conflictAlgorithm);
        } finally {
            releaseNonExclusiveLock();
        }
    }

    /**
     * See the note at the top of this file about the potential bugs when using String[] whereArgs
     *
     * @see SQLiteDatabase#update(String table, ContentValues values, String whereClause, String[] whereArgs)
     */
    protected int update(String table, ContentValues values, String whereClause, String[] whereArgs) {
        acquireNonExclusiveLock();
        try {
            return getDatabase().update(table, values, whereClause, whereArgs);
        } finally {
            releaseNonExclusiveLock();
        }
    }

    /**
     * See the note at the top of this file about the potential bugs when using String[] whereArgs
     *
     * @see SQLiteDatabase#updateWithOnConflict(String table, ContentValues values, String whereClause, String[]
     * whereArgs, int conflictAlgorithm)
     */
    protected int updateWithOnConflict(String table, ContentValues values, String whereClause, String[] whereArgs,
            int conflictAlgorithm) {
        acquireNonExclusiveLock();
        try {
            return getDatabase().updateWithOnConflict(table, values, whereClause, whereArgs, conflictAlgorithm);
        } finally {
            releaseNonExclusiveLock();
        }
    }

    /**
     * See the note at the top of this file about the potential bugs when using String[] whereArgs
     *
     * @see SQLiteDatabase#delete(String, String, String[])
     */
    protected int delete(String table, String whereClause, String[] whereArgs) {
        acquireNonExclusiveLock();
        try {
            return getDatabase().delete(table, whereClause, whereArgs);
        } finally {
            releaseNonExclusiveLock();
        }
    }

    /**
     * Convenience method for calling {@link ContentResolver#notifyChange(Uri, android.database.ContentObserver)
     * ContentResolver.notifyChange(uri, null)}.
     *
     * @param uri the Uri to notify
     */
    public void notifyChange(Uri uri) {
        context.getContentResolver().notifyChange(uri, null);
    }

    /**
     * Convenience method for calling {@link ContentResolver#notifyChange(Uri, android.database.ContentObserver)
     * ContentResolver.notifyChange(uri, null)} on all the provided Uris.
     *
     * @param uris the Uris to notify
     */
    public void notifyChange(Collection<Uri> uris) {
        if (uris != null && !uris.isEmpty()) {
            ContentResolver resolver = context.getContentResolver();
            for (Uri uri : uris) {
                resolver.notifyChange(uri, null);
            }
        }
    }
}
