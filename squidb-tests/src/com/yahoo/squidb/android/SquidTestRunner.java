/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.android;

import android.content.Context;
import android.os.Build;
import android.test.AndroidTestRunner;

import com.yahoo.squidb.data.ISQLiteOpenHelper;
import com.yahoo.squidb.data.SquidDatabase.OpenHelperDelegate;
import com.yahoo.squidb.sqlitebindings.SQLiteBindingsOpenHelper;
import com.yahoo.squidb.test.SQLiteBindingProvider;
import com.yahoo.squidb.utility.Logger;

public class SquidTestRunner extends AndroidTestRunner {

    private static final boolean TEST_ALL_BINDINGS = false;

    public static SquidbBinding selectedBinding;

    public SquidTestRunner(SquidbBinding binding) {
        selectedBinding = binding;
        SQLiteBindingProvider.setSQLiteBindingProvider(new SQLiteBindingProvider() {
            @Override
            public ISQLiteOpenHelper createOpenHelper(String databaseName, OpenHelperDelegate delegate,
                    int version) {
                return selectedBinding.getOpenHelper(ContextProvider.getContext(), databaseName, delegate, version);
            }
        });
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
        Logger.d(Logger.LOG_TAG, "SquidTestRunner running tests with " + binding + " binding");
        selectedBinding = binding;
        super.runTest();
    }

    public enum SquidbBinding {
        ANDROID {
            @Override
            public ISQLiteOpenHelper getOpenHelper(Context context, String databaseName,
                    OpenHelperDelegate delegate, int version) {
                return new AndroidOpenHelper(context, databaseName, delegate, version);
            }
        },
        SQLITE {
            @Override
            public ISQLiteOpenHelper getOpenHelper(Context context, String databaseName,
                    OpenHelperDelegate delegate, int version) {
                return new SQLiteBindingsOpenHelper(context, databaseName, delegate, version);
            }
        };

        abstract ISQLiteOpenHelper getOpenHelper(Context context, String databaseName,
                OpenHelperDelegate delegate, int version);
    }
}
