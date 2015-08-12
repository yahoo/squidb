/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.data;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteException;

import com.yahoo.squidb.data.adapter.SQLiteDatabaseWrapper;
import com.yahoo.squidb.sql.Criterion;
import com.yahoo.squidb.sql.Property;
import com.yahoo.squidb.sql.Property.StringProperty;
import com.yahoo.squidb.sql.Query;
import com.yahoo.squidb.sql.TableStatement;
import com.yahoo.squidb.test.DatabaseTestCase;
import com.yahoo.squidb.test.Employee;
import com.yahoo.squidb.test.TestDatabase;
import com.yahoo.squidb.test.TestModel;
import com.yahoo.squidb.test.TestViewModel;

import java.util.Arrays;
import java.util.List;

public class SquidDatabaseTest extends DatabaseTestCase {

    private BadDatabase badDatabase;

    @Override
    protected void setupDatabase() {
        super.setupDatabase();
        badDatabase = new BadDatabase(getContext());
        badDatabase.getDatabase(); // init
    }

    @Override
    protected void tearDownDatabase() {
        super.tearDownDatabase();
        badDatabase.clear();
    }

    public void testRawQuery() {
        badDatabase.persist(new TestModel().setFirstName("Sam").setLastName("Bosley").setBirthday(testDate));
        Cursor cursor = null;
        try {
            // Sanity check that there is only one row in the table
            assertEquals(1, badDatabase.count(TestModel.class, Criterion.all));

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
        SQLiteDatabaseWrapper db = badDatabase.getDatabase();
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
     * {@link SquidDatabase} does not automatically recreate the database when a migration fails. This is really to
     * test that {@link SquidDatabase#recreate()} can safely be called during onUpgrade or
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
        badDatabase.persist(new Employee().setName("Alice"));
        badDatabase.persist(new Employee().setName("Bob"));
        badDatabase.persist(new Employee().setName("Cindy"));
        assertEquals(3, badDatabase.count(Employee.class, Criterion.all));

        testMigrationFailureCalled(upgrade, false, true);

        // verify the db was recreated with the appropriate version and no previous data
        SQLiteDatabaseWrapper db = badDatabase.getDatabase();
        assertEquals(badDatabase.getVersion(), db.getVersion());
        assertEquals(0, badDatabase.count(Employee.class, Criterion.all));
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

    public void testCustomMigrationException() {
        TestDatabase database = new TestDatabase(getContext());
        SQLiteDatabaseWrapper db = database.getDatabase();
        // force a downgrade
        final int version = db.getVersion();
        final int previousVersion = version + 1;
        db.setVersion(previousVersion);
        database.close();
        database.getDatabase();

        try {
            assertTrue(database.caughtCustomMigrationException);
        } finally {
            database.clear(); // clean up since this is the only test using it
        }
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
        public String getName() {
            return "badDb";
        }

        @Override
        protected int getVersion() {
            return super.getVersion() + 1;
        }

        @Override
        protected void onTablesCreated(SQLiteDatabaseWrapper db) {
            onTablesCreatedCalled = true;
        }

        @Override
        protected final boolean onUpgrade(SQLiteDatabaseWrapper db, int oldVersion, int newVersion) {
            onUpgradeCalled = true;
            if (shouldThrowDuringMigration) {
                throw new SQLiteException("My name is \"NO! NO! BAD DATABASE!\". What's yours?");
            } else if (shouldRecreate) {
                recreate();
            }
            return false;
        }

        @Override
        protected boolean onDowngrade(SQLiteDatabaseWrapper db, int oldVersion, int newVersion) {
            onDowngradeCalled = true;
            if (shouldThrowDuringMigration) {
                throw new SQLiteException("My name is \"NO! NO! BAD DATABASE!\". What's yours?");
            } else if (shouldRecreate) {
                recreate();
            }
            return false;
        }

        @Override
        protected void onMigrationFailed(MigrationFailedException failure) {
            onMigrationFailedCalled = true;
            migrationFailedOldVersion = failure.oldVersion;
            migrationFailedNewVersion = failure.newVersion;
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

    public void testBasicInsertAndFetch() {
        TestModel model = insertBasicTestModel();

        TestModel fetch = database.fetch(TestModel.class, model.getId(), TestModel.PROPERTIES);
        assertEquals("Sam", fetch.getFirstName());
        assertEquals("Bosley", fetch.getLastName());
        assertEquals(testDate, fetch.getBirthday().longValue());
    }

    public void testPropertiesAreNullable() {
        TestModel model = insertBasicTestModel();
        model.setFirstName(null);
        model.setLastName(null);

        assertNull(model.getFirstName());
        assertNull(model.getLastName());
        database.persist(model);

        TestModel fetch = database.fetch(TestModel.class, model.getId(), TestModel.PROPERTIES);
        assertNull(fetch.getFirstName());
        assertNull(fetch.getLastName());
    }

    public void testBooleanProperties() {
        TestModel model = insertBasicTestModel();
        assertTrue(model.isHappy());

        model.setIsHappy(false);
        assertFalse(model.isHappy());
        database.persist(model);
        TestModel fetch = database.fetch(TestModel.class, model.getId(), TestModel.PROPERTIES);
        assertFalse(fetch.isHappy());
    }

    public void testQueriesWithBooleanPropertiesWork() {
        insertBasicTestModel();

        SquidCursor<TestModel> result = database
                .query(TestModel.class, Query.select(TestModel.PROPERTIES).where(TestModel.IS_HAPPY.isTrue()));
        assertEquals(1, result.getCount());
        result.moveToFirst();
        TestModel model = new TestModel(result);
        assertTrue(model.isHappy());

        model.setIsHappy(false);
        database.persist(model);
        result.close();

        result = database
                .query(TestModel.class, Query.select(TestModel.PROPERTIES).where(TestModel.IS_HAPPY.isFalse()));
        assertEquals(1, result.getCount());
        result.moveToFirst();
        model = new TestModel(result);
        assertFalse(model.isHappy());
    }

    public void testConflict() {
        insertBasicTestModel();

        TestModel conflict = new TestModel();
        conflict.setFirstName("Dave");
        conflict.setLastName("Bosley");

        boolean result = database.persistWithOnConflict(conflict, TableStatement.ConflictAlgorithm.IGNORE);
        assertFalse(result);
        TestModel shouldntExist = database.fetchByCriterion(TestModel.class,
                TestModel.FIRST_NAME.eq("Dave").and(TestModel.LAST_NAME.eq("Bosley")),
                TestModel.PROPERTIES);
        assertNull(shouldntExist);
        SQLiteConstraintException expected = null;
        try {
            conflict.clearValue(TestModel.ID);
            database.persistWithOnConflict(conflict, TableStatement.ConflictAlgorithm.FAIL);
        } catch (SQLiteConstraintException e) {
            expected = e;
        }
        assertNotNull(expected);
    }

    public void testListProperty() {
        TestModel model = insertBasicTestModel();
        List<String> numbers = Arrays.asList("0", "1", "2", "3");
        model.setSomeList(numbers);

        database.persist(model);

        model = database.fetch(TestModel.class, model.getId(), TestModel.PROPERTIES);
        List<String> readNumbers = model.getSomeList();
        assertEquals(numbers.size(), readNumbers.size());
        for (int i = 0; i < numbers.size(); i++) {
            assertEquals(numbers.get(i), readNumbers.get(i));
        }
    }

    public void testFetchByQueryResetsLimitAndTable() {
        TestModel model1 = new TestModel().setFirstName("Sam1").setLastName("Bosley1");
        TestModel model2 = new TestModel().setFirstName("Sam2").setLastName("Bosley2");
        TestModel model3 = new TestModel().setFirstName("Sam3").setLastName("Bosley3");
        database.persist(model1);
        database.persist(model2);
        database.persist(model3);

        Query query = Query.select().limit(2, 1);
        TestModel fetched = database.fetchByQuery(TestModel.class, query);
        assertEquals(model2.getId(), fetched.getId());
        assertEquals(2, query.getLimit());
        assertEquals(1, query.getOffset());
        assertEquals(null, query.getTable());
    }

    public void testEqCaseInsensitive() {
        insertBasicTestModel();

        TestModel fetch = database.fetchByCriterion(TestModel.class, TestModel.LAST_NAME.eqCaseInsensitive("BOSLEY"),
                TestModel.PROPERTIES);
        assertNotNull(fetch);
    }

    public void testInsertRow() {
        TestModel model = insertBasicTestModel();
        assertNotNull(database.fetch(TestModel.class, model.getId()));

        database.delete(TestModel.class, model.getId());
        assertNull(database.fetch(TestModel.class, model.getId()));

        long modelId = model.getId();
        database.insertRow(model); // Should reinsert the row with the same id

        assertEquals(modelId, model.getId());
        assertNotNull(database.fetch(TestModel.class, model.getId()));
        assertEquals(1, database.count(TestModel.class, Criterion.all));
    }
}
