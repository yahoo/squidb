/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.android;

import android.database.Cursor;
import android.database.CursorWrapper;

import com.yahoo.squidb.data.ICursor;

/**
 * Boilerplate for creating an Android {@link Cursor} that declares it also implements SquiDB's {@link ICursor}
 * interface
 */
public class SquidCursorWrapper extends CursorWrapper implements ICursor {

    public SquidCursorWrapper(Cursor cursor) {
        super(cursor);
    }
}
