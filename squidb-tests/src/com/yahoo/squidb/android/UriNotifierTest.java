package com.yahoo.squidb.android;

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;

import com.yahoo.squidb.data.AbstractModel;
import com.yahoo.squidb.data.SquidDatabase;
import com.yahoo.squidb.sql.SqlTable;
import com.yahoo.squidb.test.DatabaseTestCase;
import com.yahoo.squidb.test.Employee;
import com.yahoo.squidb.test.TestModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class UriNotifierTest extends DatabaseTestCase {

    private static class TestUriNotifier extends UriNotifier {

        public TestUriNotifier(Context context) {
            super(context, TestModel.TABLE);
        }

        @Override
        protected boolean accumulateNotificationObjects(Set<Uri> accumulatorSet, SqlTable<?> table,
                SquidDatabase database, DBOperation operation, AbstractModel modelValues, long rowId) {
            return accumulatorSet.add(TestModel.CONTENT_URI);
        }
    }

    private List<ContentObserver> observers = new ArrayList<ContentObserver>();

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        database.unregisterAllDataChangedNotifiers();
        for (ContentObserver observer : observers) {
            ContextProvider.getContext().getContentResolver().unregisterContentObserver(observer);
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
        ContextProvider.getContext().getContentResolver().registerContentObserver(uri, notifyForDescendants, observer);
        return gotUriNotification;
    }

    public void testUriNotificationOccurs() {
        AtomicBoolean notified = listenTo(TestModel.CONTENT_URI, false);
        waitForResolver();

        database.registerDataChangedNotifier(new TestUriNotifier(ContextProvider.getContext()));
        insertBasicTestModel();
        waitForResolver();
        assertTrue(notified.get());
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
        UriNotifier idNotifier = new TestUriNotifier(ContextProvider.getContext()) {
            @Override
            protected boolean accumulateNotificationObjects(Set<Uri> accumulatorSet, SqlTable<?> table,
                    SquidDatabase database, DBOperation operation, AbstractModel modelValues, long rowId) {
                Uri uri = TestModel.CONTENT_URI;
                if (rowId > 0) {
                    uri = uri.buildUpon().appendPath(Long.toString(rowId)).build();
                }
                return accumulatorSet.add(uri);
            }
        };

        database.registerDataChangedNotifier(idNotifier);

        AtomicBoolean uri1 = listenTo(Uri.withAppendedPath(TestModel.CONTENT_URI, "1"), false);
        AtomicBoolean uri2 = listenTo(Uri.withAppendedPath(TestModel.CONTENT_URI, "2"), false);
        AtomicBoolean uri3 = listenTo(Uri.withAppendedPath(TestModel.CONTENT_URI, "3"), false);
        AtomicBoolean uri4 = listenTo(Uri.withAppendedPath(TestModel.CONTENT_URI, "4"), false);
        waitForResolver();

        database.beginTransaction();
        try {
            insertBasicTestModel("Peter", "Quincy Taggart", System.currentTimeMillis() - 5);
            insertBasicTestModel("Guy", "Fleegman", System.currentTimeMillis() - 4);
            assertFalse(uri1.get());
            assertFalse(uri2.get());

            if (addNestedTransaction) {
                database.beginTransaction();
                try {
                    insertBasicTestModel("Young", "Laredo", System.currentTimeMillis() - 3);
                    insertBasicTestModel("Doctor", "Lazarus", System.currentTimeMillis() - 2);

                    if (nestedSuccessful) {
                        database.setTransactionSuccessful();
                    }
                } finally {
                    database.endTransaction();
                    assertFalse(uri1.get());
                    assertFalse(uri2.get());
                    assertFalse(uri3.get());
                    assertFalse(uri4.get());
                }
            }

            if (setSuccessful) {
                database.setTransactionSuccessful();
            }
        } finally {
            database.endTransaction();
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

        ContextProvider.getContext().getContentResolver().notifyChange(TestModel.CONTENT_URI, null);
        waitForResolver();
        assertTrue(notifyCalled.get());
        notifyCalled.set(false);

        ContextProvider.getContext().getContentResolver()
                .notifyChange(Uri.withAppendedPath(TestModel.CONTENT_URI, "1"), null);
        waitForResolver();
        assertTrue(notifyCalled.get());
        notifyCalled.set(false);

        ContextProvider.getContext().getContentResolver()
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

    public void testUriNotifierConstructors() {
        testNotifierConstructorsInternal(new UriNotifier(ContextProvider.getContext()) {
            @Override
            protected boolean accumulateNotificationObjects(Set<Uri> accumulatorSet, SqlTable<?> table,
                    SquidDatabase database, DBOperation operation, AbstractModel modelValues, long rowId) {
                return false;
            }
        });
        testNotifierConstructorsInternal(new UriNotifier(ContextProvider.getContext(), TestModel.TABLE) {
            @Override
            protected boolean accumulateNotificationObjects(Set<Uri> accumulatorSet, SqlTable<?> table,
                    SquidDatabase database, DBOperation operation, AbstractModel modelValues, long rowId) {
                return false;
            }
        }, TestModel.TABLE);

        testNotifierConstructorsInternal(
                new UriNotifier(ContextProvider.getContext(), Arrays.asList(TestModel.TABLE, Employee.TABLE)) {
                    @Override
                    protected boolean accumulateNotificationObjects(Set<Uri> accumulatorSet, SqlTable<?> table,
                            SquidDatabase database, DBOperation operation, AbstractModel modelValues, long rowId) {
                        return false;
                    }
                }, TestModel.TABLE, Employee.TABLE);
    }

    private void testNotifierConstructorsInternal(UriNotifier notifier, SqlTable<?>... tables) {
        Set<SqlTable<?>> whichTables = notifier.whichTables();
        assertEquals(tables.length, whichTables.size());
        assertTrue(whichTables.containsAll(Arrays.asList(tables)));
    }
}
