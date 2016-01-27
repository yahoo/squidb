/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.data;

import com.yahoo.squidb.json.JSONFunctions;
import com.yahoo.squidb.test.DatabaseTestCase;

public class JSONTestCase extends DatabaseTestCase {

    protected void testJsonFunction(Runnable toTest) {
        testForMinVersionCode(JSONFunctions.JSON1_MIN_VERSION, toTest);
    }
}
