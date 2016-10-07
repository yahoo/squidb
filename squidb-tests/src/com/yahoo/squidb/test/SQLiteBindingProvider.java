/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.test;

import com.yahoo.squidb.data.ISQLiteOpenHelper;
import com.yahoo.squidb.data.SquidDatabase;

public abstract class SQLiteBindingProvider {

    private static SQLiteBindingProvider sProvider = null;

    public static void setSQLiteBindingProvider(SQLiteBindingProvider provider) {
        sProvider = provider;
    }

    public static SQLiteBindingProvider getInstance() {
        return sProvider;
    }

    public abstract ISQLiteOpenHelper createOpenHelper(String databaseName,
            SquidDatabase.OpenHelperDelegate delegate, int version);

    public abstract String getWriteableTestDir();

}
