/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.data;

import com.yahoo.squidb.sql.Field;
import com.yahoo.squidb.sql.Property.StringProperty;
import com.yahoo.squidb.sql.Query;
import com.yahoo.squidb.sql.TableModelName;
import com.yahoo.squidb.sql.TableStatement;
import com.yahoo.squidb.test.DatabaseTestCase;
import com.yahoo.squidb.test.Employee;
import com.yahoo.squidb.test.SQLiteBindingProvider;
import com.yahoo.squidb.test.TestDatabase;
import com.yahoo.squidb.test.TestModel;
import com.yahoo.squidb.test.TestViewModel;
import com.yahoo.squidb.test.Thing;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class SquidDatabaseTest extends DatabaseTestCase {

    private TestHookDatabase testHookDatabase;

    @Override
    protected void setupDatabase() {
        super.setupDatabase();
        testHookDatabase = new TestHookDatabase();
        testHookDatabase.getDatabase(); // init
    }

    @Override
    protected void tearDownDatabase() {
        super.tearDownDatabase();
        testHookDatabase.clear();
    }

    public void testRawQuery() {
        testHookDatabase.persist(new TestModel().setFirstName("Sam").setLastName("Bosley").setBirthday(testDate));
        ICursor cursor = null;
        try {
            // Sanity check that there is only one row in the table
            assertEquals(1, testHookDatabase.countAll(TestModel.class));

            // Test that raw query binds arguments correctly--if the argument
            // is bound as a String, the result will be empty
            cursor = testHookDatabase.rawQuery("select * from testModels where abs(_id) = ?", new Object[]{1});
            assertEquals(1, cursor.getCount());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public void testTryAddColumn() {
        StringProperty goodProperty = new StringProperty(
                new TableModelName(TestModel.class, TestModel.TABLE.getName()), "good_column");
        testHookDatabase.tryAddColumn(goodProperty); // don't care about the result, just that it didn't throw

        final StringProperty badProperty = new StringProperty(
                new TableModelName(TestViewModel.class, TestViewModel.VIEW.getName()), "bad_column");
        testThrowsException(new Runnable() {
            @Override
            public void run() {
                testHookDatabase.tryAddColumn(badProperty);
            }
        }, IllegalArgumentException.class);
    }

    public void testMigrationFailureCalledWhenOnUpgradeReturnsFalse() {
        testMigrationFailureCalled(true, false, false, false);
    }

    public void testMigrationFailureCalledWhenOnUpgradeThrowsException() {
        testMigrationFailureCalled(true, true, false, false);
    }

    public void testMigrationFailureCalledWhenOnDowngradeReturnsFalse() {
        testMigrationFailureCalled(false, false, false, false);
    }

    public void testMigrationFailureCalledWhenOnDowngradeThrowsException() {
        testMigrationFailureCalled(false, true, false, false);
    }

    private void testMigrationFailureCalled(boolean upgrade, boolean shouldThrow,
            boolean shouldRecreateDuringMigration, boolean shouldRecreateOnMigrationFailed) {
        testHookDatabase.shouldThrowDuringMigration = shouldThrow;
        testHookDatabase.shouldRecreateInMigration = shouldRecreateDuringMigration;
        testHookDatabase.shouldRecreateInOnMigrationFailed = shouldRecreateOnMigrationFailed;

        // set version manually
        ISQLiteDatabase db = testHookDatabase.getDatabase();
        final int version = db.getVersion();
        final int previousVersion = upgrade ? version - 1 : version + 1;
        db.setVersion(previousVersion);
        // close and reopen to trigger an upgrade/downgrade
        testHookDatabase.onTablesCreatedCalled = false;
        testHookDatabase.close();
        RuntimeException caughtException = null;
        try {
            testHookDatabase.getDatabase();
        } catch (RuntimeException e) {
            caughtException = e;
        }
        // If throwing or returning false from migration but not handling it, we expect getDatabase to also throw
        // since the DB will not be open after exiting the onMigrationFailed hook
        if (!shouldRecreateDuringMigration) {
            assertNotNull(caughtException);
        } else {
            assertNull(caughtException);
        }

        assertTrue(upgrade ? testHookDatabase.onUpgradeCalled : testHookDatabase.onDowngradeCalled);
        if (shouldRecreateDuringMigration || shouldRecreateOnMigrationFailed) {
            assertTrue(testHookDatabase.onTablesCreatedCalled);
        } else {
            assertTrue(testHookDatabase.onMigrationFailedCalled);
            assertEquals(previousVersion, testHookDatabase.migrationFailedOldVersion);
            assertEquals(version, testHookDatabase.migrationFailedNewVersion);
        }
    }

    /**
     * {@link SquidDatabase} does not automatically recreate the database when a migration fails. This is really to
     * test that {@link SquidDatabase#recreate()} can safely be called during onUpgrade or
     * onDowngrade as an exemplar for client developers.
     */
    public void testRecreateOnUpgradeFailure() {
        testRecreateDuringMigrationOrFailureCallback(true, false);
    }

    public void testRecreateOnDowngradeFailure() {
        testRecreateDuringMigrationOrFailureCallback(false, false);
    }

    public void testRecreateDuringOnMigrationFailed() {
        testRecreateDuringMigrationOrFailureCallback(true, true);
    }

    private void testRecreateDuringMigrationOrFailureCallback(boolean upgrade, boolean recreateDuringMigration) {
        // insert some data to check for later
        testHookDatabase.persist(new Employee().setName("Alice"));
        testHookDatabase.persist(new Employee().setName("Bob"));
        testHookDatabase.persist(new Employee().setName("Cindy"));
        assertEquals(3, testHookDatabase.countAll(Employee.class));

        testMigrationFailureCalled(upgrade, recreateDuringMigration, true, recreateDuringMigration);

        // verify the db was recreated with the appropriate version and no previous data
        ISQLiteDatabase db = testHookDatabase.getDatabase();
        assertEquals(testHookDatabase.getVersion(), db.getVersion());
        assertEquals(0, testHookDatabase.countAll(Employee.class));
    }

    public void testExceptionDuringOpenCleansUp() {
        testHookDatabase.shouldThrowDuringMigration = true;
        testHookDatabase.shouldRecreateInMigration = true;
        testHookDatabase.shouldRecreateInOnMigrationFailed = false;
        testHookDatabase.shouldRethrowInOnMigrationFailed = true;

        testThrowsException(new Runnable() {
            @Override
            public void run() {
                // set version manually
                ISQLiteDatabase db = testHookDatabase.getDatabase();
                final int version = db.getVersion();
                final int previousVersion = version - 1;
                db.setVersion(previousVersion);
                // close and reopen to trigger an upgrade/downgrade
                testHookDatabase.onTablesCreatedCalled = false;
                testHookDatabase.close();
                testHookDatabase.getDatabase();
            }
        }, SquidDatabase.MigrationFailedException.class);

        assertFalse(testHookDatabase.inTransaction());
        assertFalse(testHookDatabase.isOpen());
    }

    public void testAcquireExclusiveLockFailsWhenInTransaction() {
        testThrowsException(new Runnable() {
            @Override
            public void run() {
                IllegalStateException caughtException = null;
                testHookDatabase.beginTransaction();
                try {
                    testHookDatabase.acquireExclusiveLock();
                } catch (IllegalStateException e) {
                    // Need to do this in the catch block rather than the finally block, because otherwise tearDown is
                    // called before we have a chance to release the transaction lock
                    testHookDatabase.endTransaction();
                    caughtException = e;
                } finally {
                    if (caughtException == null) { // Sanity cleanup if catch block was never reached
                        testHookDatabase.endTransaction();
                    }
                }
                if (caughtException != null) {
                    throw caughtException;
                }
            }
        }, IllegalStateException.class);
    }

    public void testCustomMigrationException() {
        final TestDatabase database = new TestDatabase();
        ISQLiteDatabase db = database.getDatabase();
        // force a downgrade
        final int version = db.getVersion();
        final int previousVersion = version + 1;
        db.setVersion(previousVersion);
        database.close();
        // Expect an exception here because this DB does not cleanly re-open the DB in case of errors
        testThrowsException(new Runnable() {
            @Override
            public void run() {
                database.getDatabase();
            }
        }, RuntimeException.class);

        try {
            assertTrue(database.caughtCustomMigrationException);
        } finally {
            database.clear(); // clean up since this is the only test using it
        }
    }

    public void testRetryOpenDatabase() {
        testHookDatabase.clear(); // init opens it, we need a new db
        final AtomicBoolean openFailedHandlerCalled = new AtomicBoolean();
        testHookDatabase.shouldThrowDuringOpen = true;
        testHookDatabase.dbOpenFailedHandler = new DbOpenFailedHandler() {
            @Override
            public void dbOpenFailed(RuntimeException failure, int openFailureCount) {
                openFailedHandlerCalled.set(true);
                testHookDatabase.shouldThrowDuringOpen = false;
                testHookDatabase.getDatabase();
            }
        };
        testHookDatabase.getDatabase();
        assertTrue(testHookDatabase.isOpen());
        assertTrue(openFailedHandlerCalled.get());
        assertEquals(1, testHookDatabase.dbOpenFailureCount);
    }

    public void testRecreateOnOpenFailed() {
        final AtomicBoolean openFailedHandlerCalled = new AtomicBoolean();
        testHookDatabase.persist(new Employee().setName("Alice"));
        testHookDatabase.persist(new Employee().setName("Bob"));
        testHookDatabase.persist(new Employee().setName("Cindy"));
        assertEquals(3, testHookDatabase.countAll(Employee.class));

        ISQLiteDatabase db = testHookDatabase.getDatabase();
        db.setVersion(db.getVersion() + 1);
        testHookDatabase.close();
        testHookDatabase.shouldThrowDuringMigration = true;
        testHookDatabase.shouldRethrowInOnMigrationFailed = true;
        testHookDatabase.dbOpenFailedHandler = new DbOpenFailedHandler() {
            @Override
            public void dbOpenFailed(RuntimeException failure, int openFailureCount) {
                openFailedHandlerCalled.set(true);
                testHookDatabase.shouldThrowDuringOpen = false;
                testHookDatabase.recreate();
            }
        };
        testHookDatabase.getDatabase();
        assertTrue(testHookDatabase.isOpen());
        assertTrue(openFailedHandlerCalled.get());
        assertEquals(1, testHookDatabase.dbOpenFailureCount);
        assertEquals(0, testHookDatabase.countAll(Employee.class));
    }

    public void testSuppressingDbOpenFailedThrowsRuntimeException() {
        testHookDatabase.clear();
        final AtomicBoolean openFailedHandlerCalled = new AtomicBoolean();
        testHookDatabase.shouldThrowDuringOpen = true;
        testHookDatabase.dbOpenFailedHandler = new DbOpenFailedHandler() {
            @Override
            public void dbOpenFailed(RuntimeException failure, int openFailureCount) {
                openFailedHandlerCalled.set(true);
            }
        };
        testThrowsException(new Runnable() {
            @Override
            public void run() {
                testHookDatabase.getDatabase();
            }
        }, RuntimeException.class);
        assertTrue(openFailedHandlerCalled.get());
        assertFalse(testHookDatabase.isOpen());
    }

    public void testRecursiveRetryDbOpen() {
        testRecursiveRetryDbOpen(1, true);
        testRecursiveRetryDbOpen(1, false);
        testRecursiveRetryDbOpen(2, true);
        testRecursiveRetryDbOpen(2, false);
        testRecursiveRetryDbOpen(3, true);
        testRecursiveRetryDbOpen(3, false);
    }

    private void testRecursiveRetryDbOpen(final int maxRetries, final boolean eventualRecovery) {
        testHookDatabase.clear();
        testHookDatabase.shouldThrowDuringOpen = true;
        testHookDatabase.dbOpenFailedHandler = new DbOpenFailedHandler() {
            @Override
            public void dbOpenFailed(RuntimeException failure, int openFailureCount) {
                if (openFailureCount >= maxRetries) {
                    testHookDatabase.shouldThrowDuringOpen = false;
                    if (!eventualRecovery) {
                        throw failure;
                    }
                }
                testHookDatabase.getDatabase();
            }
        };

        if (eventualRecovery) {
            testHookDatabase.getDatabase();
        } else {
            testThrowsException(new Runnable() {
                @Override
                public void run() {
                    testHookDatabase.getDatabase();
                }
            }, RuntimeException.class);
        }
        assertEquals(maxRetries, testHookDatabase.dbOpenFailureCount);
        assertEquals(eventualRecovery, testHookDatabase.isOpen());
    }

    public void testDataChangeNotifiersDisabledDuringDbOpen() {
        testDataChangeNotifiersDisabledDuringDbOpen(true, true);
        testDataChangeNotifiersDisabledDuringDbOpen(true, false);
        testDataChangeNotifiersDisabledDuringDbOpen(false, true);
        testDataChangeNotifiersDisabledDuringDbOpen(false, false);
    }

    private void testDataChangeNotifiersDisabledDuringDbOpen(boolean startEnabled, final boolean openFailure) {
        final AtomicBoolean notifierCalled = new AtomicBoolean();
        final AtomicBoolean failOnOpen = new AtomicBoolean(openFailure);
        SimpleDataChangedNotifier simpleNotifier = new SimpleDataChangedNotifier(Thing.TABLE) {
            @Override
            protected void onDataChanged() {
                notifierCalled.set(true);
            }
        };
        TestDatabase testDb = new TestDatabase() {
            @Override
            public String getName() {
                return super.getName() + "2";
            }

            @Override
            protected void onTablesCreated(ISQLiteDatabase db) {
                super.onTablesCreated(db);
                persist(new Thing().setFoo("foo").setBar(1));
                if (failOnOpen.getAndSet(false)) {
                    throw new RuntimeException("Failed to open db");
                }
            }

            @Override
            protected void onDatabaseOpenFailed(RuntimeException failure, int openFailureCount) {
                getDatabase();
            }
        };
        testDb.setDataChangedNotificationsEnabled(startEnabled);
        testDb.registerDataChangedNotifier(simpleNotifier);
        testDb.getDatabase();
        assertFalse(notifierCalled.get());
        assertEquals(1, testDb.countAll(Thing.class));
        assertEquals(startEnabled, testDb.areDataChangedNotificationsEnabled());
        testDb.persist(new Thing().setFoo("foo2").setBar(2));
        assertEquals(startEnabled, notifierCalled.get());
        testDb.clear();
    }

    public void testOnCloseHook() {
        final AtomicReference<ISQLitePreparedStatement> preparedStatementRef = new AtomicReference<>();
        testHookDatabase.onCloseTester = new DbHookTester() {
            @Override
            void onHookImpl(ISQLiteDatabase db) {
                assertTrue(db.isOpen());
                ISQLitePreparedStatement statement = preparedStatementRef.get();
                assertNotNull(statement);
                statement.close();
                preparedStatementRef.set(null);
            }
        };

        preparedStatementRef.set(testHookDatabase.prepareStatement("SELECT COUNT(*) FROM testModels"));
        assertNotNull(preparedStatementRef.get());
        assertEquals(0, preparedStatementRef.get().simpleQueryForLong());
        testHookDatabase.close();
        assertTrue(testHookDatabase.onCloseTester.wasCalled);
        assertNull(preparedStatementRef.get());
    }

    /**
     * A {@link TestDatabase} that intentionally fails in onUpgrade and onDowngrade and provides means of testing
     * various other SquidDatabase hooks
     */
    private static class TestHookDatabase extends TestDatabase {

        private boolean onMigrationFailedCalled = false;
        private boolean onUpgradeCalled = false;
        private boolean onDowngradeCalled = false;
        private boolean onTablesCreatedCalled = false;
        private int dbOpenFailureCount = 0;
        private int migrationFailedOldVersion = 0;
        private int migrationFailedNewVersion = 0;

        private boolean shouldThrowDuringOpen = false;
        private boolean shouldThrowDuringMigration = false;
        private boolean shouldRecreateInMigration = false;
        private boolean shouldRecreateInOnMigrationFailed = false;
        private boolean shouldRethrowInOnMigrationFailed = false;
        private DbOpenFailedHandler dbOpenFailedHandler = null;

        private DbHookTester onCloseTester = null;

        public TestHookDatabase() {
            super();
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
        protected void onTablesCreated(ISQLiteDatabase db) {
            onTablesCreatedCalled = true;
            if (shouldThrowDuringOpen) {
                throw new RuntimeException("Simulating DB open failure");
            }
        }

        @Override
        protected final boolean onUpgrade(ISQLiteDatabase db, int oldVersion, int newVersion) {
            onUpgradeCalled = true;
            if (shouldThrowDuringMigration) {
                throw new RuntimeException("My name is \"NO! NO! BAD DATABASE!\". What's yours?");
            } else if (shouldRecreateInMigration) {
                recreate();
            }
            return false;
        }

        @Override
        protected boolean onDowngrade(ISQLiteDatabase db, int oldVersion, int newVersion) {
            onDowngradeCalled = true;
            if (shouldThrowDuringMigration) {
                throw new RuntimeException("My name is \"NO! NO! BAD DATABASE!\". What's yours?");
            } else if (shouldRecreateInMigration) {
                recreate();
            }
            return false;
        }

        @Override
        protected void onMigrationFailed(MigrationFailedException failure) {
            onMigrationFailedCalled = true;
            migrationFailedOldVersion = failure.oldVersion;
            migrationFailedNewVersion = failure.newVersion;
            if (shouldRecreateInOnMigrationFailed) {
                recreate();
            } else if (shouldRethrowInOnMigrationFailed) {
                throw failure;
            }
        }

        @Override
        protected void onDatabaseOpenFailed(RuntimeException failure, int openFailureCount) {
            dbOpenFailureCount = openFailureCount;
            if (dbOpenFailedHandler != null) {
                dbOpenFailedHandler.dbOpenFailed(failure, openFailureCount);
            } else {
                super.onDatabaseOpenFailed(failure, openFailureCount);
            }
        }

        @Override
        protected void onClose(ISQLiteDatabase db) {
            if (onCloseTester != null) {
                onCloseTester.onHook(db);
            }
        }
    }

    private interface DbOpenFailedHandler {

        void dbOpenFailed(RuntimeException failure, int openFailureCount);
    }

    // This class can be used to add customized behavior to the optional SquidDatabase overrideable hook methods
    // in the TestHookDatabase class.
    private static abstract class DbHookTester {

        boolean wasCalled = false;

        final void onHook(ISQLiteDatabase db) {
            wasCalled = true;
            onHookImpl(db);
        }

        abstract void onHookImpl(ISQLiteDatabase db);
    }

    public void testBasicInsertAndFetch() {
        TestModel model = insertBasicTestModel();

        TestModel fetch = database.fetch(TestModel.class, model.getRowId(), TestModel.PROPERTIES);
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

        TestModel fetch = database.fetch(TestModel.class, model.getRowId(), TestModel.PROPERTIES);
        assertNull(fetch.getFirstName());
        assertNull(fetch.getLastName());
    }

    public void testBooleanProperties() {
        TestModel model = insertBasicTestModel();
        assertTrue(model.isHappy());

        model.setIsHappy(false);
        assertFalse(model.isHappy());
        database.persist(model);
        TestModel fetch = database.fetch(TestModel.class, model.getRowId(), TestModel.PROPERTIES);
        assertFalse(fetch.isHappy());
    }

    public void testQueriesWithBooleanPropertiesWork() {
        insertBasicTestModel();

        TestModel model;
        SquidCursor<TestModel> result = database.query(TestModel.class,
                Query.select(TestModel.PROPERTIES).where(TestModel.IS_HAPPY.isTrue()));
        try {
            assertEquals(1, result.getCount());
            result.moveToFirst();
            model = new TestModel(result);
            assertTrue(model.isHappy());

            model.setIsHappy(false);
            database.persist(model);
        } finally {
            result.close();
        }

        result = database.query(TestModel.class,
                Query.select(TestModel.PROPERTIES).where(TestModel.IS_HAPPY.isFalse()));

        try {
            assertEquals(1, result.getCount());
            result.moveToFirst();
            model = new TestModel(result);
            assertFalse(model.isHappy());
        } finally {
            result.close();
        }
    }

    public void testConflict() {
        insertBasicTestModel();

        TestModel conflict = new TestModel();
        conflict.setFirstName("Dave");
        conflict.setLastName("Bosley");

        boolean result = database.persistWithOnConflict(conflict, TableStatement.ConflictAlgorithm.IGNORE);
        assertFalse(result);
        TestModel shouldntExist = database.fetchByCriterion(TestModel.class,
                TestModel.FIRST_NAME.eq("Dave").and(TestModel.LAST_NAME.eq("Bosley")), TestModel.PROPERTIES);
        assertNull(shouldntExist);
        RuntimeException expected = null;
        try {
            conflict.clearValue(TestModel.ID);
            database.persistWithOnConflict(conflict, TableStatement.ConflictAlgorithm.FAIL);
        } catch (RuntimeException e) {
            expected = e;
        }
        assertNotNull(expected);
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
        assertEquals(model2.getRowId(), fetched.getRowId());
        assertEquals(Field.field("2"), query.getLimit());
        assertEquals(Field.field("1"), query.getOffset());
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
        assertNotNull(database.fetch(TestModel.class, model.getRowId()));

        database.delete(TestModel.class, model.getRowId());
        assertNull(database.fetch(TestModel.class, model.getRowId()));

        long modelId = model.getRowId();
        database.insertRow(model); // Should reinsert the row with the same id

        assertEquals(modelId, model.getRowId());
        assertNotNull(database.fetch(TestModel.class, model.getRowId()));
        assertEquals(1, database.countAll(TestModel.class));
    }

    public void testDropView() {
        database.tryDropView(TestViewModel.VIEW);
        testThrowsRuntimeException(new Runnable() {
            @Override
            public void run() {
                database.query(TestViewModel.class, Query.select().from(TestViewModel.VIEW));
            }
        });
    }

    public void testDropTable() {
        database.tryDropTable(TestModel.TABLE);
        testThrowsRuntimeException(new Runnable() {
            @Override
            public void run() {
                database.query(TestModel.class, Query.select().from(TestModel.TABLE));
            }
        });
    }

    public void testTryExecSqlReturnsFalseForInvalidSql() {
        assertFalse(database.tryExecSql("CREATE TABLE"));
    }

    public void testExecSqlOrThrowThrowsForInvalidSql() {
        testThrowsRuntimeException(new Runnable() {
            @Override
            public void run() {
                database.execSqlOrThrow("CREATE TABLE");
            }
        });
        testThrowsRuntimeException(new Runnable() {
            @Override
            public void run() {
                database.execSqlOrThrow("CREATE TABLE", null);
            }
        });
    }

    public void testUpdateAll() {
        database.persist(new TestModel().setFirstName("A").setLastName("A")
                .setBirthday(System.currentTimeMillis() - 1).setLuckyNumber(1));
        database.persist(new TestModel().setFirstName("A").setLastName("B")
                .setBirthday(System.currentTimeMillis() - 1).setLuckyNumber(2));

        database.updateAll(new TestModel().setLuckyNumber(5));

        assertEquals(0, database.count(TestModel.class, TestModel.LUCKY_NUMBER.neq(5)));
    }

    public void testDeleteAll() {
        insertBasicTestModel("A", "B", System.currentTimeMillis() - 1);
        insertBasicTestModel("C", "D", System.currentTimeMillis());

        assertEquals(2, database.countAll(TestModel.class));
        database.deleteAll(TestModel.class);
        assertEquals(0, database.countAll(TestModel.class));
    }

    public void testBlobs() {
        List<byte[]> randomBlobs = new ArrayList<>();
        Random r = new Random();

        randomBlobs.add(new byte[0]); // Test 0-length blob
        int numBlobs = 10;
        for (int i = 0; i < numBlobs; i++) {
            byte[] blob = new byte[i + 1];
            r.nextBytes(blob);
            randomBlobs.add(blob);
        }

        Thing t = new Thing();
        database.beginTransaction();
        try {
            for (byte[] blob : randomBlobs) {
                t.setBlob(blob);
                database.createNew(t);
            }
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }

        SquidCursor<Thing> cursor = database.query(Thing.class, Query.select(Thing.BLOB).orderBy(Thing.ID.asc()));
        try {
            assertEquals(randomBlobs.size(), cursor.getCount());
            for (int i = 0; i < randomBlobs.size(); i++) {
                cursor.moveToPosition(i);
                byte[] blob = cursor.get(Thing.BLOB);
                assertTrue(Arrays.equals(randomBlobs.get(i), blob));
            }
        } finally {
            cursor.close();
        }
    }

    public void testConcurrentReadsWithWAL() {
        // Tests that concurrent reads can happen when using WAL, but only committed changes will be visible
        // on other threads
        insertBasicTestModel();
        final Semaphore sema1 = new Semaphore(0);
        final Semaphore sema2 = new Semaphore(0);
        final AtomicInteger countBeforeCommit = new AtomicInteger();
        final AtomicInteger countAfterCommit = new AtomicInteger();
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    sema2.acquire();
                } catch (InterruptedException e) {
                    fail(e.getMessage());
                }
                countBeforeCommit.set(database.countAll(TestModel.class));
                sema1.release();
                try {
                    sema2.acquire();
                } catch (InterruptedException e) {
                    fail(e.getMessage());
                }
                countAfterCommit.set(database.countAll(TestModel.class));
            }
        });
        database.beginTransactionNonExclusive();
        try {
            thread.start();
            insertBasicTestModel("A", "B", System.currentTimeMillis() + 100);
            sema2.release();
            try {
                sema1.acquire();
            } catch (InterruptedException e) {
                fail(e.getMessage());
            }
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
            sema2.release();
        }
        try {
            thread.join();
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
        assertEquals(1, countBeforeCommit.get());
        assertEquals(2, countAfterCommit.get());
    }

    public void testConcurrencyStressTest() {
        int numThreads = 20;
        final AtomicReference<Exception> exception = new AtomicReference<>();
        List<Thread> workers = new ArrayList<>();
        for (int i = 0; i < numThreads; i++) {
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    concurrencyStressTest(exception);
                }
            });
            t.start();
            workers.add(t);
        }
        for (Thread t : workers) {
            try {
                t.join();
            } catch (Exception e) {
                exception.set(e);
            }
        }
        assertNull(exception.get());
    }

    private void concurrencyStressTest(AtomicReference<Exception> exception) {
        try {
            Random r = new Random();
            int numOperations = 100;
            Thing t = new Thing();
            for (int i = 0; i < numOperations; i++) {
                int rand = r.nextInt(10);
                if (rand == 0) {
                    database.close();
                } else if (rand == 1) {
                    database.clear();
                } else if (rand == 2) {
                    database.recreate();
                } else if (rand == 3) {
                    database.beginTransactionNonExclusive();
                    try {
                        for (int j = 0; j < 20; j++) {
                            t.setFoo(Integer.toString(j))
                                    .setBar(-j);
                            database.createNew(t);
                        }
                        database.setTransactionSuccessful();
                    } finally {
                        database.endTransaction();
                    }
                } else {
                    t.setFoo(Integer.toString(i))
                            .setBar(-i);
                    database.createNew(t);
                }
            }
        } catch (Exception e) {
            exception.set(e);
        }
    }

    public void testSimpleQueries() {
        database.beginTransactionNonExclusive();
        try {
            // the changes() function only works inside a transaction, because otherwise you may not get
            // the same connection to the sqlite database depending on what the connection pool feels like giving you.
            String sql = "SELECT CHANGES()";
            insertBasicTestModel();

            assertEquals(1, database.simpleQueryForLong(sql, null));
            assertEquals("1", database.simpleQueryForString(sql, null));
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
    }

    public void testCopyDatabase() {
        insertBasicTestModel(); // Make sure DB is open and populated

        File destinationDir = new File(SQLiteBindingProvider.getInstance().getWriteableTestDir());

        File dbFile = new File(database.getDatabasePath());
        File walFile = new File(database.getDatabasePath() + "-wal");
        assertTrue(dbFile.exists());
        assertTrue(walFile.exists());

        File destinationDbFile = new File(destinationDir.getPath() + File.separator + database.getName());
        File destinationWalFile = new File(destinationDir.getPath() + File.separator + database.getName() + "-wal");
        destinationDbFile.delete();
        destinationWalFile.delete();
        assertFalse(destinationDbFile.exists());
        assertFalse(destinationWalFile.exists());

        assertTrue(database.copyDatabase(destinationDir));
        assertTrue(destinationDbFile.exists());
        assertTrue(destinationWalFile.exists());

        assertTrue(filesAreEqual(dbFile, destinationDbFile));
        assertTrue(filesAreEqual(walFile, destinationWalFile));
    }

    private boolean filesAreEqual(File f1, File f2) {
        if (f1.length() != f2.length()) {
            return false;
        }

        try {
            FileInputStream f1in = new FileInputStream(f1);
            FileInputStream f2in = new FileInputStream(f2);

            int f1Bytes;
            int f2Bytes;
            byte[] f1Buffer;
            byte[] f2Buffer;
            int BUFFER_SIZE = 1024;
            f1Buffer = new byte[BUFFER_SIZE];
            f2Buffer = new byte[BUFFER_SIZE];
            while ((f1Bytes = f1in.read(f1Buffer)) != -1) {
                f2Bytes = f2in.read(f2Buffer);
                if (f1Bytes != f2Bytes) {
                    return false;
                }
                if (!Arrays.equals(f1Buffer, f2Buffer)) {
                    return false;
                }
            }
            return f2in.read(f2Buffer) == -1;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

//    public void testValidIdentifierCharacters() {
//        database.getDatabase();
//        // Characters between 0xd800 and 0xdfff don't work as standalone names
//        testIdentifiersInRange((char) 0x00A1, (char) 0xd7ff);
//        testIdentifiersInRange((char) 0xe000, (char) 0xffff);
//        testIdentifiersInRange('a', 'z');
//        testIdentifiersInRange('A', 'Z');
//        testIdentifiersInRange('_', '_');
//    }
//
//    private void testIdentifiersInRange(char min, char max) {
//        for (char c = min; c >= min && c <= max; c++) {
//            String tableName = Character.toString(c);
//            if (!database.tryExecSql("create table " + tableName + " (" + tableName + " text)")) {
//                fail("Failed to create table named " + tableName + ", char was " + Integer.toHexString(c));
//            }
//            if (!database.tryExecSql("insert into " + tableName + " (" + tableName + ") values ('a')")) {
//                fail("Failed to insert into table named " + tableName + ", char was " + Integer.toHexString(c));
//            }
//            if (database.simpleQueryForLong("select count(*) from " + tableName, null) != 1) {
//                fail("Thought insert into table named " + tableName + " had worked but count wasn't 1" +
//                        ", char was " + Integer.toHexString(c));
//            }
//            if (!database.tryExecSql("drop table " + tableName)) {
//                fail("Failed to drop table named " + tableName + ", char was " + Integer.toHexString(c));
//            }
//            Logger.d(Logger.LOG_TAG, "Identifier character " + c + " is valid");
//        }
//    }
}
