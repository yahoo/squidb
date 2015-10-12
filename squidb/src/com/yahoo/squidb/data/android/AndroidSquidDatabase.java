/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.data.android;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;

import com.yahoo.squidb.data.SquidDatabase;
import com.yahoo.squidb.data.adapter.SQLiteOpenHelperWrapper;

import java.util.Collection;

/**
 * An Android-specific extension of SquidDatabase
 */
public abstract class AndroidSquidDatabase extends SquidDatabase {

    private final Context context;

    public AndroidSquidDatabase(Context context, SQLiteOpenHelperWrapper openHelper) {
        super(openHelper);
        if (context == null) {
            throw new NullPointerException("Null context creating SquidDatabase");
        }
        this.context = context.getApplicationContext();
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
