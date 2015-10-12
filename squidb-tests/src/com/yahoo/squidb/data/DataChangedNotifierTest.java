/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.data;

import android.net.Uri;
import android.text.format.DateUtils;

import com.yahoo.squidb.data.android.UriNotifier;
import com.yahoo.squidb.sql.Delete;
import com.yahoo.squidb.sql.Insert;
import com.yahoo.squidb.sql.SqlTable;
import com.yahoo.squidb.sql.Update;
import com.yahoo.squidb.test.DatabaseTestCase;
import com.yahoo.squidb.test.Employee;
import com.yahoo.squidb.test.TestModel;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class DataChangedNotifierTest extends DatabaseTestCase {

    private static class TestDataChangedNotifier extends DataChangedNotifier<TestDataChangedNotifier> {

        private boolean accumulateCalled = false;
        private boolean sendNotificationCalled = false;
        private Set<TestDataChangedNotifier> accumulatorSet = null;

        @Override
        protected boolean accumulateNotificationObjects(Set<TestDataChangedNotifier> accumulatorSet, SqlTable<?> table,
                SquidDatabase database, DBOperation operation, AbstractModel modelValues, long rowId) {
            accumulateCalled = true;
            this.accumulatorSet = accumulatorSet;
            return accumulatorSet.add(this);
        }

        @Override
        protected void sendNotification(SquidDatabase database, TestDataChangedNotifier notifyObject) {
            sendNotificationCalled = true;
            assertTrue(notifyObject == this);
        }

        private void reset() {
            accumulateCalled = sendNotificationCalled = false;
        }
    }

    public void testFlushAccumulationsClearsSet() {
        TestDataChangedNotifier notifier = new TestDataChangedNotifier();

        notifier.onDataChanged(null, database, DataChangedNotifier.DBOperation.INSERT, null, 0);
        assertFalse(notifier.accumulatorSet.isEmpty());
        notifier.flushAccumulatedNotifications(database, true);
        assertTrue(notifier.accumulateCalled);
        assertTrue(notifier.sendNotificationCalled);
        assertTrue(notifier.accumulatorSet.isEmpty());
        notifier.reset();

        notifier.onDataChanged(null, database, DataChangedNotifier.DBOperation.INSERT, null, 0);
        assertFalse(notifier.accumulatorSet.isEmpty());
        notifier.flushAccumulatedNotifications(database, false);
        assertTrue(notifier.accumulateCalled);
        assertFalse(notifier.sendNotificationCalled);
        assertTrue(notifier.accumulatorSet.isEmpty());
    }

    public void testRegisterAndUnregister() {
        TestDataChangedNotifier notifier = new TestDataChangedNotifier();
        database.registerDataChangedNotifier(notifier);

        TestModel t1 = insertBasicTestModel();
        assertTrue(notifier.accumulateCalled);
        assertTrue(notifier.sendNotificationCalled);

        notifier.reset();
        database.unregisterDataChangedNotifier(notifier);
        database.delete(TestModel.class, t1.getId());
        assertFalse(notifier.accumulateCalled);
        assertFalse(notifier.sendNotificationCalled);
    }

    public void testInsert() {
        final TestModel t1 = new TestModel().setFirstName("Sam").setLastName("Bosley")
                .setBirthday(System.currentTimeMillis() - 1);
        final TestModel t2 = new TestModel().setFirstName("Jon").setLastName("Koren")
                .setBirthday(System.currentTimeMillis());
        Runnable toRun = new Runnable() {
            @Override
            public void run() {
                database.createNew(t1);
            }
        };
        testForParameters(toRun, TestModel.TABLE, DataChangedNotifier.DBOperation.INSERT, t1, 1L);

        toRun = new Runnable() {
            @Override
            public void run() {
                database.createNew(t2);
            }
        };
        testForParameters(toRun, TestModel.TABLE, DataChangedNotifier.DBOperation.INSERT, t2, 2L);

        toRun = new Runnable() {
            public void run() {
                database.insert(Insert.into(TestModel.TABLE).columns(TestModel.FIRST_NAME, TestModel.LAST_NAME)
                        .values("Some", "Guy"));
            }
        };

        testForParameters(toRun, TestModel.TABLE, DataChangedNotifier.DBOperation.INSERT, null, 3);
    }

    public void testUpdate() {
        final TestModel t1 = insertBasicTestModel();
        Runnable toRun = new Runnable() {
            @Override
            public void run() {
                t1.setLastName("Boss");
                database.persist(t1);
            }
        };
        testForParameters(toRun, TestModel.TABLE, DataChangedNotifier.DBOperation.UPDATE, t1, t1.getId());

        insertBasicTestModel("Sam", "Bosley", System.currentTimeMillis());
        final TestModel template = new TestModel().setFirstName("The");
        toRun = new Runnable() {
            @Override
            public void run() {
                database.update(TestModel.LAST_NAME.like("Bos%"), template);
            }
        };
        testForParameters(toRun, TestModel.TABLE, DataChangedNotifier.DBOperation.UPDATE, template, 0);

        toRun = new Runnable() {
            @Override
            public void run() {
                database.update(Update.table(TestModel.TABLE).fromTemplate(new TestModel().setFirstName("Guy"))
                        .where(TestModel.LAST_NAME.like("Bos%")));
            }
        };
        testForParameters(toRun, TestModel.TABLE, DataChangedNotifier.DBOperation.UPDATE, null, 0);
    }

    public void testDelete() {
        final TestModel t1 = insertBasicTestModel();
        Runnable toRun = new Runnable() {
            @Override
            public void run() {
                database.delete(TestModel.class, t1.getId());
            }
        };
        testForParameters(toRun, TestModel.TABLE, DataChangedNotifier.DBOperation.DELETE, null, t1.getId());

        insertBasicTestModel("Sam", "Bosley", System.currentTimeMillis());
        toRun = new Runnable() {
            @Override
            public void run() {
                database.deleteWhere(TestModel.class, TestModel.LAST_NAME.like("Bos%"));
            }
        };
        testForParameters(toRun, TestModel.TABLE, DataChangedNotifier.DBOperation.DELETE, null, 0);

        insertBasicTestModel("Sam", "Bosley", System.currentTimeMillis() + DateUtils.DAY_IN_MILLIS);
        toRun = new Runnable() {
            @Override
            public void run() {
                database.delete(Delete.from(TestModel.TABLE).where(TestModel.LAST_NAME.like("Bos%")));
            }
        };
        testForParameters(toRun, TestModel.TABLE, DataChangedNotifier.DBOperation.DELETE, null, 0);
    }

    private void testForParameters(Runnable execute, final SqlTable<?> expectedTable,
            final DataChangedNotifier.DBOperation expectedOp,
            final AbstractModel expectedModel, final long expectedRowId) {
        TestDataChangedNotifier notifier = new TestDataChangedNotifier() {
            @Override
            protected boolean accumulateNotificationObjects(Set<TestDataChangedNotifier> accumulatorSet,
                    SqlTable<?> table,
                    SquidDatabase database, DataChangedNotifier.DBOperation operation, AbstractModel modelValues,
                    long rowId) {
                assertEquals(expectedTable, table);
                assertEquals(expectedOp, operation);
                assertEquals(expectedModel, modelValues);
                assertEquals(expectedRowId, rowId);
                return super.accumulateNotificationObjects(accumulatorSet, table, database, operation,
                        modelValues, rowId);
            }
        };
        database.registerDataChangedNotifier(notifier);
        execute.run();
        assertTrue(notifier.accumulateCalled);
        assertTrue(notifier.sendNotificationCalled);
        database.unregisterDataChangedNotifier(notifier);
    }

    public void testMultipleNotifiersCanBeRegistered() {
        TestDataChangedNotifier n1 = new TestDataChangedNotifier();
        TestDataChangedNotifier n2 = new TestDataChangedNotifier();

        database.registerDataChangedNotifier(n1);
        database.registerDataChangedNotifier(n2);

        insertBasicTestModel();
        assertTrue(n1.accumulateCalled);
        assertTrue(n2.accumulateCalled);
        assertTrue(n1.sendNotificationCalled);
        assertTrue(n2.sendNotificationCalled);
    }

    public void testGlobalNotifiersNotifiedForAllTables() {
        final Set<SqlTable<?>> calledForTables = new HashSet<SqlTable<?>>();
        TestDataChangedNotifier globalNotifier = new TestDataChangedNotifier() {
            @Override
            protected boolean accumulateNotificationObjects(Set<TestDataChangedNotifier> accumulatorSet,
                    SqlTable<?> table,
                    SquidDatabase database, DBOperation operation, AbstractModel modelValues, long rowId) {
                calledForTables.add(table);
                return super
                        .accumulateNotificationObjects(accumulatorSet, table, database, operation, modelValues, rowId);
            }
        };

        database.registerDataChangedNotifier(globalNotifier);

        insertBasicTestModel();
        database.persist(new Employee().setName("Elmo"));

        assertTrue(calledForTables.contains(TestModel.TABLE));
        assertTrue(calledForTables.contains(Employee.TABLE));
    }


    public void testEnableAndDisableNotifications() {
        TestDataChangedNotifier notifier = new TestDataChangedNotifier();
        database.registerDataChangedNotifier(notifier);

        database.beginTransaction();
        try {
            database.setDataChangedNotificationsEnabled(false);

            insertBasicTestModel("Tech Sergeant", "Chen", System.currentTimeMillis() - 1);
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
            assertFalse(notifier.accumulateCalled);
            assertFalse(notifier.sendNotificationCalled);
            database.setDataChangedNotificationsEnabled(true);
            assertFalse(notifier.accumulateCalled);
            assertFalse(notifier.sendNotificationCalled);
        }

        insertBasicTestModel();
        assertTrue(notifier.accumulateCalled);
        assertTrue(notifier.sendNotificationCalled);
    }

    public void testSimpleDataChangedNotifier() {
        final AtomicInteger onDataChangedCalledCount = new AtomicInteger(0);
        SimpleDataChangedNotifier notifier = new SimpleDataChangedNotifier() {
            @Override
            protected void onDataChanged() {
                onDataChangedCalledCount.incrementAndGet();
            }
        };

        database.registerDataChangedNotifier(notifier);
        database.beginTransaction();
        try {
            insertBasicTestModel("Peter", "Quincy Taggart", System.currentTimeMillis() - 5);
            insertBasicTestModel("Guy", "Fleegman", System.currentTimeMillis() - 4);
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
        assertEquals(1, onDataChangedCalledCount.get());
    }

    public void testNotifierConstructors() {
        testNotifierConstructorsInternal(new UriNotifier(getContext()) {
            @Override
            protected boolean accumulateNotificationObjects(Set<Uri> accumulatorSet, SqlTable<?> table,
                    SquidDatabase database, DBOperation operation, AbstractModel modelValues, long rowId) {
                return false;
            }
        });
        testNotifierConstructorsInternal(new SimpleDataChangedNotifier() {
            @Override
            protected void onDataChanged() {

            }
        });

        testNotifierConstructorsInternal(new UriNotifier(getContext(), TestModel.TABLE) {
            @Override
            protected boolean accumulateNotificationObjects(Set<Uri> accumulatorSet, SqlTable<?> table,
                    SquidDatabase database, DBOperation operation, AbstractModel modelValues, long rowId) {
                return false;
            }
        }, TestModel.TABLE);
        testNotifierConstructorsInternal(new SimpleDataChangedNotifier(TestModel.TABLE) {
            @Override
            protected void onDataChanged() {

            }
        }, TestModel.TABLE);

        testNotifierConstructorsInternal(new UriNotifier(getContext(), Arrays.asList(TestModel.TABLE, Employee.TABLE)) {
            @Override
            protected boolean accumulateNotificationObjects(Set<Uri> accumulatorSet, SqlTable<?> table,
                    SquidDatabase database, DBOperation operation, AbstractModel modelValues, long rowId) {
                return false;
            }
        }, TestModel.TABLE, Employee.TABLE);
        testNotifierConstructorsInternal(new SimpleDataChangedNotifier(Arrays.asList(TestModel.TABLE, Employee.TABLE)) {
            @Override
            protected void onDataChanged() {

            }
        }, TestModel.TABLE, Employee.TABLE);
    }

    private void testNotifierConstructorsInternal(DataChangedNotifier<?> notifier, SqlTable<?>... tables) {
        Set<SqlTable<?>> whichTables = notifier.whichTables();
        assertEquals(tables.length, whichTables.size());
        assertTrue(whichTables.containsAll(Arrays.asList(tables)));
    }

}
