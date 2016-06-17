/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.sample.database;

import com.yahoo.squidb.data.ISQLiteOpenHelper;
import com.yahoo.squidb.data.SquidDatabase;

public abstract class OpenHelperCreator {

    private static OpenHelperCreator sCreator = null;

    public static OpenHelperCreator getCreator() {
        return sCreator;
    }

    public static void setCreator(OpenHelperCreator creator) {
        sCreator = creator;
    }

    protected abstract ISQLiteOpenHelper createOpenHelper(String databaseName,
            SquidDatabase.OpenHelperDelegate delegate, int version);

}
