/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.test;

import android.os.Build;
import android.os.Bundle;
import android.test.AndroidTestRunner;
import android.test.InstrumentationTestRunner;


public class SquidInstrumentationTestRunner extends InstrumentationTestRunner {

    private static final String KEY_USE_SQLITE_BINDINGS = "use_sqlite_bindings";

    private boolean hadArguments = false;
    private static final boolean RUN_TESTS_WITH_SQLITE_BINDINGS = false;
    public static boolean useSqliteBindings = false;

    @Override
    public void onCreate(Bundle arguments) {
        if (arguments != null && arguments.containsKey(KEY_USE_SQLITE_BINDINGS)) {
            hadArguments = true;
            useSqliteBindings = Boolean.parseBoolean(arguments.getString(KEY_USE_SQLITE_BINDINGS));
        }
        super.onCreate(arguments);
    }

    @Override
    protected AndroidTestRunner getAndroidTestRunner() {
        return new SquidTestRunner(RUN_TESTS_WITH_SQLITE_BINDINGS &&
                !hadArguments && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN);
    }
}
