/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.data.adapter;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;

import com.yahoo.squidb.data.ISQLiteDatabase;
import com.yahoo.squidb.data.SquidDatabase;

/**
 * SQLiteOpenHelperWrapper implementation for a standard Android SQLiteOpenHelper. By default, SquidDatabase uses
 * this class to access a standard Android SQLiteDatabase.
 */
public class DefaultOpenHelperWrapper extends SQLiteOpenHelper implements SQLiteOpenHelperWrapper {

    private final Context context;
    private final SquidDatabase.OpenHelperDelegate delegate;

    public DefaultOpenHelperWrapper(Context context, String name, SquidDatabase.OpenHelperDelegate delegate,
            int version) {
        super(context, name, null, version);
        this.context = context.getApplicationContext();
        this.delegate = delegate;
    }

    @Override
    public ISQLiteDatabase openForWriting() {
        SQLiteDatabase database = super.getWritableDatabase();
        return new SQLiteDatabaseAdapter(database);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        delegate.onCreate(new SQLiteDatabaseAdapter(db));
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        delegate.onUpgrade(new SQLiteDatabaseAdapter(db), oldVersion, newVersion);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        delegate.onDowngrade(new SQLiteDatabaseAdapter(db), oldVersion, newVersion);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        delegate.onConfigure(new SQLiteDatabaseAdapter(db));
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        SQLiteDatabaseAdapter adapter = new SQLiteDatabaseAdapter(db);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            delegate.onConfigure(adapter);
        }
        delegate.onOpen(adapter);
    }

    @Override
    public void deleteDatabase(String databaseName) {
        context.deleteDatabase(databaseName);
    }

    @Override
    public String getDatabasePath(String databaseName) {
        return context.getDatabasePath(databaseName).getAbsolutePath();
    }
}
