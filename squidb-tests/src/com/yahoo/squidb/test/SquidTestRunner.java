/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.test;

import android.test.AndroidTestRunner;

public class SquidTestRunner extends AndroidTestRunner {

    private final boolean runTestsTwice;

    public SquidTestRunner(boolean runTestsTwice) {
        this.runTestsTwice = runTestsTwice;
    }

    @Override
    public void runTest() {
        super.runTest();
        if (runTestsTwice) {
            SquidInstrumentationTestRunner.useSqliteBindings = !SquidInstrumentationTestRunner.useSqliteBindings;
            super.runTest();
        }
    }
}
