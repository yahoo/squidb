/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.data;

import android.database.Cursor;
import android.database.sqlite.SQLiteTransactionListener;
import android.net.Uri;

import com.yahoo.squidb.data.UriNotifier.DBOperation;
import com.yahoo.squidb.sql.CompiledStatement;
import com.yahoo.squidb.sql.Criterion;
import com.yahoo.squidb.sql.Delete;
import com.yahoo.squidb.sql.Insert;
import com.yahoo.squidb.sql.Property;
import com.yahoo.squidb.sql.Property.IntegerProperty;
import com.yahoo.squidb.sql.Query;
import com.yahoo.squidb.sql.SqlTable;
import com.yahoo.squidb.sql.TableStatement.ConflictAlgorithm;
import com.yahoo.squidb.sql.Update;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Class for reading data from an instance of {@link AbstractDatabase}.
 * <p>
 * As a convenience, when calling the {@link #query(Class, Query) query} and {@link #fetchByQuery(Class, Query)
 * fetchByQuery} methods, if the {@code query} argument does not have a FROM clause, the table or view to select from
 * will be inferred from the provided {@code modelClass} argument (if possible). This allows for invocations where
 * {@link Query#from(com.yahoo.squidb.sql.SqlTable) Query.from} is never explicitly called:
 *
 * <pre>
 * SquidCursor{@code<Person>} cursor =
 *         dao.query(Person.class, Query.select().orderBy(Person.NAME.asc()));
 * </pre>
 *
 * By convention, the {@code fetch...} methods return a single model instance corresponding to the first record found,
 * or null if no records are found for that particular form of fetch.
 */
public class DatabaseDao {

    private final AbstractDatabase database;

    public DatabaseDao(AbstractDatabase database) {
        this.database = database;
    }

    // --- dao methods

    protected String getDatabaseName() {
        return database.getName();
    }

    private SqlTable<?> getTableFrom(Class<? extends AbstractModel> modelClass) {
        return database.getTable(modelClass);
    }

    /**
     * Query the database
     *
     * @param modelClass the type to parameterize the cursor by. If the query does not contain a FROM clause, the table
     * or view corresponding to this model class will be used.
     * @param query the query to execute
     * @return a {@link SquidCursor} containing the query results
     */
    public <TYPE extends AbstractModel> SquidCursor<TYPE> query(Class<TYPE> modelClass, Query query) {
        if (!query.hasTable() && modelClass != null) {
            SqlTable<?> table = getTableFrom(modelClass);
            if (table == null) {
                throw new IllegalArgumentException("Query has no FROM clause and model class "
                        + modelClass.getSimpleName() + " has no associated table");
            }
            query = query.from(table); // If argument was frozen, we may get a new object
        }
        if (query.needsValidation()) {
            query.parenthesizeWhere(true);
            CompiledStatement compiled = query.compile();
            database.compileStatement(compiled.sql); // throws if the statement fails to compile
            query.parenthesizeWhere(false);
        }
        CompiledStatement compiled = query.compile();
        Cursor cursor = database.rawQuery(compiled.sql, compiled.sqlArgs);
        return new SquidCursor<TYPE>(cursor, query.getFields());
    }


    /**
     * Fetch the specified model object with the given row ID
     *
     * @param modelClass the model class to fetch
     * @param id the row ID of the item
     * @param properties the {@link Property properties} to read
     * @return an instance of the model with the given ID, or null if no record was found
     */
    public <TYPE extends TableModel> TYPE fetch(Class<TYPE> modelClass, long id, Property<?>... properties) {
        SquidCursor<TYPE> cursor = fetchItemById(modelClass, id, properties);
        return returnFetchResult(modelClass, cursor);
    }

    /**
     * Fetch the first model matching the given {@link Criterion}. This is useful if you expect uniqueness of models
     * with respect to the given criterion.
     *
     * @param modelClass the model class to fetch
     * @param properties the {@link Property properties} to read
     * @param criterion the criterion to match
     * @return an instance of the model matching the given criterion, or null if no record was found
     */
    public <TYPE extends AbstractModel> TYPE fetchByCriterion(Class<TYPE> modelClass, Criterion criterion,
            Property<?>... properties) {
        SquidCursor<TYPE> cursor = fetchFirstItem(modelClass, criterion, properties);
        return returnFetchResult(modelClass, cursor);
    }

    /**
     * Fetch the first model matching the query. This is useful if you expect uniqueness of models with respect to the
     * given query.
     *
     * @param modelClass the model class to fetch
     * @param query the query to execute
     * @return an instance of the model returned by the given query, or null if no record was found
     */
    public <TYPE extends AbstractModel> TYPE fetchByQuery(Class<TYPE> modelClass, Query query) {
        SquidCursor<TYPE> cursor = fetchFirstItem(modelClass, query);
        return returnFetchResult(modelClass, cursor);
    }

    protected <TYPE extends AbstractModel> TYPE returnFetchResult(Class<TYPE> modelClass, SquidCursor<TYPE> cursor) {
        try {
            if (cursor.getCount() == 0) {
                return null;
            }
            TYPE toReturn = modelClass.newInstance();
            toReturn.readPropertiesFromCursor(cursor);
            return toReturn;
        } catch (SecurityException e) {
            throw new RuntimeException(e);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(e);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } finally {
            cursor.close();
        }
    }

    /**
     * Delete the row with the given row ID
     *
     * @param modelClass the model class corresponding to the table to delete from
     * @param id the row ID of the record
     * @return true if delete was successful
     */
    public boolean delete(Class<? extends TableModel> modelClass, long id) {
        SqlTable<?> table = getTableFrom(modelClass);
        int rowsUpdated = database.delete(table.getExpression(), TableModel.ID_PROPERTY.eq(id).toRawSql(), null);
        if (rowsUpdated > 0) {
            notifyForTable(DBOperation.DELETE, null, table, id);
        }
        return rowsUpdated > 0;
    }

    /**
     * Delete all rows matching the given {@link Criterion}
     *
     * @param where the Criterion to match
     * @return the number of deleted rows
     */
    public int deleteWhere(Class<? extends TableModel> modelClass, Criterion where) {
        SqlTable<?> table = getTableFrom(modelClass);
        int rowsUpdated = database.delete(table.getExpression(), where.toRawSql(), null);
        if (rowsUpdated > 0) {
            notifyForTable(DBOperation.DELETE, null, table, TableModel.NO_ID);
        }
        return rowsUpdated;
    }

    /**
     * Executes a {@link Delete} statement.
     * <p>
     * Note: Generally speaking, you should prefer to use {@link #delete(Class, long) delete} or
     * {@link #deleteWhere(Class, Criterion) deleteWhere} for deleting database rows. This is provided as a convenience
     * in case there exists a non-ORM case where a more traditional SQL delete statement is required.
     *
     * @param delete the statement to execute
     * @return the number of rows deleted on success, -1 on failure
     */
    public int delete(Delete delete) {
        int result = database.delete(delete);
        if (result > 0) {
            notifyForTable(DBOperation.DELETE, null, delete.getTable(), TableModel.NO_ID);
        }
        return result;
    }

    /**
     * Update all rows matching the given {@link Criterion}, setting values based on the provided template model. For
     * example, this code would change all persons' names from "joe" to "bob":
     *
     * <pre>
     * Person template = new Person();
     * template.setName(&quot;bob&quot;);
     * update(Person.NAME.eq(&quot;joe&quot;), template);
     * </pre>
     *
     * @param where the criterion to match
     * @param template a model containing new values for the properties (columns) that should be updated
     * @return the number of updated rows
     */
    public int update(Criterion where, TableModel template) {
        return updateWithOnConflict(where, template, null);
    }

    /**
     * Update all rows matching the given {@link Criterion}, setting values based on the provided template model. Any
     * constraint violations will be resolved using the specified {@link ConflictAlgorithm}.
     *
     * @param where the criterion to match
     * @param template a model containing new values for the properties (columns) that should be updated
     * @param conflictAlgorithm the conflict algorithm to use
     * @return the number of updated rows
     * @see #update(Criterion, TableModel)
     */
    public int updateWithOnConflict(Criterion where, TableModel template, ConflictAlgorithm conflictAlgorithm) {
        Class<? extends TableModel> modelClass = template.getClass();
        SqlTable<?> table = getTableFrom(modelClass);
        int rowsUpdated;
        if (conflictAlgorithm == null) {
            rowsUpdated = database.update(table.getExpression(), template.getSetValues(),
                    where.toRawSql(), null);
        } else {
            rowsUpdated = database.updateWithOnConflict(table.getExpression(),
                    template.getSetValues(), where.toRawSql(), null, conflictAlgorithm.getAndroidValue());
        }
        if (rowsUpdated > 0) {
            notifyForTable(DBOperation.UPDATE, template, table, TableModel.NO_ID);
        }
        return rowsUpdated;
    }

    /**
     * Executes an {@link Update} statement.
     * <p>
     * Note: Generally speaking, you should prefer to use {@link #update(Criterion, TableModel)}
     * or {@link #updateWithOnConflict(Criterion, TableModel, ConflictAlgorithm)} for bulk database updates.
     * This is provided as a convenience in case there exists a non-ORM case where a more
     * traditional SQL update statement is required for some reason.
     *
     * @param update statement to execute
     * @return the number of rows updated on success, -1 on failure
     */
    public int update(Update update) {
        int result = database.update(update);
        if (result > 0) {
            notifyForTable(DBOperation.UPDATE, null, update.getTable(), TableModel.NO_ID);
        }
        return result;
    }

    /**
     * Save a model to the database. Creates a new row if the model does not have an ID, otherwise updates the row with
     * the corresponding row ID. If a new row is inserted, the model will have its ID set to the corresponding row ID.
     *
     * @param item the model to save
     * @return true if current the model data is stored in the database
     */
    public boolean persist(TableModel item) {
        return persistWithOnConflict(item, null);
    }

    /**
     * Save a model to the database. Creates a new row if the model does not have an ID, otherwise updates the row with
     * the corresponding row ID. If a new row is inserted, the model will have its ID set to the corresponding row ID.
     * Any constraint violations will be resolved using the specified {@link ConflictAlgorithm}.
     *
     * @param item the model to save
     * @param conflictAlgorithm the conflict algorithm to use
     * @return true if current the model data is stored in the database
     * @see #persist(TableModel)
     */
    public boolean persistWithOnConflict(TableModel item, ConflictAlgorithm conflictAlgorithm) {
        if (!item.isSaved()) {
            return insertRow(item, conflictAlgorithm);
        }
        if (!item.isModified()) {
            return true;
        }
        return updateRow(item, conflictAlgorithm);
    }

    /**
     * Save a model to the database. This method always inserts a new row and sets the ID of the model to the
     * corresponding row ID.
     *
     * @param item the model to save
     * @return true if current the model data is stored in the database
     */
    public boolean createNew(TableModel item) {
        item.setId(TableModel.NO_ID);
        return insertRow(item, null);
    }

    /**
     * Save a model to the database. This method always updates an existing row with a row ID corresponding to the
     * model's ID. If the model doesn't have an ID, or the corresponding row no longer exists in the database, this
     * will return false.
     *
     * @param item the model to save
     * @return true if current the model data is stored in the database
     */
    public boolean saveExisting(TableModel item) {
        return updateRow(item, null);
    }

    /**
     * Inserts a new row using the item's merged values into the DB.
     * <p>
     * Note: unlike {@link #createNew(TableModel)}, which will always create a new row even if an id is set on the
     * model, this method will blindly attempt to insert the primary key id value if it is provided. This may cause
     * conflicts, throw exceptions, etc. if the row id already exists, so be sure to check for such cases if you
     * expect they may happen.
     *
     * @param item the model to insert
     * @return true if success, false otherwise
     */
    protected final boolean insertRow(TableModel item) {
        return insertRow(item, null);
    }

    /**
     * Same as {@link #insertRow(TableModel)} with the ability to specify a ConflictAlgorithm for handling constraint
     * violations
     *
     * @param item the model to insert
     * @param conflictAlgorithm the conflict algorithm to use
     * @return true if success, false otherwise
     */
    protected final boolean insertRow(TableModel item, ConflictAlgorithm conflictAlgorithm) {
        Class<? extends TableModel> modelClass = item.getClass();
        SqlTable<?> table = getTableFrom(modelClass);
        long newRow;
        if (conflictAlgorithm == null) {
            newRow = database.insert(table.getExpression(),
                    TableModel.ID_PROPERTY_NAME, item.getMergedValues());
        } else {
            newRow = database.insertWithOnConflict(table.getExpression(), TableModel.ID_PROPERTY_NAME,
                    item.getMergedValues(), conflictAlgorithm.getAndroidValue());
        }
        boolean result = newRow > 0;
        if (result) {
            notifyForTable(DBOperation.INSERT, item, table, newRow);
            item.setId(newRow);
            item.markSaved();
        }
        return result;
    }

    /**
     * Update an existing row in the database using the item's setValues. The item must have the primary key id set;
     * if it does not, the method will return false.
     *
     * @param item the model to save
     * @return true if success, false otherwise
     */
    protected final boolean updateRow(TableModel item) {
        return updateRow(item, null);
    }

    /**
     * Same as {@link #updateRow(TableModel)} with the ability to specify a ConflictAlgorithm for handling constraint
     * violations
     *
     * @param item the model to save
     * @param conflictAlgorithm the conflict algorithm to use
     * @return true if success, false otherwise
     */
    protected final boolean updateRow(TableModel item, ConflictAlgorithm conflictAlgorithm) {
        if (!item.isModified()) { // nothing changed
            return true;
        }
        if (!item.isSaved()) {
            return false;
        }

        Class<? extends TableModel> modelClass = item.getClass();
        SqlTable<?> table = getTableFrom(modelClass);
        boolean result;
        if (conflictAlgorithm == null) {
            result = database.update(table.getExpression(), item.getSetValues(),
                    TableModel.ID_PROPERTY.eq(item.getId()).toRawSql(), null) > 0;
        } else {
            result = database.updateWithOnConflict(table.getExpression(), item.getSetValues(),
                    TableModel.ID_PROPERTY.eq(item.getId()).toRawSql(), null, conflictAlgorithm.getAndroidValue()) > 0;
        }
        if (result) {
            notifyForTable(DBOperation.UPDATE, item, table, item.getId());
            item.markSaved();
        }
        return result;
    }

    /**
     * Executes an {@link Insert} statement.
     * <p>
     * Note: Generally speaking, you should prefer to use {@link #persist(TableModel) persist} or
     * {@link #createNew(TableModel) createNew} for inserting database rows. This is provided as a convenience in case
     * there exists a non-ORM case where a more traditional SQL insert statement is required.
     *
     * @param insert the statement to execute
     * @return the row id of the last row inserted on success, 0 on failure
     */
    public long insert(Insert insert) {
        long result = database.insert(insert);
        if (result > TableModel.NO_ID) {
            int numInserted = insert.getNumRows();
            notifyForTable(DBOperation.INSERT, null, insert.getTable(), numInserted == 1 ? result : TableModel.NO_ID);
        }
        return result;
    }

    /**
     * Notify that content for the given uri has changed
     *
     * @param uri the Uri to notify
     * @see com.yahoo.squidb.data.AbstractDatabase#notifyChange(java.util.Collection)
     */
    public void notifyChange(Uri uri) {
        database.notifyChange(uri);
    }

    /**
     * Notify that content for the given uris has changed
     *
     * @param uris the Uris to notify
     * @see com.yahoo.squidb.data.AbstractDatabase#notifyChange(java.util.Collection)
     */
    public void notifyChange(Collection<Uri> uris) {
        database.notifyChange(uris);
    }

    // --- transaction management

    // Tracks nested transaction success or failure state. If any
    // nested transaction fails, the entire outer transaction
    // is also considered to have failed.
    private static class TransactionSuccessState {

        Deque<Boolean> nestedSuccessStack = new LinkedList<Boolean>();
        boolean outerTransactionSuccess = true;

        private void beginTransaction() {
            nestedSuccessStack.push(false);
        }

        private void setTransactionSuccessful() {
            nestedSuccessStack.pop();
            nestedSuccessStack.push(true);
        }

        private void endTransaction() {
            Boolean mostRecentTransactionSuccess = nestedSuccessStack.pop();
            if (!mostRecentTransactionSuccess) {
                outerTransactionSuccess = false;
            }
        }

        private void reset() {
            nestedSuccessStack.clear();
            outerTransactionSuccess = true;
        }
    }

    private ThreadLocal<TransactionSuccessState> transactionSuccessState = new ThreadLocal<TransactionSuccessState>() {
        protected TransactionSuccessState initialValue() {
            return new TransactionSuccessState();
        }
    };

    /**
     * Begin a transaction
     *
     * @see android.database.sqlite.SQLiteDatabase#beginTransaction()
     */
    public void beginTransaction() {
        database.beginTransaction();
        transactionSuccessState.get().beginTransaction();
    }

    /**
     * Begin a non-exclusive transaction
     *
     * @see android.database.sqlite.SQLiteDatabase#beginTransactionNonExclusive()
     */
    public void beginTransactionNonExclusive() {
        database.beginTransactionNonExclusive();
        transactionSuccessState.get().beginTransaction();
    }

    /**
     * Begin a transaction with a listener
     *
     * @param listener the transaction listener
     * @see android.database.sqlite.SQLiteDatabase#beginTransactionWithListener(android.database.sqlite.SQLiteTransactionListener)
     */
    public void beginTransactionWithListener(SQLiteTransactionListener listener) {
        database.beginTransactionWithListener(listener);
        transactionSuccessState.get().beginTransaction();
    }

    /**
     * Begin a non-exclusive transaction with a listener
     *
     * @param listener the transaction listener
     * @see android.database.sqlite.SQLiteDatabase#beginTransactionWithListenerNonExclusive(android.database.sqlite.SQLiteTransactionListener)
     */
    public void beginTransactionWithListenerNonExclusive(SQLiteTransactionListener listener) {
        database.beginTransactionWithListenerNonExclusive(listener);
        transactionSuccessState.get().beginTransaction();
    }

    /**
     * Mark the current transaction as successful
     *
     * @see android.database.sqlite.SQLiteDatabase#setTransactionSuccessful()
     */
    public void setTransactionSuccessful() {
        database.setTransactionSuccessful();
        transactionSuccessState.get().setTransactionSuccessful();
    }

    /**
     * @return true if a transaction is active
     * @see android.database.sqlite.SQLiteDatabase#inTransaction()
     */
    public boolean inTransaction() {
        return database.inTransaction();
    }

    /**
     * End the current transaction
     *
     * @see android.database.sqlite.SQLiteDatabase#endTransaction()
     */
    public void endTransaction() {
        database.endTransaction();
        TransactionSuccessState successState = transactionSuccessState.get();
        successState.endTransaction();

        if (!inTransaction()) {
            flushAccumulatedUris(uriAccumulator.get(), successState.outerTransactionSuccess);
            successState.reset();
        }
    }

    /**
     * Yield the current transaction
     *
     * @see android.database.sqlite.SQLiteDatabase#yieldIfContendedSafely()
     */
    public boolean yieldIfContendedSafely() {
        return database.yieldIfContendedSafely();
    }

    // --- helper methods

    protected <TYPE extends TableModel> SquidCursor<TYPE> fetchItemById(Class<TYPE> modelClass, long id,
            Property<?>... properties) {
        return fetchFirstItem(modelClass, TableModel.ID_PROPERTY.eq(id), properties);
    }

    protected <TYPE extends AbstractModel> SquidCursor<TYPE> fetchFirstItem(Class<TYPE> modelClass,
            Criterion criterion, Property<?>... properties) {
        return fetchFirstItem(modelClass, Query.select(properties).where(criterion));
    }

    protected <TYPE extends AbstractModel> SquidCursor<TYPE> fetchFirstItem(Class<TYPE> modelClass, Query query) {
        int beforeLimit = query.getLimit();
        SqlTable<?> beforeTable = query.getTable();
        query = query.limit(1); // If argument was frozen, we may get a new object
        SquidCursor<TYPE> cursor = query(modelClass, query);
        query.limit(beforeLimit); // Reset for user
        query.from(beforeTable); // Reset for user
        cursor.moveToFirst();
        return cursor;
    }

    /**
     * Count the number of rows matching a given {@link Criterion}. Use {@link Criterion#all} to count all rows.
     *
     * @param modelClass the model class corresponding to the table
     * @param criterion the criterion to match
     * @return the number of rows matching the given criterion
     */
    public int count(Class<? extends AbstractModel> modelClass, Criterion criterion) {
        IntegerProperty countProperty = IntegerProperty.countProperty();
        Query query = Query.select(countProperty).where(criterion);
        SquidCursor<?> cursor = query(modelClass, query);
        try {
            cursor.moveToFirst();
            return cursor.get(countProperty);
        } finally {
            cursor.close();
        }
    }

    // --- Uri notification

    private final Object uriNotifiersLock = new Object();
    private boolean uriNotificationsDisabled = false;
    private List<UriNotifier> globalNotifiers = new ArrayList<UriNotifier>();
    private Map<SqlTable<?>, List<UriNotifier>> tableNotifiers = new HashMap<SqlTable<?>, List<UriNotifier>>();

    // Using a ThreadLocal makes it easy to have one accumulator set per transaction, since
    // transactions are also associated with the thread they run on
    private ThreadLocal<Set<Uri>> uriAccumulator = new ThreadLocal<Set<Uri>>() {
        protected Set<Uri> initialValue() {
            return new HashSet<Uri>();
        }
    };

    /**
     * Register a {@link UriNotifier} to listen for database changes. The UriNotifier object will be asked to return a
     * Uri to notify whenever a table it is interested is modified.
     *
     * @param notifier the UriNotifier to register
     */
    public void registerUriNotifier(UriNotifier notifier) {
        if (notifier == null) {
            return;
        }
        synchronized (uriNotifiersLock) {
            List<SqlTable<?>> tables = notifier.whichTables();
            if (tables == null || tables.isEmpty()) {
                globalNotifiers.add(notifier);
            } else {
                for (SqlTable<?> table : tables) {
                    List<UriNotifier> notifiersForTable = tableNotifiers.get(table);
                    if (notifiersForTable == null) {
                        notifiersForTable = new ArrayList<UriNotifier>();
                        tableNotifiers.put(table, notifiersForTable);
                    }
                    notifiersForTable.add(notifier);
                }
            }
        }
    }

    /**
     * Unregister a {@link UriNotifier} previously registered by {@link #registerUriNotifier(UriNotifier)}
     *
     * @param notifier the UriNotifier to unregister
     */
    public void unregisterUriNotifier(UriNotifier notifier) {
        if (notifier == null) {
            return;
        }
        synchronized (uriNotifiersLock) {
            List<SqlTable<?>> tables = notifier.whichTables();
            if (tables == null || tables.isEmpty()) {
                globalNotifiers.remove(notifier);
            } else {
                for (SqlTable<?> table : tables) {
                    List<UriNotifier> notifiersForTable = tableNotifiers.get(table);
                    if (notifiersForTable != null) {
                        notifiersForTable.remove(notifier);
                    }
                }
            }
        }
    }

    /**
     * Unregister all {@link UriNotifier}s previously registered by {@link #registerUriNotifier(UriNotifier)}
     */
    public void unregisterAllUriNotifiers() {
        synchronized (uriNotifiersLock) {
            globalNotifiers.clear();
            tableNotifiers.clear();
        }
    }

    /**
     * Set a flag to disable Uri notifications. No Uris will be notified (or accumulated during transactions) after
     * this method is called, until {@link #enableUriNotifications()} is called to re-enable notifications.
     */
    public void disableUriNotifications() {
        uriNotificationsDisabled = true;
    }

    /**
     * Re-enables Uri notifications after a call to {@link #disableUriNotifications()}
     */
    public void enableUriNotifications() {
        uriNotificationsDisabled = false;
    }

    private void notifyForTable(DBOperation op, AbstractModel modelValues, SqlTable<?> table, long rowId) {
        if (uriNotificationsDisabled) {
            return;
        }
        Set<Uri> accumulatorSet = uriAccumulator.get();
        synchronized (uriNotifiersLock) {
            accumulateUrisToNotify(globalNotifiers, accumulatorSet, op, modelValues, table, rowId);
            accumulateUrisToNotify(tableNotifiers.get(table), accumulatorSet, op, modelValues, table, rowId);
        }
        if (!inTransaction()) {
            flushAccumulatedUris(accumulatorSet, true);
        }
    }

    private void accumulateUrisToNotify(List<UriNotifier> notifiers, Set<Uri> accumulatorSet, DBOperation op,
            AbstractModel modelValues, SqlTable<?> table, long rowId) {
        if (notifiers != null) {
            for (UriNotifier notifier : notifiers) {
                notifier.addUrisToNotify(accumulatorSet, table, getDatabaseName(), op, modelValues, rowId);
            }
        }
    }

    private void flushAccumulatedUris(Set<Uri> urisToNotify, boolean transactionSuccess) {
        if (!urisToNotify.isEmpty()) {
            if (transactionSuccess && !uriNotificationsDisabled) {
                notifyChange(urisToNotify);
            }
            urisToNotify.clear();
        }
    }
}
