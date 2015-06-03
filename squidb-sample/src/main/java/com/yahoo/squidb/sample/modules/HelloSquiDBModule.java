/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.sample.modules;

import android.content.Context;

import com.yahoo.squidb.data.DatabaseDao;
import com.yahoo.squidb.sample.HelloSquiDBApplication;
import com.yahoo.squidb.sample.TaskListActivity;
import com.yahoo.squidb.sample.database.TasksDatabase;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module(injects = {TaskListActivity.class})
public class HelloSquiDBModule {

    private Context applicationContext;

    HelloSquiDBModule(HelloSquiDBApplication app) {
        this.applicationContext = app;
    }

    @Provides
    @ForApplicaton
    Context provideContext() {
        return applicationContext;
    }

    @Provides
    @Singleton
    DatabaseDao provideDatabaseDao(@ForApplicaton Context context) {
        // Since we want the wrapped TasksDatabase to be a singleton, DatabaseDao is also a singleton
        return new DatabaseDao(new TasksDatabase(context));
    }

}
