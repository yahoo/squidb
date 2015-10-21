/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.android;

import com.yahoo.squidb.test.DatabaseTestCase;
import com.yahoo.squidb.utility.VersionCode;

public class SquidDatabaseAndroidTest extends DatabaseTestCase {

    public void testVersionForCustomBinding() {
        if (SquidTestRunner.selectedBinding == SquidTestRunner.SquidbBinding.SQLITE) {
            assertEquals(VersionCode.LATEST, database.getSqliteVersion());
        }
    }

}
