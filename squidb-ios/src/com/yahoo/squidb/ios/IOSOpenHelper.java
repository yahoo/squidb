/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.ios;

import com.yahoo.android.sqlite.SQLiteDatabase;
import com.yahoo.android.sqlite.SQLiteOpenHelper;
import com.yahoo.squidb.data.ISQLiteDatabase;
import com.yahoo.squidb.data.ISQLiteOpenHelper;
import com.yahoo.squidb.data.SquidDatabase;

import java.io.File;
import java.io.IOException;

/**
 * ISQLiteOpenHelper implementation that wraps the iOS port of SQLiteOpenHelper. When on iOS, returning an instance of
 * this class in {@link SquidDatabase#createOpenHelper(String, SquidDatabase.OpenHelperDelegate, int)} will connect
 * SquidDatabase to the iOS SQLiteDatabase port contained in this module.
 */
public class IOSOpenHelper extends SQLiteOpenHelper implements ISQLiteOpenHelper {

    private final SquidDatabase.OpenHelperDelegate delegate;

    public IOSOpenHelper(String path, String name, SquidDatabase.OpenHelperDelegate delegate,
            int version) {
        super(path, name, null, version);
        this.delegate = delegate;
    }

    @Override
    public ISQLiteDatabase openForWriting() {
        SQLiteDatabase database = super.getWritableDatabase();
        return new IOSSQLiteDatabaseAdapter(database);
    }

    @Override
    public String getDatabasePath() {
        return getDatabaseFile().getAbsolutePath();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        delegate.onCreate(new IOSSQLiteDatabaseAdapter(db));
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        delegate.onUpgrade(new IOSSQLiteDatabaseAdapter(db), oldVersion, newVersion);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        delegate.onDowngrade(new IOSSQLiteDatabaseAdapter(db), oldVersion, newVersion);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        delegate.onConfigure(new IOSSQLiteDatabaseAdapter(db));
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        delegate.onOpen(new IOSSQLiteDatabaseAdapter(db));
    }

    @Override
    public synchronized void deleteDatabase() {
        File file = getDatabaseFile();
        if (file.exists() && !file.delete()) {
            throw new RuntimeException(new IOException("Failed to delete database file"));
        }
    }
}
