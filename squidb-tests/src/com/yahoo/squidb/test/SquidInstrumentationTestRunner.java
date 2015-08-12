/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.test;

import android.os.Bundle;
import android.test.InstrumentationTestRunner;


public class SquidInstrumentationTestRunner extends InstrumentationTestRunner {

    private static final String KEY_USE_SQLITE_BINDINGS = "use_sqlite_bindings";

    public static boolean useSqliteBindings = false;

    @Override
    public void onCreate(Bundle arguments) {
        if (arguments != null && arguments.containsKey(KEY_USE_SQLITE_BINDINGS)) {
            useSqliteBindings = Boolean.parseBoolean(arguments.getString(KEY_USE_SQLITE_BINDINGS));
        }
        super.onCreate(arguments);
    }
}
