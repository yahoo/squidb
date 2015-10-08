/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.data.android;

import android.content.ContentResolver;
import android.net.Uri;

import com.yahoo.squidb.data.AbstractModel;
import com.yahoo.squidb.data.DataChangedNotifier;
import com.yahoo.squidb.data.SquidDatabase;
import com.yahoo.squidb.sql.SqlTable;
import com.yahoo.squidb.sql.Table;
import com.yahoo.squidb.sql.View;

import java.util.Collection;
import java.util.Set;

/**
 * UriNotifiers can be registered with an instance of {@link SquidDatabase} to receive notifications whenever a table
 * they are interested in is updated.
 * <p>
 * A UriNotifier can be constructed to listen for database operations on specific instances of {@link SqlTable} (a
 * {@link Table}, a {@link View}, etc.) or using model classes (the tables they correspond to can be extracted
 * automatically). If you want your UriNotifier instance to be notified of all database operations regardless of table,
 * use the no-argument constructor.
 * <p>
 * When an instance of UriNotifier is registered with a SquidDatabase, the db will call {@link
 * #accumulateNotificationObjects(Set, SqlTable, SquidDatabase, DataChangedNotifier.DBOperation, AbstractModel, long)} on the
 * notifier whenever one of the notifier's relevant tables was modified. Subclasses should override this method to
 * construct a Uri to notify based on the parameters passed to the method.
 *
 * @see DataChangedNotifier
 * @see SquidDatabase#registerDataChangedNotifier(DataChangedNotifier)
 */
public abstract class UriNotifier extends DataChangedNotifier<Uri> {

    /**
     * Construct a UriNotifier that will be notified of changes to all tables
     */
    public UriNotifier() {
        super();
    }

    /**
     * Construct a UriNotifier that will be notified of changes to the given tables
     */
    public UriNotifier(SqlTable<?>... tables) {
        super(tables);
    }

    /**
     * Construct a UriNotifier that will be notified of changes to the given tables
     */
    public UriNotifier(Collection<? extends SqlTable<?>> tables) {
        super(tables);
    }

    /**
     * By overriding this method, subclasses of UriNotifier can create Uris for the database to notify when various
     * kinds of database operations have completed successfully. Whenever a database change occurs, the database will
     * call this method on UriNotifiers registered to the relevant table. The UriNotifier should construct a Uri to
     * notify based on the method parameters and add it to the set. Multiple Uris can be added if desired. A
     * notification will then be sent to the Uris in the set via the {@link ContentResolver} when the current
     * transaction completes successfully, or immediately if no transaction is ongoing.
     * <p>
     * Most UriNotifiers will probably not need all these parameters. For example:
     *
     * <pre>
     * protected boolean accumulateNotificationObjects(Set&lt;Uri&gt; accumulatorSet, SqlTable&lt;?&gt; table,
     *     SquidDatabase database, DBOperation operation, AbstractModel modelValues, long rowId) {
     *     // Notifies some constant Uri for any update on the students table
     *     if (Student.TABLE.equals(table)) {
     *         return uris.add(Student.CONTENT_URI);
     *     }
     *     return false;
     * }
     * </pre>
     *
     * @param accumulatorSet add uris to notify to this accumulator set
     * @param table the affected table.
     * @param database the SquidDatabase instance this change occurred in
     * @param operation the type of database write that occurred
     * @param modelValues the model values that triggered this database update. This parameter may be null; the database
     * will provide it when possible, but it is not always present. If you only need a row id, check the rowId
     * parameter. This parameter will be null for delete operations, and will contain only the changed columns and
     * their new values for updates.
     * @param rowId the single row id that was updated, if applicable
     * @return true if any Uris were added to the accumulator set to be notified, false otherwise
     */
    @Override
    protected abstract boolean accumulateNotificationObjects(Set<Uri> accumulatorSet, SqlTable<?> table,
            SquidDatabase database, DBOperation operation, AbstractModel modelValues, long rowId);

    @Override
    protected void sendNotification(SquidDatabase database, Uri notifyObject) {
        database.notifyChange(notifyObject);
    }
}
