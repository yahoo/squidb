/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.data;

import com.yahoo.squidb.sql.SqlTable;

import java.util.Collection;
import java.util.Set;

/**
 * SimpleDataChangedNotifier is a very basic implementation of {@link DataChangedNotifier}. Subclasses of
 * SimpleDataChangedNotifier override a single, no-arg method: {@link #onDataChanged()}. This method will be called once
 * at the end of any statement or transaction that modifies one or more of the tables that the notifier is listening to.
 * In other words, it functions as a simple listener that can execute arbitrary code after any change to a table.
 * <p>
 * Note: Be wary of making further database changes from within onDataChanged()! It could trigger recursive data change
 * notifications, which could eventually lead to a StackOverflowException or an infinite loop.
 */
public abstract class SimpleDataChangedNotifier extends DataChangedNotifier<SimpleDataChangedNotifier> {

    /**
     * Construct a SimpleDataChangedNotifier that will be notified of changes to all tables
     */
    public SimpleDataChangedNotifier() {
        super();
    }

    /**
     * Construct a SimpleDataChangedNotifier that will be notified of changes to the given tables
     */
    public SimpleDataChangedNotifier(SqlTable<?>... tables) {
        super(tables);
    }

    /**
     * Construct a SimpleDataChangedNotifier that will be notified of changes to the given tables
     */
    public SimpleDataChangedNotifier(Collection<? extends SqlTable<?>> tables) {
        super(tables);
    }

    @Override
    protected final boolean accumulateNotificationObjects(Set<SimpleDataChangedNotifier> accumulatorSet,
            SqlTable<?> table, SquidDatabase database, DBOperation operation, AbstractModel modelValues, long rowId) {
        return accumulatorSet.add(this);
    }

    @Override
    protected final void sendNotification(SquidDatabase database, SimpleDataChangedNotifier notifyObject) {
        notifyObject.onDataChanged();
    }

    /**
     * By overriding this method, subclasses of SimpleDataChangedNotifier can run arbitrary code after statements or
     * transactions that modify the tables the notifier listens to.
     */
    protected abstract void onDataChanged();

}
