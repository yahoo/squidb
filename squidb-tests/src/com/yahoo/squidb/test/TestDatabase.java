/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.test;

import com.yahoo.squidb.data.ISQLiteDatabase;
import com.yahoo.squidb.data.ISQLiteOpenHelper;
import com.yahoo.squidb.data.SquidDatabase;
import com.yahoo.squidb.sql.Index;
import com.yahoo.squidb.sql.Table;
import com.yahoo.squidb.sql.View;

public class TestDatabase extends SquidDatabase {

    public boolean caughtCustomMigrationException;

    private static final Index INDEX_TESTMODELS_LUCKYNUMBER = TestModel.TABLE
            .index("index_testmodels_luckynumber", TestModel.LUCKY_NUMBER);

    public TestDatabase() {
        super();
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
    protected ISQLiteOpenHelper createOpenHelper(String databaseName, OpenHelperDelegate delegate, int version) {
        return SQLiteBindingProvider.provideOpenHelper(databaseName, delegate, version);
    }

    @Override
    protected int getVersion() {
        return 1;
    }

    @Override
    protected void onTablesCreated(ISQLiteDatabase db) {
        super.onTablesCreated(db);
    }

    @Override
    protected boolean onUpgrade(ISQLiteDatabase db, int oldVersion, int newVersion) {
        return true;
    }

    @Override
    protected void onOpen(ISQLiteDatabase db) {
        super.onOpen(db);
    }

    @Override
    protected void onConfigure(ISQLiteDatabase db) {
        /** @see AttachDetachTest#testAttacherInTransactionOnAnotherThread() */
        db.enableWriteAheadLogging();
    }

    @Override
    protected boolean onDowngrade(ISQLiteDatabase db, int oldVersion, int newVersion) {
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
