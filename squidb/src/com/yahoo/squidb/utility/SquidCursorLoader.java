/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.utility;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.content.CursorLoader;
import android.database.Cursor;
import android.net.Uri;

import com.yahoo.squidb.data.AbstractModel;
import com.yahoo.squidb.data.SquidCursor;
import com.yahoo.squidb.data.SquidDatabase;
import com.yahoo.squidb.sql.Query;

/**
 * A {@link CursorLoader} that queries a {@link SquidDatabase}
 */
public class SquidCursorLoader<T extends AbstractModel> extends AsyncTaskLoader<SquidCursor<T>> {

    private final Query query;
    private final SquidDatabase database;
    private Uri notificationUri = null;
    private SquidCursor<T> cursor = null;
    private final Class<T> modelClass;

    private final ForceLoadContentObserver observer = new ForceLoadContentObserver();

    public SquidCursorLoader(Context context, SquidDatabase database, Class<T> modelClass, Query query) {
        super(context);
        this.database = database;
        this.query = query;
        this.modelClass = modelClass;
    }

    /**
     * Provide a {@link Uri} to be set as the notification Uri on the cursor once it is loaded
     */
    public void setNotificationUri(Uri uri) {
        this.notificationUri = uri;
    }

    @Override
    public SquidCursor<T> loadInBackground() {
        SquidCursor<T> result = database.query(modelClass, query);
        if (result != null) {
            result.getCount(); // Make sure the window is filled
            Cursor androidResult = (Cursor) result.getCursor();
            androidResult.registerContentObserver(observer);
            if (notificationUri != null) {
                androidResult.setNotificationUri(getContext().getContentResolver(), notificationUri);
            }
        }
        return result;
    }

    @Override
    public void deliverResult(SquidCursor<T> data) {
        if (isReset()) {
            if (data != null) {
                data.close();
            }
            return;
        }

        SquidCursor<T> oldCursor = this.cursor;
        this.cursor = data;

        if (isStarted()) {
            super.deliverResult(data);
        }

        if (oldCursor != null && oldCursor != data && !oldCursor.isClosed()) {
            oldCursor.close();
        }
    }

    @Override
    protected void onStartLoading() {
        if (cursor != null) {
            deliverResult(cursor);
        }

        if (takeContentChanged() || cursor == null) {
            forceLoad();
        }
    }

    @Override
    protected void onStopLoading() {
        cancelLoad();
    }

    @Override
    public void onCanceled(SquidCursor<T> data) {
        if (data != null && !data.isClosed()) {
            data.close();
        }
    }

    @Override
    protected void onReset() {
        super.onReset();
        // Ensure the loader is stopped
        onStopLoading();

        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }

        cursor = null;
    }

}
