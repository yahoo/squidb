/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.android;

import com.yahoo.squidb.data.ISQLiteDatabase;
import com.yahoo.squidb.data.ISQLiteOpenHelper;
import com.yahoo.squidb.reactive.ReactiveSquidDatabase;
import com.yahoo.squidb.sql.Index;
import com.yahoo.squidb.sql.Table;
import com.yahoo.squidb.sql.View;
import com.yahoo.squidb.test.BasicData;
import com.yahoo.squidb.test.Employee;
import com.yahoo.squidb.test.SQLiteBindingProvider;
import com.yahoo.squidb.test.TestModel;
import com.yahoo.squidb.test.TestViewModel;
import com.yahoo.squidb.test.TestVirtualModel;
import com.yahoo.squidb.test.Thing;
import com.yahoo.squidb.test.TriggerTester;

import org.sqlite.database.sqlite.SQLiteDatabase;

import javax.annotation.Nonnull;

public class TestReactiveDatabase extends ReactiveSquidDatabase {

    private static final Index INDEX_TESTMODELS_LUCKYNUMBER = TestModel.TABLE
            .index("index_testmodels_luckynumber", TestModel.LUCKY_NUMBER);

    public TestReactiveDatabase() {
        super();
    }

    @Override
    @Nonnull
    public String getName() {
        return "testDb";
    }

    @Override
    protected Table[] getTables() {
        return new Table[]{
                TestModel.TABLE,
                Thing.TABLE,
                Employee.TABLE,
                TriggerTester.TABLE,
                BasicData.TABLE,
                TestVirtualModel.TABLE
        };
    }

    @Override
    protected Index[] getIndexes() {
        return new Index[]{INDEX_TESTMODELS_LUCKYNUMBER};
    }

    @Override
    protected View[] getViews() {
        return new View[]{
                TestViewModel.VIEW
        };
    }

    @Override
    @Nonnull
    protected ISQLiteOpenHelper createOpenHelper(@Nonnull String databaseName, @Nonnull OpenHelperDelegate delegate,
            int version) {
        return SQLiteBindingProvider.getInstance().createOpenHelper(databaseName, delegate, version);
    }

    @Override
    protected int getVersion() {
        return 1;
    }

    @Override
    protected boolean onUpgrade(@Nonnull ISQLiteDatabase db, int oldVersion, int newVersion) {
        return true;
    }

    @Override
    protected void onConfigure(@Nonnull ISQLiteDatabase db) {
        // see AttachDetachTest#testAttacherInTransactionOnAnotherThread()
        Object wrappedObject = db.getWrappedObject();
        if (wrappedObject instanceof SQLiteDatabase) {
            ((SQLiteDatabase) wrappedObject).enableWriteAheadLogging();
        } else if (wrappedObject instanceof android.database.sqlite.SQLiteDatabase) {
            ((android.database.sqlite.SQLiteDatabase) wrappedObject).enableWriteAheadLogging();
        }
    }
}
