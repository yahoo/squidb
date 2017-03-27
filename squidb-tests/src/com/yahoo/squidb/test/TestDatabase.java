/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.test;

import com.yahoo.squidb.data.ISQLiteDatabase;
import com.yahoo.squidb.data.ISQLiteOpenHelper;
import com.yahoo.squidb.data.SquidDatabase;
import com.yahoo.squidb.sql.CompileContext;
import com.yahoo.squidb.sql.DefaultArgumentResolver;
import com.yahoo.squidb.sql.Index;
import com.yahoo.squidb.sql.Table;
import com.yahoo.squidb.sql.View;

import javax.annotation.Nonnull;

public class TestDatabase extends SquidDatabase {

    public boolean caughtCustomMigrationException;
    public boolean useCustomArgumentBinder;

    private static final Index INDEX_TESTMODELS_LUCKYNUMBER = TestModel.TABLE
            .index("index_testmodels_luckynumber", TestModel.LUCKY_NUMBER);

    public TestDatabase() {
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
                TestVirtualModel.TABLE,
                TestMultiColumnKey.TABLE,
                TestNonIntegerPrimaryKey.TABLE,
                TestSingleColumnUpsertable.TABLE,
                TestMultiColumnUpsertable.TABLE,
                TestConstraint.TABLE
        };
    }

    @Override
    protected Index[] getIndexes() {
        return new Index[]{
                INDEX_TESTMODELS_LUCKYNUMBER
        };
    }

    @Override
    protected View[] getViews() {
        return new View[]{
                TestViewModel.VIEW
        };
    }

    @Override
    @Nonnull
    protected ISQLiteOpenHelper createOpenHelper(@Nonnull String databaseName,
            @Nonnull OpenHelperDelegate delegate, int version) {
        return SQLiteBindingProvider.getInstance().createOpenHelper(databaseName, delegate, version);
    }

    @Override
    protected int getVersion() {
        return 1;
    }

    @Override
    protected void onTablesCreated(@Nonnull ISQLiteDatabase db) {
        super.onTablesCreated(db);
    }

    @Override
    protected boolean onUpgrade(@Nonnull ISQLiteDatabase db, int oldVersion, int newVersion) {
        return true;
    }

    @Override
    protected void onOpen(@Nonnull ISQLiteDatabase db) {
        super.onOpen(db);
    }

    @Override
    protected void onConfigure(@Nonnull ISQLiteDatabase db) {
        // see AttachDetachTest#testAttacherInTransactionOnAnotherThread()
        db.enableWriteAheadLogging();
        setPreparedInsertCacheEnabled(true);
    }

    @Override
    protected boolean onDowngrade(@Nonnull ISQLiteDatabase db, int oldVersion, int newVersion) {
        throw new CustomMigrationException(getName(), oldVersion, newVersion);
    }

    @Override
    protected void onMigrationFailed(@Nonnull MigrationFailedException failure) {
        if (failure instanceof CustomMigrationException) {
            // suppress
            caughtCustomMigrationException = true;
        }
    }

    @Override
    protected void buildCompileContext(@Nonnull CompileContext.Builder builder) {
        if (useCustomArgumentBinder) {
            builder.setArgumentResolver(new DefaultArgumentResolver() {
                @Override
                protected boolean canResolveCustomType(Object arg) {
                    return arg instanceof Enum<?>;
                }

                @Override
                protected Object resolveCustomType(Object arg) {
                    return ((Enum<?>) arg).ordinal();
                }
            });
        }
    }

    private static class CustomMigrationException extends MigrationFailedException {

        public CustomMigrationException(String dbName, int oldVersion, int newVersion) {
            super(dbName, oldVersion, newVersion);
        }
    }

}
