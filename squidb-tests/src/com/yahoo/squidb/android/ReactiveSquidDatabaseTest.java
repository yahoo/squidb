/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.android;

import com.yahoo.squidb.reactive.ReactiveSquidDatabase;
import com.yahoo.squidb.sql.Query;
import com.yahoo.squidb.sql.Table;
import com.yahoo.squidb.test.Employee;
import com.yahoo.squidb.test.SquidTestCase;
import com.yahoo.squidb.test.TestDatabase;
import com.yahoo.squidb.test.TestModel;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;

public class ReactiveSquidDatabaseTest extends SquidTestCase {

    private ReactiveSquidDatabase database;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        setupDatabase();
    }

    /**
     * Called during {@link #setUp()} to initialize the database. The base implementation creates a new
     * {@link TestDatabase}. Subclasses that want to insert test data should override and call super, then perform its
     * operations.
     */
    protected void setupDatabase() {
        database = new TestReactiveDatabase();
        database.clear();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        tearDownDatabase();
    }

    /**
     * Called during {@link #tearDown()} to clean up any databases. The base implementation tries to close the database
     * created in {@link #setupDatabase()}.
     */
    protected void tearDownDatabase() {
        if (database != null) {
            database.close();
        }
    }

    public void testObservableWithInitialSubscribeFlagEmitsOnFirstSubscribe() {
        final AtomicBoolean called = new AtomicBoolean(false);
        Observable<Table> observable = database.observeTable(TestModel.TABLE, true);
        observable.subscribe(new Action1<Table>() {
            @Override
            public void call(Table table) {
                called.set(true);
            }
        });
        assertTrue(called.get());
    }

    public void testSimpleObservableEmitsTable() {
        final AtomicBoolean tablesMatch = new AtomicBoolean(false);
        Observable<Table> observable = database.observeTable(TestModel.TABLE, true);
        observable.subscribe(new Action1<Table>() {
            @Override
            public void call(Table table) {
                tablesMatch.set(TestModel.TABLE.equals(table));
            }
        });
        assertTrue(tablesMatch.get());
    }

    public void testObservableEmitsCustomObject() {
        final AtomicBoolean objectsMatch = new AtomicBoolean(false);
        final Query originalQuery = Query.select().from(TestModel.TABLE);
        Observable<Query> observable = database.observeTableAndEmit(TestModel.TABLE, originalQuery, true);
        observable.subscribe(new Action1<Query>() {
            @Override
            public void call(Query query) {
                objectsMatch.set(originalQuery == query);
            }
        });
        assertTrue(objectsMatch.get());

    }

    public void testObservableEmitsOncePerTransaction() {
        testMultipleStatements(true, true);
    }

    public void testObservableEmitsNothingAfterFailedTransaction() {
        testMultipleStatements(true, false);
    }

    public void testObservableCalledForEachChange() {
        testMultipleStatements(false, false);
    }

    private void testMultipleStatements(boolean useTransaction, boolean successfulTransaction) {
        final AtomicInteger callCount = new AtomicInteger();
        Observable<Table> observable = database.observeTable(TestModel.TABLE);
        observable.subscribe(new Action1<Table>() {
            @Override
            public void call(Table table) {
                callCount.incrementAndGet();
            }
        });
        assertEquals(0, callCount.get());
        if (useTransaction) {
            database.beginTransaction();
        }
        try {
            database.persist(new TestModel().setFirstName("A").setLastName("B")
                    .setBirthday(System.currentTimeMillis() - 2));
            database.persist(new TestModel().setFirstName("C").setLastName("D")
                    .setBirthday(System.currentTimeMillis() - 1));
            if (useTransaction && successfulTransaction) {
                database.setTransactionSuccessful();
            }
        } finally {
            if (useTransaction) {
                database.endTransaction();
            }
        }
        int expectedCount;
        if (useTransaction) {
            expectedCount = successfulTransaction ? 1 : 0;
        } else {
            expectedCount = 2;
        }
        assertEquals(expectedCount, callCount.get());
    }

    public void testObserveMultipleTables() {
        testObserveMultipleTables(true);
        testObserveMultipleTables(false);
    }

    private void testObserveMultipleTables(boolean useTransaction) {
        AtomicInteger callCount = new AtomicInteger();
        Observable<AtomicInteger> observable = database.observeTablesAndEmit(
                Arrays.asList(TestModel.TABLE, Employee.TABLE), callCount);
        observable.subscribe(new Action1<AtomicInteger>() {
            @Override
            public void call(AtomicInteger callCount) {
                callCount.incrementAndGet();
            }
        });
        assertEquals(0, callCount.get());
        if (useTransaction) {
            database.beginTransaction();
        }
        try {
            database.persist(
                    new TestModel().setFirstName("A").setLastName("B").setBirthday(System.currentTimeMillis()));
            database.persist(new Employee().setName("ABC").setIsHappy(true).setManagerId(0L));
            if (useTransaction) {
                database.setTransactionSuccessful();
            }
        } finally {
            if (useTransaction) {
                database.endTransaction();
            }
        }
        assertEquals(useTransaction ? 1 : 2, callCount.get());
        database.deleteAll(TestModel.class);
    }

    public void testObservableNotCalledForUnobservedTable() {
        AtomicInteger callCount = new AtomicInteger();
        Observable<AtomicInteger> observable = database.observeTableAndEmit(TestModel.TABLE, callCount);
        observable.subscribe(new Action1<AtomicInteger>() {
            @Override
            public void call(AtomicInteger callCount) {
                callCount.incrementAndGet();
            }
        });
        assertEquals(0, callCount.get());
        database.persist(new Employee().setName("ABC").setIsHappy(true).setManagerId(0L));
        assertEquals(0, callCount.get());
    }

    public void testUnsubscribeStopsNotifications() {
        AtomicInteger callCount = new AtomicInteger();
        Observable<AtomicInteger> observable = database.observeTableAndEmit(Employee.TABLE, callCount);
        Subscription s = observable.subscribe(new Action1<AtomicInteger>() {
            @Override
            public void call(AtomicInteger callCount) {
                callCount.incrementAndGet();
            }
        });
        assertEquals(0, callCount.get());
        database.persist(new Employee().setName("ABC").setIsHappy(true).setManagerId(0L));
        assertEquals(1, callCount.get());

        s.unsubscribe();
        database.persist(new Employee().setName("DEF").setIsHappy(true).setManagerId(0L));
        assertEquals(1, callCount.get());
    }

    public void testSubscribeDuringTransaction() {
        AtomicInteger callCount = new AtomicInteger();
        Observable<AtomicInteger> observable = database.observeTableAndEmit(Employee.TABLE, callCount);
        database.beginTransaction();
        try {
            database.persist(new Employee().setName("ABC").setIsHappy(true).setManagerId(0L));
            observable.subscribe(new Action1<AtomicInteger>() {
                @Override
                public void call(AtomicInteger atomicInteger) {
                    atomicInteger.incrementAndGet();
                }
            });
            assertEquals(0, callCount.get());
            database.persist(new Employee().setName("DEF").setIsHappy(true).setManagerId(0L));
            assertEquals(0, callCount.get());
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
        assertEquals(1, callCount.get());
    }
}
