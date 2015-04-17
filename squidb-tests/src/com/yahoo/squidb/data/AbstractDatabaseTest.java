/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.data;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

import com.yahoo.squidb.sql.Criterion;
import com.yahoo.squidb.sql.Property;
import com.yahoo.squidb.sql.Property.StringProperty;
import com.yahoo.squidb.test.DatabaseTestCase;
import com.yahoo.squidb.test.Employee;
import com.yahoo.squidb.test.TestDatabase;
import com.yahoo.squidb.test.TestModel;
import com.yahoo.squidb.test.TestViewModel;

public class AbstractDatabaseTest extends DatabaseTestCase {

    private BadDatabase badDatabase;

    @Override
    protected void setupDatabase() {
        // don't call super, we'll set up our own database instead
        badDatabase = new BadDatabase(getContext());
        badDatabase.getDatabase(); // init
        dao = new DatabaseDao(badDatabase);
    }

    @Override
    protected void tearDownDatabase() {
        // don't call super, we'll clean it up ourselves
        badDatabase.clear();
    }

    public void testRawQuery() {
        insertBasicTestModel();
        Cursor cursor = null;
        try {
            // Sanity check that there is only one row in the table
            assertEquals(1, dao.count(TestModel.class, Criterion.all));

            // Test that raw query binds arguments correctly--if the argument
            // is bound as a String, the result will be empty
            cursor = badDatabase.rawQuery("select * from testModels where abs(_id) = ?", new Object[]{1});
            assertEquals(1, cursor.getCount());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public void testTryAddColumn() {
        StringProperty goodProperty = new StringProperty(TestModel.TABLE, "good_column");
        badDatabase.tryAddColumn(goodProperty); // don't care about the result, just that it didn't throw

        final StringProperty badProperty = new StringProperty(TestViewModel.VIEW, "bad_column");
        testThrowsException(new Runnable() {
            @Override
            public void run() {
                badDatabase.tryAddColumn(badProperty);
            }
        }, IllegalArgumentException.class);
    }

    public void testMigrationFailureCalledWhenOnUpgradeReturnsFalse() {
        testMigrationFailureCalled(true, false, false);
    }

    public void testMigrationFailureCalledWhenOnUpgradeThrowsException() {
        testMigrationFailureCalled(true, true, false);
    }

    public void testMigrationFailureCalledWhenOnDowngradeReturnsFalse() {
        testMigrationFailureCalled(false, false, false);
    }

    public void testMigrationFailureCalledWhenOnDowngradeThrowsException() {
        testMigrationFailureCalled(false, true, false);
    }

    private void testMigrationFailureCalled(boolean upgrade, boolean shouldThrow, boolean shouldRecreate) {
        badDatabase.setShouldThrowDuringMigration(shouldThrow);
        badDatabase.setShouldRecreate(shouldRecreate);

        // set version manually
        SQLiteDatabase db = badDatabase.getDatabase();
        final int version = db.getVersion();
        final int previousVersion = upgrade ? version - 1 : version + 1;
        db.setVersion(previousVersion);
        // close and reopen to trigger an upgrade/downgrade
        badDatabase.onTablesCreatedCalled = false;
        badDatabase.close();
        badDatabase.getDatabase();

        assertTrue(upgrade ? badDatabase.onUpgradeCalled : badDatabase.onDowngradeCalled);
        if (shouldRecreate) {
            assertTrue(badDatabase.onTablesCreatedCalled);
        } else {
            assertTrue(badDatabase.onMigrationFailedCalled);
            assertEquals(previousVersion, badDatabase.migrationFailedOldVersion);
            assertEquals(version, badDatabase.migrationFailedNewVersion);
        }
    }

    /**
     * {@link AbstractDatabase} does not automatically recreate the database when a migration fails. This is really to
     * test that {@link com.yahoo.squidb.data.AbstractDatabase#recreate()} can safely be called during onUpgrade or
     * onDowngrade as an exemplar for client developers.
     */
    public void testRecreateOnUpgradeFailure() {
        testRecreateDuringMigration(true);
    }

    public void testRecreateOnDowngradeFailure() {
        testRecreateDuringMigration(false);
    }

    private void testRecreateDuringMigration(boolean upgrade) {
        // insert some data to check for later
        dao.persist(new Employee().setName("Alice"));
        dao.persist(new Employee().setName("Bob"));
        dao.persist(new Employee().setName("Cindy"));
        assertEquals(3, dao.count(Employee.class, Criterion.all));

        testMigrationFailureCalled(upgrade, false, true);

        // verify the db was recreated with the appropriate version and no previous data
        SQLiteDatabase db = badDatabase.getDatabase();
        assertEquals(badDatabase.getVersion(), db.getVersion());
        assertEquals(0, dao.count(Employee.class, Criterion.all));
    }

    public void testAcquireExclusiveLockFailsWhenInTransaction() {
        testThrowsException(new Runnable() {
            @Override
            public void run() {
                badDatabase.beginTransaction();
                try {
                    badDatabase.acquireExclusiveLock();
                } finally {
                    badDatabase.endTransaction();
                }
            }
        }, IllegalStateException.class);
    }

    /**
     * {@link TestDatabase} that intentionally fails in onUpgrade and onDowngrade
     */
    private static class BadDatabase extends TestDatabase {

        private boolean onMigrationFailedCalled = false;
        private boolean onUpgradeCalled = false;
        private boolean onDowngradeCalled = false;
        private boolean onTablesCreatedCalled = false;
        private int migrationFailedOldVersion = 0;
        private int migrationFailedNewVersion = 0;

        private boolean shouldThrowDuringMigration = false;
        private boolean shouldRecreate = false;

        public BadDatabase(Context context) {
            super(context);
        }

        @Override
        protected String getName() {
            return "badDb";
        }

        @Override
        protected int getVersion() {
            return super.getVersion() + 1;
        }

        @Override
        protected void onTablesCreated(SQLiteDatabase db) {
            onTablesCreatedCalled = true;
        }

        @Override
        protected final boolean onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            onUpgradeCalled = true;
            if (shouldThrowDuringMigration) {
                throw new SQLiteException("My name is \"NO! NO! BAD DATABASE!\". What's yours?");
            } else if (shouldRecreate) {
                recreate();
            }
            return false;
        }

        @Override
        protected boolean onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            onDowngradeCalled = true;
            if (shouldThrowDuringMigration) {
                throw new SQLiteException("My name is \"NO! NO! BAD DATABASE!\". What's yours?");
            } else if (shouldRecreate) {
                recreate();
            }
            return false;
        }

        @Override
        protected void onMigrationFailed(int oldVersion, int newVersion) {
            onMigrationFailedCalled = true;
            migrationFailedOldVersion = oldVersion;
            migrationFailedNewVersion = newVersion;
        }

        public void setShouldThrowDuringMigration(boolean flag) {
            shouldThrowDuringMigration = flag;
        }

        public void setShouldRecreate(boolean flag) {
            shouldRecreate = flag;
        }

        @Override
        protected boolean tryAddColumn(Property<?> property) {
            return super.tryAddColumn(property);
        }
    }
}
