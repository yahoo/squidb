/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.android;

import android.content.Context;
import android.os.Bundle;
import android.support.test.runner.AndroidJUnitRunner;

import com.yahoo.squidb.data.ISQLiteOpenHelper;
import com.yahoo.squidb.data.JSONPropertyTest;
import com.yahoo.squidb.data.SquidDatabase;
import com.yahoo.squidb.sqlitebindings.SQLiteBindingsOpenHelper;
import com.yahoo.squidb.test.SQLiteBindingProvider;
import com.yahoo.squidb.utility.Logger;

public class SquidInstrumentationTestRunner extends AndroidJUnitRunner {

    /**
     * Command line option specifying which binding to use for the test database. Argument value is case insensitive
     * and accepts the following values:
     * <ul>
     * <li>android: Use the standard Android SDK bindings (for the standard Android SQLiteDatabase)</li>
     * <li>sqlite: Use the org.sqlite bindings (for use with packaged SQLite binaries)</li>
     * </ul>
     * If not specified, the default is android.
     */
    private static final String KEY_SQUIDB_BINDING = "squidb_binding";

    private static final String DEFAULT_BINDING = SquidbBinding.ANDROID.name();

    public static SquidbBinding selectedBinding;

    @Override
    public void onCreate(Bundle arguments) {
        String binding = DEFAULT_BINDING;
        Logger.setLogger(new AndroidLogger());
        if (arguments != null) {
            binding = arguments.getString(KEY_SQUIDB_BINDING, binding);
        }
        selectedBinding = SquidbBinding.valueOf(binding.toUpperCase());
        SQLiteBindingProvider.setSQLiteBindingProvider(new SQLiteBindingProvider() {
            @Override
            public ISQLiteOpenHelper createOpenHelper(String databaseName, SquidDatabase.OpenHelperDelegate delegate,
                    int version) {
                return selectedBinding.getOpenHelper(getTargetContext(), databaseName, delegate, version);
            }

            @Override
            public String getWriteableTestDir() {
                return getTargetContext().getFilesDir().getPath();
            }
        });
        ContextProvider.setContext(getTargetContext());
        JSONPropertyTest.MAPPERS = AndroidJSONMappers.MAPPERS;
        super.onCreate(arguments);
    }


    public enum SquidbBinding {
        ANDROID {
            @Override
            public ISQLiteOpenHelper getOpenHelper(Context context, String databaseName,
                    SquidDatabase.OpenHelperDelegate delegate, int version) {
                return new AndroidOpenHelper(context, databaseName, delegate, version);
            }
        },
        SQLITE {
            @Override
            public ISQLiteOpenHelper getOpenHelper(Context context, String databaseName,
                    SquidDatabase.OpenHelperDelegate delegate, int version) {
                return new SQLiteBindingsOpenHelper(context, databaseName, delegate, version);
            }
        };

        abstract ISQLiteOpenHelper getOpenHelper(Context context, String databaseName,
                SquidDatabase.OpenHelperDelegate delegate, int version);
    }
}
