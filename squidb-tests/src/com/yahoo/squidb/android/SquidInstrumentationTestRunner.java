/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.android;

import android.os.Bundle;
import android.test.AndroidTestRunner;
import android.test.InstrumentationTestRunner;

import com.yahoo.squidb.android.SquidTestRunner.SquidbBinding;

public class SquidInstrumentationTestRunner extends InstrumentationTestRunner {

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

    private SquidbBinding binding;

    @Override
    public void onCreate(Bundle arguments) {
        String binding = DEFAULT_BINDING;
        if (arguments != null) {
            binding = arguments.getString(KEY_SQUIDB_BINDING, binding);
        }
        this.binding = SquidbBinding.valueOf(binding.toUpperCase());
        ContextProvider.setContext(getTargetContext());
        super.onCreate(arguments);
    }

    @Override
    protected AndroidTestRunner getAndroidTestRunner() {
        return new SquidTestRunner(binding);
    }
}
