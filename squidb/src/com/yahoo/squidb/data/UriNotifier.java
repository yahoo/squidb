/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.data;

import android.content.ContentResolver;
import android.net.Uri;

import com.yahoo.squidb.sql.SqlTable;
import com.yahoo.squidb.sql.Table;
import com.yahoo.squidb.sql.View;
import com.yahoo.squidb.utility.SquidUtilities;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * UriNotifiers can be registered with an instance of {@link DatabaseDao} to receive notifications whenever a table
 * they are interested in is updated.
 * <p>
 * A UriNotifier can be constructed to listen for database operations on specific instances of {@link SqlTable} (a
 * {@link Table}, a {@link View}, etc.) or using model classes (the tables they correspond to can be extracted
 * automatically). If you want your UriNotifier instance to be notified of all database operations regardless of table,
 * use the no-argument constructor.
 * <p>
 * When an instance of UriNotifier is registered with a DatabaseDao, the dao will call {@link #addUrisToNotify(Set,
 * SqlTable, String, DBOperation, AbstractModel, long) addUrisToNotify} on the notifier whenever one of the
 * notifier's relevant tables was modified. Subclasses should override this method to construct a Uri to notify based
 * on the parameters passed to the method.
 *
 * @see com.yahoo.squidb.data.DatabaseDao#registerUriNotifier(UriNotifier)
 * @see #addUrisToNotify(java.util.Set, com.yahoo.squidb.sql.SqlTable, String, com.yahoo.squidb.data.UriNotifier.DBOperation,
 * AbstractModel, long)
 */
public abstract class UriNotifier {

    /**
     * Enumerates the possible database write operations
     */
    public enum DBOperation {
        INSERT,
        UPDATE,
        DELETE
    }

    private final List<SqlTable<?>> tables = new ArrayList<SqlTable<?>>();

    /**
     * Construct a UriNotifier that will be notified of changes to all tables
     */
    public UriNotifier() {
        // Valid for all tables
    }

    /**
     * For constructing a UriNotifier that will be notified of changes to the given tables
     */
    public UriNotifier(SqlTable<?>... tables) {
        SquidUtilities.addAll(this.tables, tables);
    }

    /**
     * @return a list of {@link SqlTable SqlTables} that this UriNotifier wants to receive notifications about. If
     * this method returns an empty list, it will receive notifications about all database updates.
     */
    public List<SqlTable<?>> whichTables() {
        return tables;
    }

    /**
     * By overriding this method, subclasses of UriNotifier can create Uris for the dao to notify when various kinds of
     * database operations have completed successfully. Whenever a database change occurs, the dao will call this
     * method on UriNotifiers registered to the relevant table. The UriNotifier should construct a Uri to notify
     * based on the method parameters and add it to the set. Multiple Uris can be added if desired. A notification
     * will then be sent to the Uris in the set via the {@link ContentResolver} when the current transaction
     * completes successfully, or immediately if no transaction is ongoing.
     * <p>
     * Most UriNotifiers will probably not need all these parameters. For example:
     *
     * <pre>
     * public void addUrisToNotify(Set<Uri> uris, SqlTable<?> table, String databaseName, DBOperation operation,
     *     AbstractModel modelValues, long rowId) {
     *     // Notifies some constant Uri for any update on the students table
     *     if (Student.TABLE.equals(table)) {
     *         uris.add(Student.CONTENT_URI);
     *     }
     * }
     * </pre>
     *
     * @param uris add uris to notify to this accumulator set
     * @param table the affected table.
     * @param databaseName the name of the database
     * @param operation the type of database write that occurred
     * @param modelValues the model values that triggered this database update. This parameter may be null; the dao
     * will provide it when possible, but it is not always present. If you only need a row id, check the rowId
     * parameter. This parameter will be null for delete operations, and will contain only the changed columns and
     * their new values for updates.
     * @param rowId the single row id that was updated, if applicable
     */
    public abstract void addUrisToNotify(Set<Uri> uris, SqlTable<?> table, String databaseName, DBOperation operation,
            AbstractModel modelValues, long rowId);
}
