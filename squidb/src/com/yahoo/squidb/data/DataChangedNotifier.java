/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.data;

import com.yahoo.squidb.sql.SqlTable;
import com.yahoo.squidb.sql.Table;
import com.yahoo.squidb.sql.View;
import com.yahoo.squidb.utility.SquidUtilities;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Subclasses of DataChangedNotifier can be registered with an instance of {@link SquidDatabase} to receive
 * notifications whenever a table they are interested in is updated.
 * <p>
 * A DataChangedNotifier can be constructed to listen for database operations on specific instances of {@link SqlTable}
 * (a {@link Table}, a {@link View}, etc.). If you want your DataChangedNotifier instance to be notified of all database
 * operations regardless of table, use the no-argument constructor.
 * <p>
 * When an instance of DataChangedNotifier is registered with a SquidDatabase, the db will call {@link
 * #onDataChanged(SqlTable, SquidDatabase, DBOperation, AbstractModel, long)} on the notifier whenever one of the
 * notifier's relevant tables was modified.
 * <p>
 * Subclasses must override two abstract methods: {@link #accumulateNotificationObjects(Set, SqlTable, SquidDatabase,
 * DBOperation, AbstractModel, long)} and {@link #sendNotification(SquidDatabase, Object)}. In
 * accumulateNotificationObjects, the DataChangedNotifier subclass should add objects/metadata about that notification
 * that needs to be sent for that data change when the statement transaction has completed successfully. When the
 * statement or transaction completes successfully, sendNotification will be called for each object that was accumulated
 * during the transaction. The subclass should define in this method how to actually send the notification.
 *
 * @param <T> the type of object/metadata to accumulate for sending notifications
 * @see SquidDatabase#registerDataChangedNotifier(DataChangedNotifier)
 * @see {com.yahoo.squidb.android.UriNotifier} for an example of a DataChangedNotifier that can send
 * ContentObserver notifications to Uris on data changes
 */
public abstract class DataChangedNotifier<T> {

    /**
     * Enumerates the possible database write operations
     */
    public enum DBOperation {
        INSERT,
        UPDATE,
        DELETE
    }

    private final Set<SqlTable<?>> tables = new HashSet<SqlTable<?>>();
    private boolean enabled = true;

    // Using a ThreadLocal makes it easy to have one accumulator set per transaction, since
    // transactions are also associated with the thread they run on
    private ThreadLocal<Set<T>> notifyObjectAccumulator = new ThreadLocal<Set<T>>() {
        protected Set<T> initialValue() {
            return new HashSet<T>();
        }
    };

    /**
     * Construct a DataChangedNotifier that will be notified of changes to all tables
     */
    public DataChangedNotifier() {
        // Valid for all tables
    }

    /**
     * For constructing a DataChangedNotifier that will be notified of changes to the given tables
     */
    public DataChangedNotifier(SqlTable<?>... tables) {
        SquidUtilities.addAll(this.tables, tables);
    }

    /**
     * For constructing a DataChangedNotifier that will be notified of changes to the given tables
     */
    public DataChangedNotifier(Collection<? extends SqlTable<?>> tables) {
        this.tables.addAll(tables);
    }

    /**
     * @return a set of {@link SqlTable SqlTables} that this DataChangedNotifier wants to receive notifications about.
     * If this method returns an empty list, it will receive notifications about all database updates.
     */
    public Set<SqlTable<?>> whichTables() {
        return tables;
    }

    /**
     * Set whether or not this DataChangedNotifier is enabled. When not enabled, no data changed notifications will be
     * accumulated for any statement or transaction.
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    // Called by SquidDatabase for each data change
    final boolean onDataChanged(SqlTable<?> table, SquidDatabase database, DBOperation operation,
            AbstractModel modelValues, long rowId) {
        if (!enabled) {
            return false;
        }

        return accumulateNotificationObjects(notifyObjectAccumulator.get(), table, database, operation,
                modelValues, rowId);
    }

    /**
     * Subclasses override this abstract method to accumulate objects to notify at the end of a successful transaction.
     * For example, in UriNotifier the objects to notify are Uris (so UriNotifier extends
     * DataChangedNotifier&lt;Uri&gt;). If you want to just run arbitrary code after a data change, the object could be
     * a Runnable.
     *
     * @param accumulatorSet add objects to be notified at the end of a successful transaction to this data set
     * @param table the affected table.
     * @param database the SquidDatabase instance this change occurred in
     * @param operation the type of database write that occurred
     * @param modelValues the model values that triggered this database update. This parameter may be null; the database
     * will provide it when possible, but it is not always present. If you only need a row id, check the rowId
     * parameter. This parameter will be null for delete operations, and will contain only the changed columns and
     * their new values for updates.
     * @param rowId the single row id that was updated, if applicable
     * @return true if any objects were added to the accumulator set to be notified, false otherwise
     */
    protected abstract boolean accumulateNotificationObjects(Set<T> accumulatorSet, SqlTable<?> table,
            SquidDatabase database, DBOperation operation, AbstractModel modelValues, long rowId);

    // Called by SquidDatabase when a transaction or statement has finished and any accumulated notifications should be
    // flushed/sent
    final void flushAccumulatedNotifications(SquidDatabase database, boolean shouldSendNotifications) {
        Set<T> accumulatedNotifications = notifyObjectAccumulator.get();
        if (enabled && shouldSendNotifications) {
            sendNotificationsToAll(database, accumulatedNotifications);
        }
        accumulatedNotifications.clear();
    }

    /**
     * The default implementation of this method iterates over the notifyObjects set and calls
     * {@link #sendNotification(SquidDatabase, Object)} for each of them. Subclasses may override if they want to
     * handle notifying the entire set differently.
     *
     * @param database the SquidDatabase the change occurred in
     * @param notifyObjects the objects to be used for sending a notification
     */
    protected void sendNotificationsToAll(SquidDatabase database, Set<T> notifyObjects) {
        for (T notifyObject : notifyObjects) {
            sendNotification(database, notifyObject);
        }
    }

    /**
     * Subclasses override this abstract method to define how to send a notification to the accumulated notifyObject.
     * For example, in UriNotifier, the object is a Uri, so UriNotifier calls
     * {@link android.content.ContentResolver#notifyChange(android.net.Uri, android.database.ContentObserver)
     * ContentResolver.notifyChange} with the Uri. If the object were a Runnable, the caller could simply call
     * {@link Runnable#run() run()}.
     *
     * @param database the SquidDatabase the change occurred in
     * @param notifyObject the object to be used for sending a notification
     */
    protected abstract void sendNotification(SquidDatabase database, T notifyObject);

}
