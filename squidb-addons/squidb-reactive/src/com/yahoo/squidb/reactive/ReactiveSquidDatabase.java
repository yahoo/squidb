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
import com.yahoo.squidb.data.android.AndroidSquidDatabase;
import com.yahoo.squidb.sql.SqlTable;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import rx.Observable;
import rx.functions.Func1;
import rx.subjects.PublishSubject;

/**
 * ReactiveSquidDatabase is an extension of SquidDatabase that enables RxJava-style observation of table changes.
 * Users can call {@link #observeTable(SqlTable)}, {@link #observeTableAndEmit(SqlTable, Object)}, or
 * {@link #observeTablesAndEmit(Collection, Object)} to create {@link Observable}s that will emit objects to
 * subscribers whenever the given table(s) are written to successfully.
 * <p>
 * {@link #observeTable(SqlTable)} acts as a simple listener that just emits the table itself, while
 * {@link #observeTableAndEmit(SqlTable, Object)} and {@link #observeTablesAndEmit(Collection, Object)} can
 * pass an object that they would like to be emitted--for example, a SquiDB Query to be run.
 * <p>
 * By default, created observables will only emit objects to subscribers registered before table changes. To make the
 * observables immediately emit the requested object on first subscribe, use the alternative version of the "observe"
 * method that takes a boolean argument and pass true for "emitOnFirstSubscribe". This can be used to issue a query
 * and automatically requery on a change:
 * <pre>
 * final ReactiveSquidDatabase db = ...;
 * Query query = Query.select(...)...;
 * Observable&lt;Query&gt; observable = db.observeTable(Model.TABLE, query, true);
 * observable.subscribe(new Action1&lt;Query&gt;() {
 *
 *     &#064;Override
 *     public void call(Query query) {
 *         SquidCursor&lt;Model&gt; cursor = db.query(Model.class, query);
 *         // do something with cursor
 *     }
 * });
 * </pre>
 * Note: If data changed notifications are disabled on an instance of ReactiveSquidDatabase using
 * {@link #setDataChangedNotificationsEnabled(boolean)}, the observables created from it won't emit any events either.
 */
public abstract class ReactiveSquidDatabase extends AndroidSquidDatabase {

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
     * Convenience method for {@link #observeTable(SqlTable, boolean) observeTable(table, false)};
     */
    public <T extends SqlTable<?>> Observable<T> observeTable(T table) {
        return observeTable(table, false);
    }

    /**
     * Create an {@link Observable} that emits the specified table whenever that table is written to in a successful
     * statement or transaction. To create an Observable that emits some other object, use {@link
     * #observeTableAndEmit(SqlTable, Object)}.
     *
     * @param table the table to observe
     * @param emitOnFirstSubscribe pass true if you want the first subscriber to the observable to be triggered
     * immediately
     * @return a new Observable
     */
    public <T extends SqlTable<?>> Observable<T> observeTable(T table, boolean emitOnFirstSubscribe) {
        return observeTableAndEmit(table, table, emitOnFirstSubscribe);
    }

    /**
     * Convenience method for
     * {@link #observeTableAndEmit(SqlTable, Object, boolean) observeTableAndEmit(table, objectToEmit, false)};
     */
    public <T> Observable<T> observeTableAndEmit(SqlTable<?> table, T objectToEmit) {
        return observeTableAndEmit(table, objectToEmit, false);
    }

    /**
     * Create an {@link Observable} that emits the specified object whenever the specified table is written to in a
     * successful statement or transaction.
     *
     * @param objectToEmit an object for the created Observable to emit
     * @param table the table to observe
     * @param emitOnFirstSubscribe pass true if you want the first subscriber to the observable to be triggered
     * immediately
     * @return a new Observable
     */
    public <T> Observable<T> observeTableAndEmit(final SqlTable<?> table, T objectToEmit,
            boolean emitOnFirstSubscribe) {
        if (table == null) {
            throw new IllegalArgumentException("Cannot observe a null table");
        }

        return observeAndEmit(objectToEmit, new Func1<Set<SqlTable<?>>, Boolean>() {
            @Override
            public Boolean call(Set<SqlTable<?>> changedTables) {
                return changedTables.contains(table);
            }
        }, emitOnFirstSubscribe);
    }

    /**
     * Convenience method for
     * {@link #observeTablesAndEmit(Collection, Object, boolean) observeTablesAndEmit(table, objectToEmit, false)};
     */
    public <T> Observable<T> observeTablesAndEmit(Collection<? extends SqlTable<?>> tables, T objectToEmit) {
        return observeTablesAndEmit(tables, objectToEmit, false);
    }

    /**
     * Create an {@link Observable} that emits the specified object whenever any of the specified tables is written to
     * in a successful statement or transaction.
     *
     * @param objectToEmit an object for the created Observable to emit
     * @param tables the tables to observe
     * @param emitOnFirstSubscribe pass true if you want the first subscriber to the observable to be triggered
     * immediately
     * @return a new Observable
     */
    public <T> Observable<T> observeTablesAndEmit(final Collection<? extends SqlTable<?>> tables, T objectToEmit,
            boolean emitOnFirstSubscribe) {
        if (tables == null) {
            throw new IllegalArgumentException("Cannot observe a null table collection");
        }
        return observeAndEmit(objectToEmit, new Func1<Set<SqlTable<?>>, Boolean>() {
            @Override
            public Boolean call(Set<SqlTable<?>> changedTables) {
                for (SqlTable<?> table : tables) {
                    if (changedTables.contains(table)) {
                        return true;
                    }
                }
                return false;
            }
        }, emitOnFirstSubscribe);
    }

    private <T> Observable<T> observeAndEmit(final T objectToEmit, Func1<Set<SqlTable<?>>, Boolean> tableFilter,
            boolean emitOnFirstSubscribe) {
        Observable<Set<SqlTable<?>>> observable = changedTablePublisher.filter(tableFilter);
        if (emitOnFirstSubscribe) {
            observable = observable.startWith(INITIAL_TABLE);
        }
        return observable.map(new Func1<Set<SqlTable<?>>, T>() {
            @Override
            public T call(Set<SqlTable<?>> sqlTables) {
                return objectToEmit;
            }
        });

    }
}
