/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.test;

import android.content.Context;
import android.os.Build;
import android.test.AndroidTestRunner;

import com.yahoo.squidb.data.SquidDatabase.OpenHelperDelegate;
import com.yahoo.squidb.data.adapter.DefaultOpenHelperWrapper;
import com.yahoo.squidb.data.adapter.SQLiteOpenHelperWrapper;
import com.yahoo.squidb.sqlitebindings.SQLiteBindingsOpenHelperWrapper;
import com.yahoo.squidb.utility.Logger;

public class SquidTestRunner extends AndroidTestRunner {

    private static final boolean TEST_ALL_BINDINGS = false;

    public static SquidbBinding selectedBinding;

    public SquidTestRunner(SquidbBinding binding) {
        selectedBinding = binding;
    }

    @Override
    public void runTest() {
        if (TEST_ALL_BINDINGS && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            for (SquidbBinding binding : SquidbBinding.values()) {
                runTest(binding);
            }
        } else {
            // run selected binding only
            runTest(selectedBinding);
        }
    }

    private void runTest(SquidbBinding binding) {
        Logger.d("SquidTestRunner running tests with " + binding + " binding");
        selectedBinding = binding;
        super.runTest();
    }

    public enum SquidbBinding {
        ANDROID {
            @Override
            public SQLiteOpenHelperWrapper getOpenHelper(Context context, String databaseName,
                    OpenHelperDelegate delegate, int version) {
                return new DefaultOpenHelperWrapper(context, databaseName, delegate, version);
            }
        },
        SQLITE {
            @Override
            public SQLiteOpenHelperWrapper getOpenHelper(Context context, String databaseName,
                    OpenHelperDelegate delegate, int version) {
                return new SQLiteBindingsOpenHelperWrapper(context, databaseName, delegate, version);
            }
        };

        abstract public SQLiteOpenHelperWrapper getOpenHelper(Context context, String databaseName,
                OpenHelperDelegate delegate, int version);
    }
}
