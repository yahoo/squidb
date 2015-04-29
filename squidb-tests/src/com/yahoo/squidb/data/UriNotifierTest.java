package com.yahoo.squidb.data;

import android.database.ContentObserver;
import android.net.Uri;
import android.text.format.DateUtils;

import com.yahoo.squidb.data.UriNotifier.DBOperation;
import com.yahoo.squidb.sql.Delete;
import com.yahoo.squidb.sql.Insert;
import com.yahoo.squidb.sql.SqlTable;
import com.yahoo.squidb.sql.Update;
import com.yahoo.squidb.test.DatabaseTestCase;
import com.yahoo.squidb.test.Employee;
import com.yahoo.squidb.test.TestModel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class UriNotifierTest extends DatabaseTestCase {

    private static class TestUriNotifier extends UriNotifier {

        @SuppressWarnings("unchecked")
        public TestUriNotifier() {
            super(TestModel.TABLE);
        }

        @Override
        public void addUrisToNotify(Set<Uri> uris, SqlTable<?> table, String databaseName, DBOperation operation,
                AbstractModel modelValues, long rowId) {
            uris.add(TestModel.CONTENT_URI);
        }
    }

    private List<ContentObserver> observers = new ArrayList<ContentObserver>();

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        dao.unregisterAllUriNotifiers();
        for (ContentObserver observer : observers) {
            getContext().getContentResolver().unregisterContentObserver(observer);
        }
    }

    private AtomicBoolean listenTo(Uri uri, boolean notifyForDescendants) {
        final AtomicBoolean gotUriNotification = new AtomicBoolean(false);
        ContentObserver observer = new ContentObserver(null) {
            @Override
            public void onChange(boolean selfChange) {
                gotUriNotification.set(true);
                super.onChange(selfChange);
            }
        };
        observers.add(observer);
        getContext().getContentResolver().registerContentObserver(uri, notifyForDescendants, observer);
        return gotUriNotification;
    }

    public void testRegisterAndUnregister() {
        final AtomicBoolean wasCalled = new AtomicBoolean(false);

        UriNotifier notifier = new TestUriNotifier() {
            @Override
            public void addUrisToNotify(Set<Uri> uris, SqlTable<?> table, String databaseName, DBOperation operation,
                    AbstractModel modelValues, long rowId) {
                wasCalled.set(true);
                super.addUrisToNotify(uris, table, databaseName, operation, modelValues, rowId);
            }

        };
        dao.registerUriNotifier(notifier);

        TestModel t1 = insertBasicTestModel();
        assertTrue(wasCalled.get());

        wasCalled.set(false);
        dao.unregisterUriNotifier(notifier);
        dao.delete(TestModel.class, t1.getId());
        assertFalse(wasCalled.get());
    }

    public void testNotificationOccurs() {
        AtomicBoolean notified = listenTo(TestModel.CONTENT_URI, false);
        waitForResolver();

        dao.registerUriNotifier(new TestUriNotifier());
        insertBasicTestModel();
        waitForResolver();
        assertTrue(notified.get());
    }

    public void testInsert() {
        final TestModel t1 = new TestModel().setFirstName("Sam").setLastName("Bosley")
                .setBirthday(System.currentTimeMillis() - 1);
        final TestModel t2 = new TestModel().setFirstName("Jon").setLastName("Koren")
                .setBirthday(System.currentTimeMillis());
        Runnable toRun = new Runnable() {
            @Override
            public void run() {
                dao.createNew(t1);
            }
        };
        testForParameters(toRun, TestModel.TABLE, DBOperation.INSERT, t1, 1L);

        toRun = new Runnable() {
            @Override
            public void run() {
                dao.createNew(t2);
            }
        };
        testForParameters(toRun, TestModel.TABLE, DBOperation.INSERT, t2, 2L);

        toRun = new Runnable() {
            public void run() {
                dao.insert(Insert.into(TestModel.TABLE).columns(TestModel.FIRST_NAME, TestModel.LAST_NAME)
                        .values("Some", "Guy"));
            }
        };

        testForParameters(toRun, TestModel.TABLE, DBOperation.INSERT, null, 3);
    }

    public void testUpdate() {
        final TestModel t1 = insertBasicTestModel();
        Runnable toRun = new Runnable() {
            @Override
            public void run() {
                t1.setLastName("Boss");
                dao.persist(t1);
            }
        };
        testForParameters(toRun, TestModel.TABLE, DBOperation.UPDATE, t1, t1.getId());

        insertBasicTestModel("Sam", "Bosley", System.currentTimeMillis());
        final TestModel template = new TestModel().setFirstName("The");
        toRun = new Runnable() {
            @Override
            public void run() {
                dao.update(TestModel.LAST_NAME.like("Bos%"), template);
            }
        };
        testForParameters(toRun, TestModel.TABLE, DBOperation.UPDATE, template, 0);

        toRun = new Runnable() {
            @Override
            public void run() {
                dao.update(Update.table(TestModel.TABLE).fromTemplate(new TestModel().setFirstName("Guy"))
                        .where(TestModel.LAST_NAME.like("Bos%")));
            }
        };
        testForParameters(toRun, TestModel.TABLE, DBOperation.UPDATE, null, 0);
    }

    public void testDelete() {
        final TestModel t1 = insertBasicTestModel();
        Runnable toRun = new Runnable() {
            @Override
            public void run() {
                dao.delete(TestModel.class, t1.getId());
            }
        };
        testForParameters(toRun, TestModel.TABLE, DBOperation.DELETE, null, t1.getId());

        insertBasicTestModel("Sam", "Bosley", System.currentTimeMillis());
        toRun = new Runnable() {
            @Override
            public void run() {
                dao.deleteWhere(TestModel.class, TestModel.LAST_NAME.like("Bos%"));
            }
        };
        testForParameters(toRun, TestModel.TABLE, DBOperation.DELETE, null, 0);

        insertBasicTestModel("Sam", "Bosley", System.currentTimeMillis() + DateUtils.DAY_IN_MILLIS);
        toRun = new Runnable() {
            @Override
            public void run() {
                dao.delete(Delete.from(TestModel.TABLE).where(TestModel.LAST_NAME.like("Bos%")));
            }
        };
        testForParameters(toRun, TestModel.TABLE, DBOperation.DELETE, null, 0);
    }

    private void testForParameters(Runnable execute, final SqlTable<?> expectedTable, final DBOperation expectedOp,
            final AbstractModel expectedModel, final long expectedRowId) {
        UriNotifier notifier = new TestUriNotifier() {
            @Override
            public void addUrisToNotify(Set<Uri> uris, SqlTable<?> table, String databaseName, DBOperation operation,
                    AbstractModel modelValues, long rowId) {
                assertEquals(expectedTable, table);
                assertEquals(expectedOp, operation);
                assertEquals(expectedModel, modelValues);
                assertEquals(expectedRowId, rowId);
                super.addUrisToNotify(uris, table, databaseName, operation, modelValues, rowId);
            }
        };
        dao.registerUriNotifier(notifier);
        execute.run();
        dao.unregisterUriNotifier(notifier);
    }

    public void testMultipleNotifiersCanBeRegistered() {
        final AtomicBoolean n1Called = new AtomicBoolean(false);
        final AtomicBoolean n2Called = new AtomicBoolean(false);
        UriNotifier n1 = new TestUriNotifier() {
            @Override
            public void addUrisToNotify(Set<Uri> uris, SqlTable<?> table, String databaseName, DBOperation operation,
                    AbstractModel modelValues, long rowId) {
                super.addUrisToNotify(uris, table, databaseName, operation, modelValues, rowId);
                n1Called.set(true);
            }
        };
        UriNotifier n2 = new TestUriNotifier() {
            @Override
            public void addUrisToNotify(Set<Uri> uris, SqlTable<?> table, String databaseName, DBOperation operation,
                    AbstractModel modelValues, long rowId) {
                super.addUrisToNotify(uris, table, databaseName, operation, modelValues, rowId);
                n2Called.set(true);
            }
        };

        dao.registerUriNotifier(n1);
        dao.registerUriNotifier(n2);

        insertBasicTestModel();
        waitForResolver();
        assertTrue(n1Called.get());
        assertTrue(n2Called.get());
    }

    public void testGlobalNotifiersNotifiedForAllTables() {
        final Set<SqlTable<?>> calledForTables = new HashSet<SqlTable<?>>();
        UriNotifier globalNotifier = new UriNotifier() {
            @Override
            public void addUrisToNotify(Set<Uri> uris, SqlTable<?> table, String databaseName,
                    DBOperation operation, AbstractModel modelValues, long rowId) {
                calledForTables.add(table);
                uris.add(Uri.parse("content://com.yahoo.squidb/"));
            }
        };

        dao.registerUriNotifier(globalNotifier);

        insertBasicTestModel();
        dao.persist(new Employee().setName("Elmo"));

        waitForResolver();
        assertTrue(calledForTables.contains(TestModel.TABLE));
        assertTrue(calledForTables.contains(Employee.TABLE));
    }

    public void testEnableAndDisableNotifications() {
        UriNotifier notifier = new TestUriNotifier();
        dao.registerUriNotifier(notifier);
        AtomicBoolean notifiedUri = listenTo(TestModel.CONTENT_URI, false);
        waitForResolver();

        dao.beginTransaction();
        try {
            dao.disableUriNotifications();

            insertBasicTestModel("Tech Sergeant", "Chen", System.currentTimeMillis() - 1);
            dao.setTransactionSuccessful();
        } finally {
            dao.endTransaction();
            assertFalse(notifiedUri.get());
            dao.enableUriNotifications();
            assertFalse(notifiedUri.get());
        }

        insertBasicTestModel();
        waitForResolver();
        assertTrue(notifiedUri.get());
    }

    public void testUrisNotifiedAtEndOfSuccessfulTransaction() {
        testNotificationsDuringTransactions(true, false, true);
    }

    public void testUrisNotNotifiedAtEndOfFailedTransaction() {
        testNotificationsDuringTransactions(false, false, true);
    }

    public void testNestedTransactionsNotifyAtEnd() {
        testNotificationsDuringTransactions(true, true, true);
    }

    public void testFailedNestedTransactionsDontNotify() {
        testNotificationsDuringTransactions(true, true, false);
    }

    private void testNotificationsDuringTransactions(boolean setSuccessful, boolean addNestedTransaction,
            boolean nestedSuccessful) {
        UriNotifier idNotifier = new TestUriNotifier() {
            @Override
            public void addUrisToNotify(Set<Uri> uris, SqlTable<?> table, String databaseName,
                    DBOperation operation, AbstractModel modelValues, long rowId) {
                Uri uri = TestModel.CONTENT_URI;
                if (rowId > 0) {
                    uri = uri.buildUpon().appendPath(Long.toString(rowId)).build();
                }
                uris.add(uri);
            }
        };

        dao.registerUriNotifier(idNotifier);

        AtomicBoolean uri1 = listenTo(Uri.withAppendedPath(TestModel.CONTENT_URI, "1"), false);
        AtomicBoolean uri2 = listenTo(Uri.withAppendedPath(TestModel.CONTENT_URI, "2"), false);
        AtomicBoolean uri3 = listenTo(Uri.withAppendedPath(TestModel.CONTENT_URI, "3"), false);
        AtomicBoolean uri4 = listenTo(Uri.withAppendedPath(TestModel.CONTENT_URI, "4"), false);
        waitForResolver();

        dao.beginTransaction();
        try {
            insertBasicTestModel("Peter", "Quincy Taggart", System.currentTimeMillis() - 5);
            insertBasicTestModel("Guy", "Fleegman", System.currentTimeMillis() - 4);
            assertFalse(uri1.get());
            assertFalse(uri2.get());

            if (addNestedTransaction) {
                dao.beginTransaction();
                try {
                    insertBasicTestModel("Young", "Laredo", System.currentTimeMillis() - 3);
                    insertBasicTestModel("Doctor", "Lazarus", System.currentTimeMillis() - 2);

                    if (nestedSuccessful) {
                        dao.setTransactionSuccessful();
                    }
                } finally {
                    dao.endTransaction();
                    assertFalse(uri1.get());
                    assertFalse(uri2.get());
                    assertFalse(uri3.get());
                    assertFalse(uri4.get());
                }
            }

            if (setSuccessful) {
                dao.setTransactionSuccessful();
            }
        } finally {
            dao.endTransaction();
        }
        waitForResolver();
        assertEquals(setSuccessful && nestedSuccessful, uri1.get());
        assertEquals(setSuccessful && nestedSuccessful, uri2.get());

        assertEquals(addNestedTransaction && nestedSuccessful, uri3.get());
        assertEquals(addNestedTransaction && nestedSuccessful, uri4.get());
    }

    public void testUriNotifyParentAndDescendant() {
        AtomicBoolean notifyCalled = listenTo(Uri.withAppendedPath(TestModel.CONTENT_URI, "1"), true);
        waitForResolver();

        getContext().getContentResolver().notifyChange(TestModel.CONTENT_URI, null);
        waitForResolver();
        assertTrue(notifyCalled.get());
        notifyCalled.set(false);

        getContext().getContentResolver().notifyChange(Uri.withAppendedPath(TestModel.CONTENT_URI, "1"), null);
        waitForResolver();
        assertTrue(notifyCalled.get());
        notifyCalled.set(false);

        getContext().getContentResolver()
                .notifyChange(Uri.withAppendedPath(TestModel.CONTENT_URI, "1/directory"), null);
        waitForResolver();
        assertTrue(notifyCalled.get());
    }

    private void waitForResolver() {
        // Registering an observer/notifying a change happen asynchronously,
        // so this hack waits for that to happen
        try {
            Thread.sleep(250L);
        } catch (InterruptedException e) {
            //
        }
    }
}
