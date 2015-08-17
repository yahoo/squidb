/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.reactive;

import android.content.Context;

import com.yahoo.squidb.data.AbstractModel;
import com.yahoo.squidb.data.DataChangedNotifier;
import com.yahoo.squidb.data.SquidDatabase;
import com.yahoo.squidb.sql.SqlTable;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import rx.Observable;
import rx.functions.Func1;
import rx.subjects.PublishSubject;

/**
 * ReactiveSquidDatabase is an extension of SquidDatabase that enables RxJava-style observation of table changes.
 * Users can call {@link #observeTable(SqlTable)}, {@link #observeTableForObject(Object, SqlTable)}, or
 * {@link #observeTablesForObject(Object, SqlTable[])} to create {@link Observable}s that will emit objects to
 * subscribers whenever the given table(s) are written to successfully.
 * <p>
 * {@link #observeTable(SqlTable)} acts as a simple listener that just emits the table itself, while
 * {@link #observeTableForObject(Object, SqlTable)} and {@link #observeTablesForObject(Object, SqlTable[])} can
 * pass an object that they would like to be emitted--for example, a SquiDB Query to be run.
 * <p>
 * Note: If data changed notifications are disabled on an instance of ReactiveSquidDatabase using
 * {@link #setDataChangedNotificationsEnabled(boolean)}, the observables created from it won't emit any events either!
 */
public abstract class ReactiveSquidDatabase extends SquidDatabase {

    private final PublishSubject<Set<SqlTable<?>>> changedTablePublisher = PublishSubject.create();

    private static final Set<SqlTable<?>> INITIAL_TABLE = new HashSet<SqlTable<?>>();
    static {
        INITIAL_TABLE.add(new SqlTable<AbstractModel>(null, null, "<initial>") {
        });
    }

    private class PublishingDataChangedNotifier extends DataChangedNotifier<SqlTable<?>> {

        @Override
        protected boolean accumulateNotificationObjects(Set<SqlTable<?>> accumulatorSet, SqlTable<?> table,
                SquidDatabase database, DBOperation operation, AbstractModel modelValues, long rowId) {
            return accumulatorSet.add(table);
        }

        @Override
        protected void sendNotificationsToAll(SquidDatabase database, Set<SqlTable<?>> notifyObjects) {
            changedTablePublisher.onNext(notifyObjects);
        }

        @Override
        protected void sendNotification(SquidDatabase database, SqlTable<?> notifyObject) {
            throw new UnsupportedOperationException("Can't send onNext to the publisher with a single table. This " +
                    "should never happen anyway.");
        }
    }

    /**
     * Create a new ReactiveSquidDatabase.
     *
     * @param context the Context, must not be null
     */
    public ReactiveSquidDatabase(Context context) {
        super(context);
        registerDataChangedNotifier(new PublishingDataChangedNotifier());
    }

    /**
     * @param table the table to observe
     * @return a new {@link Observable} that will be called whenever the given table is written to in a successful
     * statement or transaction. The Observable will emit the table itself; to emit some other object like a Query, use
     * {@link #observeTableForObject(Object, SqlTable)}
     */
    public <T extends SqlTable<?>> Observable<T> observeTable(T table) {
        return observeTableForObject(table, table);
    }

    /**
     * @param objectToEmit an object for the created Observable to emit
     * @param table the table to observe
     * @return a new {@link Observable} that will be called whenever the given table is written to in a successful
     * statement or transaction. The Observable will emit the object passed in this method; for example, a Query to run.
     */
    public <T> Observable<T> observeTableForObject(T objectToEmit, final SqlTable<?> table) {
        if (table == null) {
            throw new IllegalArgumentException("Cannot observe a null table");
        }

        return observeForObject(objectToEmit, new Func1<Set<SqlTable<?>>, Boolean>() {
            @Override
            public Boolean call(Set<SqlTable<?>> changedTables) {
                return changedTables.contains(table);
            }
        });
    }

    /**
     * @param objectToEmit an object for the created Observable to emit
     * @param tables the tables to observe
     * @return a new {@link Observable} that will be called whenever any of the given tables are written to in a
     * successful statement or transaction. The Observable will emit the object passed in this method; for example, a
     * Query to run.
     */
    public <T> Observable<T> observeTablesForObject(T objectToEmit, final SqlTable<?>... tables) {
        return observeForObject(objectToEmit, new Func1<Set<SqlTable<?>>, Boolean>() {
            @Override
            public Boolean call(Set<SqlTable<?>> changedTables) {
                for (SqlTable<?> table : tables) {
                    if (changedTables.contains(table)) {
                        return true;
                    }
                }
                return false;
            }
        });
    }

    /**
     * @param objectToEmit an object for the created Observable to emit
     * @param tables the tables to observe
     * @return a new {@link Observable} that will be called whenever any of the given tables are written to in a
     * successful statement or transaction. The Observable will emit the object passed in this method; for example, a
     * Query to run.
     */
    public <T> Observable<T> observeTablesForObject(T objectToEmit, final Collection<SqlTable<?>> tables) {
        return observeForObject(objectToEmit, new Func1<Set<SqlTable<?>>, Boolean>() {
            @Override
            public Boolean call(Set<SqlTable<?>> changedTables) {
                for (SqlTable<?> table : tables) {
                    if (changedTables.contains(table)) {
                        return true;
                    }
                }
                return false;
            }
        });
    }

    private <T> Observable<T> observeForObject(final T objectToEmit, Func1<Set<SqlTable<?>>, Boolean> tableFilter) {
        if (inTransaction()) {
            throw new IllegalStateException("Can't subscribe to observable in a transaction");
        }
        return changedTablePublisher
                .filter(tableFilter)
                .startWith(INITIAL_TABLE)
                .map(new Func1<Set<SqlTable<?>>, T>() {
                    @Override
                    public T call(Set<SqlTable<?>> sqlTables) {
                        return objectToEmit;
                    }
                });

    }
}
