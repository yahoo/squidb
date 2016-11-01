/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.android;

import com.yahoo.squidb.sqlitebindings.SQLiteBindingsVersion;
import com.yahoo.squidb.test.DatabaseTestCase;

public class SquidDatabaseAndroidTest extends DatabaseTestCase {

    public void testVersionForCustomBinding() {
        if (SquidInstrumentationTestRunner.selectedBinding == SquidInstrumentationTestRunner.SquidbBinding.SQLITE) {
            assertEquals(SQLiteBindingsVersion.VERSION, database.getSqliteVersion());
        }
    }
}
