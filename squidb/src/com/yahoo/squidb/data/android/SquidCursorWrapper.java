/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.data.android;

import android.database.Cursor;
import android.database.CursorWrapper;

import com.yahoo.squidb.data.ICursor;

public class SquidCursorWrapper extends CursorWrapper implements ICursor {

    public SquidCursorWrapper(Cursor cursor) {
        super(cursor);
    }
}
