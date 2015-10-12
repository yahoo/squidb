/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.test;

import android.content.Context;

import com.yahoo.squidb.data.SquidDatabase;
import com.yahoo.squidb.data.adapter.SQLiteDatabaseWrapper;
import com.yahoo.squidb.data.SQLiteOpenHelperWrapper;
import com.yahoo.squidb.sql.AttachDetachTest;
import com.yahoo.squidb.sql.Index;
import com.yahoo.squidb.sql.Table;
import com.yahoo.squidb.sql.View;

public class TestDatabase extends SquidDatabase {

    public boolean caughtCustomMigrationException;

    private static final Index INDEX_TESTMODELS_LUCKYNUMBER = TestModel.TABLE
            .index("index_testmodels_luckynumber", TestModel.LUCKY_NUMBER);

    public TestDatabase(Context context) {
        super(context);
    }

    @Override
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
    protected SQLiteOpenHelperWrapper getOpenHelper(Context context, String databaseName,
            OpenHelperDelegate delegate, int version) {
        return SquidTestRunner.selectedBinding.getOpenHelper(context, databaseName, delegate, version);
    }

    @Override
    protected int getVersion() {
        return 1;
    }

    @Override
    protected void onTablesCreated(SQLiteDatabaseWrapper db) {
        super.onTablesCreated(db);
    }

    @Override
    protected boolean onUpgrade(SQLiteDatabaseWrapper db, int oldVersion, int newVersion) {
        return true;
    }

    @Override
    protected void onOpen(SQLiteDatabaseWrapper db) {
        super.onOpen(db);
    }

    @Override
    protected void onConfigure(SQLiteDatabaseWrapper db) {
        /** @see AttachDetachTest#testAttacherInTransactionOnAnotherThread() */
        db.enableWriteAheadLogging();
    }

    @Override
    protected boolean onDowngrade(SQLiteDatabaseWrapper db, int oldVersion, int newVersion) {
        throw new CustomMigrationException(getName(), oldVersion, newVersion);
    }

    @Override
    protected void onMigrationFailed(MigrationFailedException failure) {
        if (failure instanceof CustomMigrationException) {
            // suppress
            caughtCustomMigrationException = true;
        }
    }

    private static class CustomMigrationException extends MigrationFailedException {

        public CustomMigrationException(String dbName, int oldVersion, int newVersion) {
            super(dbName, oldVersion, newVersion);
        }
    }

}
