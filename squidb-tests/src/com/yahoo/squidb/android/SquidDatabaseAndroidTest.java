/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.android;

import com.yahoo.squidb.sqlitebindings.SQLiteBindingsAdapter;
import com.yahoo.squidb.test.DatabaseTestCase;

public class SquidDatabaseAndroidTest extends DatabaseTestCase {

    public void testVersionForCustomBinding() {
        if (SquidInstrumentationTestRunner.selectedBinding == SquidInstrumentationTestRunner.SquidbBinding.SQLITE) {
            assertEquals(SQLiteBindingsAdapter.SQLITE_VERSION, database.getSqliteVersion());
        }
    }
}
