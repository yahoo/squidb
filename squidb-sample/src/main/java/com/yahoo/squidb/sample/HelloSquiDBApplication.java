/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.sample;

import android.app.Application;

import com.yahoo.squidb.sample.modules.HelloSquiDBInjector;

public class HelloSquiDBApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        HelloSquiDBInjector.init(this);
    }
}
