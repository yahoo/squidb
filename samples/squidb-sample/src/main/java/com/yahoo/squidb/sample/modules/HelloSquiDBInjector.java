/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.sample.modules;

import com.yahoo.squidb.sample.HelloSquiDBApplication;

import dagger.ObjectGraph;

public class HelloSquiDBInjector {

    private static HelloSquiDBInjector INSTANCE = null;
    private ObjectGraph mGraph;

    public static HelloSquiDBInjector getInstance() {
        if (INSTANCE == null) {
            throw new RuntimeException("Must call init(app) first");
        }
        return INSTANCE;
    }

    public static HelloSquiDBInjector init(HelloSquiDBApplication app) {
        INSTANCE = new HelloSquiDBInjector(app);
        return INSTANCE;
    }

    private HelloSquiDBInjector(HelloSquiDBApplication app) {
        mGraph = ObjectGraph.create(new HelloSquiDBModule(app));
    }

    public void inject(Object object) {
        mGraph.inject(object);
    }

}
