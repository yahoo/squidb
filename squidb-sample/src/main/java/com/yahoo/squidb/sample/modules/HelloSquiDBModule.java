/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.sample.modules;

import android.content.Context;
import android.net.Uri;

import com.yahoo.squidb.data.AbstractModel;
import com.yahoo.squidb.data.SquidDatabase;
import com.yahoo.squidb.data.android.UriNotifier;
import com.yahoo.squidb.sample.HelloSquiDBApplication;
import com.yahoo.squidb.sample.TaskListActivity;
import com.yahoo.squidb.sample.database.TasksDatabase;
import com.yahoo.squidb.sample.models.Tag;
import com.yahoo.squidb.sample.models.Task;
import com.yahoo.squidb.sql.SqlTable;

import java.util.Set;

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
        // We want the database instance to be a singleton
    TasksDatabase provideTasksDatabase(@ForApplicaton Context context) {
        TasksDatabase database = new TasksDatabase(context);

        // Setting up a UriNotifier will sent ContentObserver notifications to Uris on table writes
        database.registerDataChangedNotifier(new UriNotifier(Task.TABLE, Tag.TABLE) {
            @Override
            protected boolean accumulateNotificationObjects(Set<Uri> uris, SqlTable<?> table, SquidDatabase database,
                    DBOperation operation, AbstractModel modelValues, long rowId) {
                return uris.add(Task.CONTENT_URI);
            }
        });
        return database;
    }

}
