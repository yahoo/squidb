/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.data;

import com.yahoo.squidb.Beta;
import com.yahoo.squidb.sql.CompiledStatement;
import com.yahoo.squidb.sql.Criterion;
import com.yahoo.squidb.sql.Delete;
import com.yahoo.squidb.sql.Index;
import com.yahoo.squidb.sql.Insert;
import com.yahoo.squidb.sql.Property;
import com.yahoo.squidb.sql.Property.PropertyVisitor;
import com.yahoo.squidb.sql.Query;
import com.yahoo.squidb.sql.SqlStatement;
import com.yahoo.squidb.sql.SqlTable;
import com.yahoo.squidb.sql.SqlUtils;
import com.yahoo.squidb.sql.Table;
import com.yahoo.squidb.sql.TableStatement;
import com.yahoo.squidb.sql.Update;
import com.yahoo.squidb.sql.View;
import com.yahoo.squidb.sql.VirtualTable;
import com.yahoo.squidb.utility.Logger;
import com.yahoo.squidb.utility.VersionCode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * SquidDatabase is a database abstraction which wraps a SQLite database.
 * <p>
 * Use this class to control the lifecycle of your database where you would normally use a
 * {@link android.database.sqlite.SQLiteOpenHelper}. The first call to a read or write operation will open the database.
 * You can close it again using {@link #close()}.
 * <p>
 * SquidDatabase provides typesafe reads and writes using model classes. For example, rather than using rawQuery to
 * get a Cursor, use {@link #query(Class, Query)}.
 * <p>
 * By convention, methods beginning with "try" (e.g. {@link #tryCreateTable(Table) tryCreateTable}) return true
 * if the operation succeeded and false if it failed for any reason. If it fails, there will also be a call to
 * {@link #onError(String, Throwable) onError}.
 * <p>
 *
 * As a convenience, when calling the {@link #query(Class, Query) query} and {@link #fetchByQuery(Class, Query)
 * fetchByQuery} methods, if the <code>query</code> argument does not have a FROM clause, the table or view to select
 * from will be inferred from the provided <code>modelClass</code> argument (if possible). This allows for invocations
 * where {@link Query#from(com.yahoo.squidb.sql.SqlTable) Query.from} is never explicitly called:
 *
 * <pre>
 * SquidCursor&lt;Person&gt; cursor =
 *         db.query(Person.class, Query.select().orderBy(Person.NAME.asc()));
 * </pre>
 *
 * By convention, the <code>fetch...</code> methods return a single model instance corresponding to the first record
 * found, or null if no records are found for that particular form of fetch.
 */
public abstract class SquidDatabase {

    /**
     * @return the database name
     */
    public abstract String getName();

    /**
     * @return the database version
     */
    protected abstract int getVersion();

    /**
     * @return all {@link Table Tables} and {@link VirtualTable VirtualTables} and that should be created when the
     * database is created
     */
    protected abstract Table[] getTables();

    /**
     * @return all {@link View Views} that should be created when the database is created. Views will be created after
     * all Tables have been created.
     */
    protected View[] getViews() {
        return null;
    }

    /**
     * @return all {@link Index Indexes} that should be created when the database is created. Indexes will be created
     * after Tables and Views have been created.
     */
    protected Index[] getIndexes() {
        return null;
    }

    /**
     * Called after the database has been created. At this time, all {@link Table Tables} and {@link
     * VirtualTable VirtualTables} returned from {@link #getTables()}, all {@link View Views} from {@link #getViews()},
     * and all {@link Index Indexes} from {@link #getIndexes()} will have been created. Any additional database setup
     * should be done here, e.g. creating other views, indexes, triggers, or inserting data.
     *
     * @param db the {@link ISQLiteDatabase} being created
     */
    protected void onTablesCreated(ISQLiteDatabase db) {
    }

    /**
     * Called when the database should be upgraded from one version to another. The most common pattern to use is a
     * fall-through switch statement with calls to the tryAdd/Create/Drop methods:
     *
     * <pre>
     * switch(oldVersion) {
     * case 1:
     *     tryAddColumn(MyModel.NEW_COL_1);
     * case 2:
     *     tryCreateTable(MyNewModel.TABLE);
     * </pre>
     *
     * @param db the {@link ISQLiteDatabase} being upgraded
     * @param oldVersion the current database version
     * @param newVersion the database version being upgraded to
     * @return true if the upgrade was handled successfully, false otherwise
     */
    protected abstract boolean onUpgrade(ISQLiteDatabase db, int oldVersion, int newVersion);

    /**
     * Called when the database should be downgraded from one version to another
     *
     * @param db the {@link ISQLiteDatabase} being upgraded
     * @param oldVersion the current database version
     * @param newVersion the database version being downgraded to
     * @return true if the downgrade was handled successfully, false otherwise. The default implementation returns true.
     */
    protected boolean onDowngrade(ISQLiteDatabase db, int oldVersion, int newVersion) {
        return true;
    }

    /**
     * Called to notify of a failure in {@link #onUpgrade(ISQLiteDatabase, int, int) onUpgrade()} or
     * {@link #onDowngrade(ISQLiteDatabase, int, int) onDowngrade()}, either because it returned false or because
     * an unexpected exception occurred. Subclasses can take drastic corrective action here, e.g. recreating the
     * database with {@link #recreate()}. The default implementation throws an exception.
     * <p>
     * Note that taking no action here leaves the database in whatever state it was in when the error occurred, which
     * can result in unexpected errors if callers are allowed to invoke further operations on the database.
     *
     * @param failure details about the upgrade or downgrade that failed
     */
    protected void onMigrationFailed(MigrationFailedException failure) {
        throw failure;
    }

    /**
     * Called when the database connection is being configured, to enable features such as write-ahead logging or
     * foreign key support.
     *
     * This method may be called at different points in the database lifecycle depending on the environment. When using
     * a custom SQLite build with the squidb-sqlite-bindings project, or when running on Android API >= 16, it is
     * called before {@link #onTablesCreated(ISQLiteDatabase) onTablesCreated},
     * {@link #onUpgrade(ISQLiteDatabase, int, int) onUpgrade},
     * {@link #onDowngrade(ISQLiteDatabase, int, int) onDowngrade},
     * and {@link #onOpen(ISQLiteDatabase) onOpen}. If it is running on stock Android SQLite and API < 16, it
     * is called immediately before onOpen but after the other callbacks. The discrepancy is because onConfigure was
     * only introduced as a callback in API 16, but the ordering should not matter much for most use cases.
     * <p>
     *
     * This method should only call methods that configure the parameters of the database connection, such as
     * {@link ISQLiteDatabase#enableWriteAheadLogging}, {@link ISQLiteDatabase#setForeignKeyConstraintsEnabled},
     * {@link ISQLiteDatabase#setMaximumSize}, or executing PRAGMA statements.
     *
     * @param db the {@link ISQLiteDatabase} being configured
     */
    protected void onConfigure(ISQLiteDatabase db) {
    }

    /**
     * Called when the database has been opened. This method is called after the database connection has been
     * configured and after the database schema has been created, upgraded, or downgraded as necessary.
     *
     * @param db the {@link ISQLiteDatabase} being opened
     */
    protected void onOpen(ISQLiteDatabase db) {
    }

    /**
     * Called when an error occurs. This is primarily for clients to log notable errors, not for taking corrective
     * action on them. The default implementation prints a warning log.
     *
     * @param message an error message
     * @param error the error that was encountered
     */
    protected void onError(String message, Throwable error) {
        Logger.e(Logger.LOG_TAG, getClass().getSimpleName() + " -- " + message, error);
    }

    // --- internal implementation

    private static final int STRING_BUILDER_INITIAL_CAPACITY = 128;

    private SquidDatabase attachedTo = null;
    private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();

    /**
     * SQLiteOpenHelperWrapper that takes care of database operations
     */
    private ISQLiteOpenHelper helper = null;

    /**
     * Internal pointer to open database. Hides the fact that there is a database and a wrapper by making a single
     * monolithic interface
     */
    private ISQLiteDatabase database = null;

    /**
     * Cached version code
     */
    private VersionCode sqliteVersion = null;

    /**
     * Map of class objects to corresponding tables
     */
    private Map<Class<? extends AbstractModel>, SqlTable<?>> tableMap
            = new HashMap<Class<? extends AbstractModel>, SqlTable<?>>();

    private boolean isInMigration;

    /**
     * Create a new SquidDatabase
     */
    public SquidDatabase() {
        registerTableModels(getTables());
        registerTableModels(getViews());
    }

    private <T extends SqlTable<?>> void registerTableModels(T[] tables) {
        if (tables != null) {
            for (SqlTable<?> table : tables) {
                if (table.getModelClass() != null && !tableMap.containsKey(table.getModelClass())) {
                    tableMap.put(table.getModelClass(), table);
                }
            }
        }
    }

    private synchronized ISQLiteOpenHelper getOpenHelper() {
        if (helper == null) {
            helper = createOpenHelper(getName(), new OpenHelperDelegate(), getVersion());
        }
        return helper;
    }

    protected abstract ISQLiteOpenHelper createOpenHelper(String databaseName,
            OpenHelperDelegate delegate, int version);

    /**
     * @return the path to the underlying database file.
     */
    public String getDatabasePath() {
        return getOpenHelper().getDatabasePath();
    }

    /**
     * Return the {@link SqlTable} corresponding to the specified model type
     *
     * @param modelClass the model class
     * @return the corresponding data source for the model. May be a table, view, or subquery
     * @throws UnsupportedOperationException if the model class is unknown to this database
     */
    protected final SqlTable<?> getSqlTable(Class<? extends AbstractModel> modelClass) {
        Class<?> type = modelClass;
        SqlTable<?> table;
        //noinspection SuspiciousMethodCalls
        while ((table = tableMap.get(type)) == null && type != AbstractModel.class && type != Object.class) {
            type = type.getSuperclass();
        }
        if (table != null) {
            return table;
        }
        throw new UnsupportedOperationException("Unknown model class " + modelClass);
    }

    /**
     * Return the {@link Table} corresponding to the specified TableModel class
     *
     * @param modelClass the model class
     * @return the corresponding table for the model
     * @throws UnsupportedOperationException if the model class is unknown to this database
     */
    protected final Table getTable(Class<? extends TableModel> modelClass) {
        return (Table) getSqlTable(modelClass);
    }

    /**
     * Gets the underlying SQLiteDatabaseWrapper instance. Most users should not need to call this. If you call this
     * from your AbstractDatabase subclass with the intention of executing SQL, you should wrap the calls with a lock,
     * probably the non-exclusive one:
     *
     * <pre>
     * public void execSql(String sql) {
     *     acquireNonExclusiveLock();
     *     try {
     *         getDatabase().execSQL(sql);
     *     } finally {
     *         releaseNonExclusiveLock();
     *     }
     * }
     * </pre>
     *
     * You only need to acquire the exclusive lock if you truly need exclusive access to the database connection.
     *
     * @return the underlying {@link ISQLiteDatabase}, which will be opened if it is not yet opened
     * @see #acquireExclusiveLock()
     * @see #acquireNonExclusiveLock()
     */
    protected synchronized final ISQLiteDatabase getDatabase() {
        if (database == null) {
            openForWriting();
        }
        return database;
    }

    /**
     * Attaches another database to this database using the SQLite ATTACH command. This locks the other database
     * exclusively; you must call {@link #detachDatabase(SquidDatabase)} when you are done, otherwise the attached
     * database will not be unlocked.
     * <p>
     * This method will throw an exception if either database is already attached to another database, or if either
     * database has an open transaction on the current thread.
     * <p>
     * Note that Android disables write-ahead logging when attaching a database. On Jelly Bean (API 16) and later, if
     * this database has write-ahead logging enabled and it has any open transactions on other threads, this
     * method <b>will block</b> until those transactions complete before attaching the database.
     *
     * @param other the database to attach to this one
     * @return the alias used to attach the database. This can be used to qualify tables using
     * {@link Table#qualifiedFromDatabase(String)}. If the attach command fails for any reason not mentioned above,
     * null is returned.
     * @throws IllegalStateException if this database is already attached to another database
     * @throws IllegalArgumentException if the other database is already attached to another database
     * @throws IllegalStateException if either database has an open transaction on the current thread
     */
    @Beta
    public final String attachDatabase(SquidDatabase other) {
        if (attachedTo != null) {
            throw new IllegalStateException("Can't attach a database to a database that is itself attached");
        }
        if (inTransaction()) {
            throw new IllegalStateException("Can't attach a database while in a transaction on the current thread");
        }

        // Some platforms need to wait for transactions to finish,
        // so we acquire an exclusive lock before attaching
        acquireExclusiveLock();
        try {
            return other.attachTo(this);
        } finally {
            releaseExclusiveLock();
        }
    }

    /**
     * Detaches a database previously attached with {@link #attachDatabase(SquidDatabase)}
     *
     * @return true if the other database was successfully detached
     */
    @Beta
    public final boolean detachDatabase(SquidDatabase other) {
        if (other.attachedTo != this) {
            throw new IllegalArgumentException("Database " + other.getName() + " is not attached to " + getName());
        }

        return other.detachFrom(this);
    }

    private String attachTo(SquidDatabase attachTo) {
        if (attachedTo != null) {
            throw new IllegalArgumentException(
                    "Database " + getName() + " is already attached to " + attachedTo.getName());
        }
        if (inTransaction()) {
            throw new IllegalStateException(
                    "Cannot attach database " + getName() + " to " + attachTo.getName() + " -- " + getName()
                            + " is in a transaction on the calling thread");
        }

        acquireExclusiveLock();

        String attachedAs = getAttachedName();
        if (!attachTo.tryExecSql("ATTACH '" + getDatabasePath() + "' AS '" + attachedAs + "'")) {
            releaseExclusiveLock(); // Failed
            return null;
        } else {
            attachedTo = attachTo;
            return attachedAs;
        }
    }

    private boolean detachFrom(SquidDatabase detachFrom) {
        if (detachFrom.tryExecSql("DETACH '" + getAttachedName() + "'")) {
            attachedTo = null;
            releaseExclusiveLock();
            return true;
        }
        return false;
    }

    private String getAttachedName() {
        return getName().replace('.', '_');
    }

    /**
     * Open the database for writing.
     */
    private void openForWriting() {
        boolean performRecreate = false;
        try {
            setDatabase(getOpenHelper().openForWriting());
        } catch (RecreateDuringMigrationException recreate) {
            performRecreate = true;
        } catch (MigrationFailedException fail) {
            onError(fail.getMessage(), fail);
            onMigrationFailed(fail);
        } catch (RuntimeException e) {
            onError("Failed to open database: " + getName(), e);
            throw e;
        }

        if (performRecreate) {
            recreate();
        }
    }

    /**
     * @return true if a connection to the {@link ISQLiteDatabase} is open, false otherwise
     */
    public synchronized final boolean isOpen() {
        return database != null && database.isOpen();
    }

    /**
     * Close the database if it has been opened previously
     */
    public synchronized final void close() {
        if (isOpen()) {
            database.close();
        }
        getOpenHelper().close();
        setDatabase(null);
    }

    /**
     * Clear all data in the database.
     * <p>
     * WARNING: Any open database resources will be abruptly closed. Do not call this method if other threads may be
     * accessing the database. The existing database file will be deleted and all data will be lost.
     */
    public synchronized final void clear() {
        close();
        getOpenHelper().deleteDatabase();
    }

    /**
     * Clears the database and recreates an empty version of it.
     * <p>
     * WARNING: Any open connections to the database will be abruptly closed. Do not call this method if other threads
     * may be accessing the database.
     *
     * @see #clear()
     */
    public synchronized final void recreate() {
        if (isInMigration) {
            throw new RecreateDuringMigrationException();
        } else {
            clear();
            getDatabase();
        }
    }

    /**
     * @return a human-readable database name for debugging
     */
    @Override
    public String toString() {
        return "DB:" + getName();
    }

    // --- database wrapper

    /**
     * Execute a raw sqlite query. This method takes an Object[] for the arguments because Android's default behavior
     * of binding all arguments as strings can have unexpected bugs, particularly when working with functions. For
     * example:
     *
     * <pre>
     * select * from t where _id = '1'; // Returns the first row
     * select * from t where abs(_id) = '1'; // Always returns empty set
     * </pre>
     *
     * To eliminate this class of bugs, we bind all arguments as their native types, not as strings. Any object in the
     * array that is not a basic type (Number, String, Boolean, etc.) will be converted to a sanitized string before
     * binding.
     *
     * @param sql a sql statement
     * @param sqlArgs arguments to bind to the sql statement
     * @return a {@link ICursor} containing results of the query
     */
    public ICursor rawQuery(String sql, Object[] sqlArgs) {
        acquireNonExclusiveLock();
        try {
            return getDatabase().rawQuery(sql, sqlArgs);
        } finally {
            releaseNonExclusiveLock();
        }
    }

    // For use only when validating queries
    private void compileStatement(String sql) {
        acquireNonExclusiveLock();
        try {
            getDatabase().ensureSqlCompiles(sql);
        } finally {
            releaseNonExclusiveLock();
        }
    }

    /**
     * Execute a SQL {@link com.yahoo.squidb.sql.Insert} statement
     *
     * @return the row id of the last row inserted on success, -1 on failure
     */
    private long insertInternal(Insert insert) {
        CompiledStatement compiled = insert.compile(getSqliteVersion());
        acquireNonExclusiveLock();
        try {
            return getDatabase().executeInsert(compiled.sql, compiled.sqlArgs);
        } finally {
            releaseNonExclusiveLock();
        }
    }

    /**
     * Execute a SQL {@link com.yahoo.squidb.sql.Delete} statement
     *
     * @return the number of rows deleted on success, -1 on failure
     */
    private int deleteInternal(Delete delete) {
        CompiledStatement compiled = delete.compile(getSqliteVersion());
        acquireNonExclusiveLock();
        try {
            return getDatabase().executeUpdateDelete(compiled.sql, compiled.sqlArgs);
        } finally {
            releaseNonExclusiveLock();
        }
    }

    /**
     * Execute a SQL {@link com.yahoo.squidb.sql.Update} statement
     *
     * @return the number of rows updated on success, -1 on failure
     */
    private int updateInternal(Update update) {
        CompiledStatement compiled = update.compile(getSqliteVersion());
        acquireNonExclusiveLock();
        try {
            return getDatabase().executeUpdateDelete(compiled.sql, compiled.sqlArgs);
        } finally {
            releaseNonExclusiveLock();
        }
    }

    // --- transaction management

    /**
     * Begin a transaction. This acquires a non-exclusive lock.
     *
     * @see #acquireNonExclusiveLock()
     * @see ISQLiteDatabase#beginTransaction()
     */
    public void beginTransaction() {
        acquireNonExclusiveLock();
        getDatabase().beginTransaction();
        transactionSuccessState.get().beginTransaction();
    }

    /**
     * Begin a non-exclusive transaction. This acquires a non-exclusive lock.
     *
     * @see #acquireNonExclusiveLock()
     * @see ISQLiteDatabase#beginTransactionNonExclusive()
     */
    public void beginTransactionNonExclusive() {
        acquireNonExclusiveLock();
        getDatabase().beginTransactionNonExclusive();
        transactionSuccessState.get().beginTransaction();
    }

    /**
     * Begin a transaction with a listener. This acquires a non-exclusive lock.
     *
     * @param listener the transaction listener
     * @see #acquireNonExclusiveLock()
     * @see ISQLiteDatabase#beginTransactionWithListener(SquidTransactionListener)
     */
    public void beginTransactionWithListener(SquidTransactionListener listener) {
        acquireNonExclusiveLock();
        getDatabase().beginTransactionWithListener(listener);
        transactionSuccessState.get().beginTransaction();
    }

    /**
     * Begin a non-exclusive transaction with a listener. This acquires a non-exclusive lock.
     *
     * @param listener the transaction listener
     * @see #acquireNonExclusiveLock()
     * @see ISQLiteDatabase#beginTransactionWithListenerNonExclusive(SquidTransactionListener)
     */
    public void beginTransactionWithListenerNonExclusive(SquidTransactionListener listener) {
        acquireNonExclusiveLock();
        getDatabase().beginTransactionWithListenerNonExclusive(listener);
        transactionSuccessState.get().beginTransaction();
    }

    /**
     * Mark the current transaction as successful
     *
     * @see ISQLiteDatabase#setTransactionSuccessful()
     */
    public void setTransactionSuccessful() {
        getDatabase().setTransactionSuccessful();
        transactionSuccessState.get().setTransactionSuccessful();
    }

    /**
     * @return true if a transaction is active
     * @see ISQLiteDatabase#inTransaction()
     */
    public synchronized boolean inTransaction() {
        return database != null && database.inTransaction();
    }

    /**
     * End the current transaction
     *
     * @see ISQLiteDatabase#endTransaction()
     */
    public void endTransaction() {
        getDatabase().endTransaction();
        releaseNonExclusiveLock();

        TransactionSuccessState successState = transactionSuccessState.get();
        successState.endTransaction();

        if (!inTransaction()) {
            flushAccumulatedNotifications(successState.outerTransactionSuccess);
            successState.reset();
        }
    }

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
     * Yield the current transaction
     *
     * @return true if the transaction was yielded
     * @see ISQLiteDatabase#yieldIfContendedSafely()
     */
    public boolean yieldIfContendedSafely() {
        return getDatabase().yieldIfContendedSafely();
    }

    /**
     * Yield the current transaction
     *
     * @param sleepAfterYieldDelay milliseconds to sleep before restarting the transaction
     * (if it was yielded successfully)
     * @return true if the transaction was yielded
     */
    public boolean yieldIfContendedSafely(long sleepAfterYieldDelay) {
        return getDatabase().yieldIfContendedSafely(sleepAfterYieldDelay);
    }

    /**
     * Acquires an exclusive lock on the database. This is semantically similar to acquiring a write lock in a {@link
     * java.util.concurrent.locks.ReadWriteLock ReadWriteLock} but it is not generally necessary for protecting actual
     * database writes--it's only necessary when exclusive use of the database connection is required (e.g. while the
     * database is attached to another database).
     * <p>
     * Only one thread can hold an exclusive lock at a time. Calling this while on a thread that already holds a non-
     * exclusive lock is an error and will deadlock! We will throw an exception if this method is called while the
     * calling thread is in a transaction. Otherwise, this method will block until all non-exclusive locks
     * acquired with {@link #acquireNonExclusiveLock()} have been released, but will prevent any new non-exclusive
     * locks from being acquired while it blocks.
     */
    @Beta
    protected void acquireExclusiveLock() {
        if (inTransaction()) {
            throw new IllegalStateException(
                    "Can't acquire an exclusive lock when the calling thread is in a transaction");
        }
        readWriteLock.writeLock().lock();
    }

    /**
     * Release the exclusive lock acquired by {@link #acquireExclusiveLock()}
     */
    @Beta
    protected void releaseExclusiveLock() {
        readWriteLock.writeLock().unlock();
    }

    /**
     * Acquire a non-exclusive lock on the database. This is semantically similar to acquiring a read lock in a {@link
     * java.util.concurrent.locks.ReadWriteLock ReadWriteLock} but may also be used in most cases to protect database
     * writes (see {@link #acquireExclusiveLock()} for why this is true). This will block if the exclusive lock is held
     * by some other thread. Many threads can hold non-exclusive locks as long as no thread holds the exclusive lock.
     */
    @Beta
    protected void acquireNonExclusiveLock() {
        readWriteLock.readLock().lock();
    }

    /**
     * Releases a non-exclusive lock acquired with {@link #acquireNonExclusiveLock()}
     */
    @Beta
    protected void releaseNonExclusiveLock() {
        readWriteLock.readLock().unlock();
    }

    // --- helper classes

    /**
     * Delegate class passed to a {@link ISQLiteOpenHelper} instance that allows the SQLiteOpenHelperWrapper to call back
     * into its owning SquidDatabase after the database has been created or opened.
     */
    public final class OpenHelperDelegate {

        private OpenHelperDelegate() {
            // No public instantiation
        }

        /**
         * Called to create the database tables
         */
        public void onCreate(ISQLiteDatabase db) {
            setDatabase(db);
            StringBuilder sql = new StringBuilder(STRING_BUILDER_INITIAL_CAPACITY);
            SqlConstructorVisitor sqlVisitor = new SqlConstructorVisitor();

            // create tables
            Table[] tables = getTables();
            if (tables != null) {
                for (Table table : tables) {
                    table.appendCreateTableSql(getSqliteVersion(), sql, sqlVisitor);
                    db.execSQL(sql.toString());
                    sql.setLength(0);
                }
            }

            View[] views = getViews();
            if (views != null) {
                for (View view : views) {
                    view.createViewSql(getSqliteVersion(), sql);
                    db.execSQL(sql.toString());
                    sql.setLength(0);
                }
            }

            Index[] indexes = getIndexes();
            if (indexes != null) {
                for (Index idx : indexes) {
                    tryCreateIndex(idx);
                }
            }

            // post-table-creation
            SquidDatabase.this.onTablesCreated(db);
        }

        /**
         * Called to upgrade the database to a new version
         */
        public void onUpgrade(ISQLiteDatabase db, int oldVersion, int newVersion) {
            setDatabase(db);
            boolean success = false;
            Throwable thrown = null;
            isInMigration = true;
            try {
                success = SquidDatabase.this.onUpgrade(db, oldVersion, newVersion);
            } catch (Throwable t) {
                thrown = t;
                success = false;
            } finally {
                isInMigration = false;
            }

            if (thrown instanceof RecreateDuringMigrationException) {
                throw (RecreateDuringMigrationException) thrown;
            } else if (thrown instanceof MigrationFailedException) {
                throw (MigrationFailedException) thrown;
            } else if (!success) {
                throw new MigrationFailedException(getName(), oldVersion, newVersion, thrown);
            }
        }

        /**
         * Called to downgrade the database to an older version
         */
        public void onDowngrade(ISQLiteDatabase db, int oldVersion, int newVersion) {
            setDatabase(db);
            boolean success = false;
            Throwable thrown = null;
            isInMigration = true;
            try {
                success = SquidDatabase.this.onDowngrade(db, oldVersion, newVersion);
            } catch (Throwable t) {
                thrown = t;
                success = false;
            } finally {
                isInMigration = false;
            }

            if (thrown instanceof RecreateDuringMigrationException) {
                throw (RecreateDuringMigrationException) thrown;
            } else if (thrown instanceof MigrationFailedException) {
                throw (MigrationFailedException) thrown;
            } else if (!success) {
                throw new MigrationFailedException(getName(), oldVersion, newVersion, thrown);
            }
        }

        public void onConfigure(ISQLiteDatabase db) {
            setDatabase(db);
            SquidDatabase.this.onConfigure(db);
        }

        public void onOpen(ISQLiteDatabase db) {
            setDatabase(db);
            SquidDatabase.this.onOpen(db);
        }
    }

    private synchronized void setDatabase(ISQLiteDatabase db) {
        // If we're already holding a reference to the same object, don't need to update or recalculate the version
        if (database != null && db != null && db.getWrappedObject() == database.getWrappedObject()) {
            return;
        }
        database = db;
        sqliteVersion = database != null ? readSqliteVersion() : null;
    }

    // --- utility methods

    /**
     * Add a column to a table by specifying the corresponding {@link Property}
     *
     * @param property the Property associated with the column to add
     * @return true if the statement executed without error, false otherwise
     */
    protected boolean tryAddColumn(Property<?> property) {
        if (!(property.table instanceof Table)) {
            throw new IllegalArgumentException("Can't alter table: property does not belong to a Table");
        }
        SqlConstructorVisitor visitor = new SqlConstructorVisitor();
        StringBuilder sql = new StringBuilder(STRING_BUILDER_INITIAL_CAPACITY);
        sql.append("ALTER TABLE ").append(property.table.getExpression()).append(" ADD ");
        property.accept(visitor, sql);
        return tryExecSql(sql.toString());
    }

    /**
     * Create a new {@link Table} or {@link VirtualTable} in the database
     *
     * @param table the Table or VirtualTable to create
     * @return true if the statement executed without error, false otherwise
     */
    protected boolean tryCreateTable(Table table) {
        SqlConstructorVisitor sqlVisitor = new SqlConstructorVisitor();
        StringBuilder sql = new StringBuilder(STRING_BUILDER_INITIAL_CAPACITY);
        table.appendCreateTableSql(getSqliteVersion(), sql, sqlVisitor);
        return tryExecSql(sql.toString());
    }

    /**
     * Drop a {@link Table} or {@link VirtualTable} in the database if it exists
     *
     * @param table the Table or VirtualTable to drop
     * @return true if the statement executed without error, false otherwise
     */
    protected boolean tryDropTable(Table table) {
        return tryExecSql("DROP TABLE IF EXISTS " + table.getExpression());
    }

    /**
     * Create a new {@link View} in the database
     *
     * @param view the View to create
     * @return true if the statement executed without error, false otherwise
     * @see com.yahoo.squidb.sql.View#fromQuery(com.yahoo.squidb.sql.Query, String)
     * @see com.yahoo.squidb.sql.View#temporaryFromQuery(com.yahoo.squidb.sql.Query, String)
     */
    public boolean tryCreateView(View view) {
        StringBuilder sql = new StringBuilder(STRING_BUILDER_INITIAL_CAPACITY);
        view.createViewSql(getSqliteVersion(), sql);
        return tryExecSql(sql.toString());
    }

    /**
     * Drop a {@link View} in the database if it exists
     *
     * @param view the View to drop
     * @return true if the statement executed without error, false otherwise
     */
    public boolean tryDropView(View view) {
        return tryExecSql("DROP VIEW IF EXISTS " + view.getExpression());
    }

    /**
     * Create a new {@link Index} in the database
     *
     * @param index the Index to create
     * @return true if the statement executed without error, false otherwise
     * @see com.yahoo.squidb.sql.Table#index(String, com.yahoo.squidb.sql.Property[])
     * @see com.yahoo.squidb.sql.Table#uniqueIndex(String, com.yahoo.squidb.sql.Property[])
     */
    protected boolean tryCreateIndex(Index index) {
        return tryCreateIndex(index.getName(), index.getTable(), index.isUnique(), index.getProperties());
    }

    /**
     * Create a new {@link Index} in the database
     *
     * @param indexName name for the Index
     * @param table the table to create the index on
     * @param unique true if the index is a unique index on the specified columns
     * @param properties the columns to create the index on
     * @return true if the statement executed without error, false otherwise
     */
    protected boolean tryCreateIndex(String indexName, Table table, boolean unique, Property<?>... properties) {
        if (properties == null || properties.length == 0) {
            onError(String.format("Cannot create index %s: no properties specified", indexName), null);
            return false;
        }
        StringBuilder sql = new StringBuilder(STRING_BUILDER_INITIAL_CAPACITY);
        sql.append("CREATE ");
        if (unique) {
            sql.append("UNIQUE ");
        }
        sql.append("INDEX IF NOT EXISTS ").append(indexName).append(" ON ").append(table.getExpression())
                .append("(");
        for (Property<?> p : properties) {
            sql.append(p.getName()).append(",");
        }
        sql.deleteCharAt(sql.length() - 1);
        sql.append(")");
        return tryExecSql(sql.toString());
    }

    /**
     * Drop an {@link Index} if it exists
     *
     * @param index the Index to drop
     * @return true if the statement executed without error, false otherwise
     */
    protected boolean tryDropIndex(Index index) {
        return tryDropIndex(index.getName());
    }

    /**
     * Drop an {@link Index} if it exists
     *
     * @param indexName the name of the Index to drop
     * @return true if the statement executed without error, false otherwise
     */
    protected boolean tryDropIndex(String indexName) {
        return tryExecSql("DROP INDEX IF EXISTS " + indexName);
    }

    /**
     * Execute a {@link SqlStatement}
     *
     * @param statement the statement to execute
     * @return true if the statement executed without error, false otherwise
     */
    public boolean tryExecStatement(SqlStatement statement) {
        CompiledStatement compiled = statement.compile(getSqliteVersion());
        return tryExecSql(compiled.sql, compiled.sqlArgs);
    }

    /**
     * Execute a raw SQL statement
     *
     * @param sql the statement to execute
     * @return true if the statement executed without an error
     * @see ISQLiteDatabase#execSQL(String)
     */
    public boolean tryExecSql(String sql) {
        acquireNonExclusiveLock();
        try {
            getDatabase().execSQL(sql);
            return true;
        } catch (SQLExceptionWrapper e) {
            onError("Failed to execute statement: " + sql, e);
            return false;
        } finally {
            releaseNonExclusiveLock();
        }
    }

    /**
     * Execute a raw SQL statement
     *
     * @param sql the statement to execute
     * @throws SQLExceptionWrapper if there is an error parsing the SQL or some other error
     * @see ISQLiteDatabase#execSQL(String)
     */
    public void execSqlOrThrow(String sql) throws SQLExceptionWrapper {
        acquireNonExclusiveLock();
        try {
            getDatabase().execSQL(sql);
        } finally {
            releaseNonExclusiveLock();
        }
    }

    /**
     * Execute a raw SQL statement with optional arguments. The sql string may contain '?' placeholders for the
     * arguments.
     *
     * @param sql the statement to execute
     * @param bindArgs the arguments to bind to the statement
     * @return true if the statement executed without an error
     * @see ISQLiteDatabase#execSQL(String, Object[])
     */
    public boolean tryExecSql(String sql, Object[] bindArgs) {
        acquireNonExclusiveLock();
        try {
            getDatabase().execSQL(sql, bindArgs);
            return true;
        } catch (SQLExceptionWrapper e) {
            onError("Failed to execute statement: " + sql, e);
            return false;
        } finally {
            releaseNonExclusiveLock();
        }
    }

    /**
     * Execute a raw SQL statement with optional arguments. The sql string may contain '?' placeholders for the
     * arguments.
     *
     * @param sql the statement to execute
     * @param bindArgs the arguments to bind to the statement
     * @throws SQLExceptionWrapper if there is an error parsing the SQL or some other error
     * @see ISQLiteDatabase#execSQL(String, Object[])
     */
    public void execSqlOrThrow(String sql, Object[] bindArgs) throws SQLExceptionWrapper {
        acquireNonExclusiveLock();
        try {
            getDatabase().execSQL(sql, bindArgs);
        } finally {
            releaseNonExclusiveLock();
        }
    }

    /**
     * @return the current SQLite version as a {@link VersionCode}
     * @throws RuntimeException if the version could not be read
     */
    public VersionCode getSqliteVersion() {
        if (sqliteVersion == null) {
            synchronized (this) {
                if (sqliteVersion == null) {
                    sqliteVersion = readSqliteVersion();
                }
            }
        }
        return sqliteVersion;
    }

    private VersionCode readSqliteVersion() {
        acquireNonExclusiveLock();
        try {
            ICursor cursor = getDatabase().rawQuery("select sqlite_version()", null);
            try {
                if (cursor.moveToFirst()) {
                    String versionString = cursor.getString(0);
                    return VersionCode.parse(versionString);
                } else {
                    throw new RuntimeException("Query for SQLite version failed");
                }
            } finally {
                cursor.close();
            }
        } catch (RuntimeException e) {
            onError("Failed to read sqlite version", e);
            throw new RuntimeException("Failed to read sqlite version", e);
        } finally {
            releaseNonExclusiveLock();
        }
    }

    /**
     * Visitor that builds column definitions for {@link Property}s
     */
    private static class SqlConstructorVisitor implements PropertyVisitor<Void, StringBuilder> {

        private Void appendColumnDefinition(String type, Property<?> property, StringBuilder sql) {
            sql.append(property.getName()).append(" ").append(type);
            if (!SqlUtils.isEmpty(property.getColumnDefinition())) {
                sql.append(" ").append(property.getColumnDefinition());
            }
            return null;
        }

        @Override
        public Void visitDouble(Property<Double> property, StringBuilder sql) {
            return appendColumnDefinition("REAL", property, sql);
        }

        @Override
        public Void visitInteger(Property<Integer> property, StringBuilder sql) {
            return appendColumnDefinition("INTEGER", property, sql);
        }

        @Override
        public Void visitLong(Property<Long> property, StringBuilder sql) {
            return appendColumnDefinition("INTEGER", property, sql);
        }

        @Override
        public Void visitString(Property<String> property, StringBuilder sql) {
            return appendColumnDefinition("TEXT", property, sql);
        }

        @Override
        public Void visitBoolean(Property<Boolean> property, StringBuilder sql) {
            return appendColumnDefinition("INTEGER", property, sql);
        }

        @Override
        public Void visitBlob(Property<byte[]> property, StringBuilder sql) {
            return appendColumnDefinition("BLOB", property, sql);
        }
    }

    private static class RecreateDuringMigrationException extends RuntimeException {

        /* suppress compiler warning */
        private static final long serialVersionUID = 480910684116077495L;
    }

    /**
     * Exception thrown when an upgrade or downgrade fails for any reason. Clients that want to provide more
     * information about why an upgrade or downgrade failed can subclass this class and throw it intentionally in
     * {@link #onUpgrade(ISQLiteDatabase, int, int) onUpgrade()} or
     * {@link #onDowngrade(ISQLiteDatabase, int, int) onDowngrade()}, and it will be forwarded to
     * {@link #onMigrationFailed(MigrationFailedException) onMigrationFailed()}.
     */
    public static class MigrationFailedException extends RuntimeException {

        /* suppress compiler warning */
        private static final long serialVersionUID = 2949995666882182744L;

        public final String dbName;
        public final int oldVersion;
        public final int newVersion;

        public MigrationFailedException(String dbName, int oldVersion, int newVersion) {
            this(dbName, oldVersion, newVersion, null);
        }

        public MigrationFailedException(String dbName, int oldVersion, int newVersion, Throwable throwable) {
            super("Failed to migrate db " + dbName + " from version " + oldVersion + "to " + newVersion, throwable);
            this.dbName = dbName;
            this.oldVersion = oldVersion;
            this.newVersion = newVersion;
        }
    }

    // --- higher level dao methods

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
            SqlTable<?> table = getSqlTable(modelClass);
            if (table == null) {
                throw new IllegalArgumentException("Query has no FROM clause and model class "
                        + modelClass.getSimpleName() + " has no associated table");
            }
            query = query.from(table); // If argument was frozen, we may get a new object
        }
        CompiledStatement compiled = query.compile(getSqliteVersion());
        if (compiled.needsValidation) {
            String validateSql = query.sqlForValidation(getSqliteVersion());
            compileStatement(validateSql); // throws if the statement fails to compile
        }
        ICursor cursor = rawQuery(compiled.sql, compiled.sqlArgs);
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
        Table table = getTable(modelClass);
        int rowsUpdated = deleteInternal(Delete.from(table).where(table.getIdProperty().eq(id)));
        if (rowsUpdated > 0) {
            notifyForTable(DataChangedNotifier.DBOperation.DELETE, null, table, id);
        }
        return rowsUpdated > 0;
    }

    /**
     * Delete all rows matching the given {@link Criterion}
     *
     * @param modelClass model class for the table to delete from
     * @param where the Criterion to match. Note: passing null will delete all rows!
     * @return the number of deleted rows
     */
    public int deleteWhere(Class<? extends TableModel> modelClass, Criterion where) {
        Table table = getTable(modelClass);
        Delete delete = Delete.from(table);
        if (where != null) {
            delete.where(where);
        }
        int rowsUpdated = deleteInternal(delete);
        if (rowsUpdated > 0) {
            notifyForTable(DataChangedNotifier.DBOperation.DELETE, null, table, TableModel.NO_ID);
        }
        return rowsUpdated;
    }

    /**
     * Delete all rows for table corresponding to the given model class
     *
     * @param modelClass model class for the table to delete from
     * @return the number of deleted rows
     */
    public int deleteAll(Class<? extends TableModel> modelClass) {
        return deleteWhere(modelClass, null);
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
        int result = deleteInternal(delete);
        if (result > 0) {
            notifyForTable(DataChangedNotifier.DBOperation.DELETE, null, delete.getTable(), TableModel.NO_ID);
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
     * @param where the criterion to match. Note: passing null will update all rows!
     * @param template a model containing new values for the properties (columns) that should be updated. The template
     * class implicitly defines the table to be updated.
     * @return the number of updated rows
     */
    public int update(Criterion where, TableModel template) {
        return updateWithOnConflict(where, template, null);
    }

    /**
     * Update all rows in the table corresponding to the class of the given template
     *
     * @param template a model containing new values for the properties (columns) that should be updated. The template
     * class implicitly defines the table to be updated.
     * @return the number of updated rows
     */
    public int updateAll(TableModel template) {
        return update(null, template);
    }

    /**
     * Update all rows matching the given {@link Criterion}, setting values based on the provided template model. Any
     * constraint violations will be resolved using the specified {@link TableStatement.ConflictAlgorithm}.
     *
     * @param where the criterion to match. Note: passing null will update all rows!
     * @param template a model containing new values for the properties (columns) that should be updated
     * @param conflictAlgorithm the conflict algorithm to use
     * @return the number of updated rows
     * @see #update(Criterion, TableModel)
     */
    public int updateWithOnConflict(Criterion where, TableModel template,
            TableStatement.ConflictAlgorithm conflictAlgorithm) {
        Class<? extends TableModel> modelClass = template.getClass();
        Table table = getTable(modelClass);
        Update update = Update.table(table).fromTemplate(template);
        if (where != null) {
            update.where(where);
        }
        if (conflictAlgorithm != null) {
            update.onConflict(conflictAlgorithm);
        }

        int rowsUpdated = updateInternal(update);
        if (rowsUpdated > 0) {
            notifyForTable(DataChangedNotifier.DBOperation.UPDATE, template, table, TableModel.NO_ID);
        }
        return rowsUpdated;
    }

    /**
     * Update all rows in the table corresponding to the class of the given template
     *
     * @param template a model containing new values for the properties (columns) that should be updated. The template
     * class implicitly defines the table to be updated.
     * @param conflictAlgorithm the conflict algorithm to use
     * @return the number of updated rows
     */
    public int updateAllWithOnConflict(TableModel template, TableStatement.ConflictAlgorithm conflictAlgorithm) {
        return updateWithOnConflict(null, template, conflictAlgorithm);
    }

    /**
     * Executes an {@link Update} statement.
     * <p>
     * Note: Generally speaking, you should prefer to use {@link #update(Criterion, TableModel)}
     * or {@link #updateWithOnConflict(Criterion, TableModel, com.yahoo.squidb.sql.TableStatement.ConflictAlgorithm)}
     * for bulk database updates. This is provided as a convenience in case there exists a non-ORM case where a more
     * traditional SQL update statement is required for some reason.
     *
     * @param update statement to execute
     * @return the number of rows updated on success, -1 on failure
     */
    public int update(Update update) {
        int result = updateInternal(update);
        if (result > 0) {
            notifyForTable(DataChangedNotifier.DBOperation.UPDATE, null, update.getTable(), TableModel.NO_ID);
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
     * Any constraint violations will be resolved using the specified {@link TableStatement.ConflictAlgorithm}.
     *
     * @param item the model to save
     * @param conflictAlgorithm the conflict algorithm to use
     * @return true if current the model data is stored in the database
     * @see #persist(TableModel)
     */
    public boolean persistWithOnConflict(TableModel item, TableStatement.ConflictAlgorithm conflictAlgorithm) {
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
    protected final boolean insertRow(TableModel item, TableStatement.ConflictAlgorithm conflictAlgorithm) {
        Class<? extends TableModel> modelClass = item.getClass();
        Table table = getTable(modelClass);
        ValuesStorage mergedValues = item.getMergedValues();
        if (mergedValues.size() == 0) {
            return false;
        }
        Insert insert = Insert.into(table).fromValues(mergedValues);
        if (conflictAlgorithm != null) {
            insert.onConflict(conflictAlgorithm);
        }
        long newRow = insertInternal(insert);
        boolean result = newRow > 0;
        if (result) {
            notifyForTable(DataChangedNotifier.DBOperation.INSERT, item, table, newRow);
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
    protected final boolean updateRow(TableModel item, TableStatement.ConflictAlgorithm conflictAlgorithm) {
        if (!item.isModified()) { // nothing changed
            return true;
        }
        if (!item.isSaved()) {
            return false;
        }

        Class<? extends TableModel> modelClass = item.getClass();
        Table table = getTable(modelClass);
        Update update = Update.table(table).fromTemplate(item).where(table.getIdProperty().eq(item.getId()));
        if (conflictAlgorithm != null) {
            update.onConflict(conflictAlgorithm);
        }
        boolean result = updateInternal(update) > 0;
        if (result) {
            notifyForTable(DataChangedNotifier.DBOperation.UPDATE, item, table, item.getId());
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
        long result = insertInternal(insert);
        if (result > TableModel.NO_ID) {
            int numInserted = insert.getNumRows();
            notifyForTable(DataChangedNotifier.DBOperation.INSERT, null, insert.getTable(),
                    numInserted == 1 ? result : TableModel.NO_ID);
        }
        return result;
    }

    // --- helper methods

    protected <TYPE extends TableModel> SquidCursor<TYPE> fetchItemById(Class<TYPE> modelClass, long id,
            Property<?>... properties) {
        Table table = getTable(modelClass);
        return fetchFirstItem(modelClass, table.getIdProperty().eq(id), properties);
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
     * Count the number of rows matching a given {@link Criterion}. Use null to count all rows.
     *
     * @param modelClass the model class corresponding to the table
     * @param criterion the criterion to match
     * @return the number of rows matching the given criterion
     */
    public int count(Class<? extends AbstractModel> modelClass, Criterion criterion) {
        Property.IntegerProperty countProperty = Property.IntegerProperty.countProperty();
        Query query = Query.select(countProperty);
        if (criterion != null) {
            query.where(criterion);
        }
        SquidCursor<?> cursor = query(modelClass, query);
        try {
            cursor.moveToFirst();
            return cursor.get(countProperty);
        } finally {
            cursor.close();
        }
    }

    /**
     * Count the number of rows in the given table.
     *
     * @param modelClass the model class corresponding to the table
     * @return the number of rows in the table
     */
    public int countAll(Class<? extends AbstractModel> modelClass) {
        return count(modelClass, null);
    }

    // --- Data change notifications

    private final Object notifiersLock = new Object();
    private boolean dataChangedNotificationsEnabled = true;
    private List<DataChangedNotifier<?>> globalNotifiers = new ArrayList<DataChangedNotifier<?>>();
    private Map<SqlTable<?>, List<DataChangedNotifier<?>>> tableNotifiers
            = new HashMap<SqlTable<?>, List<DataChangedNotifier<?>>>();

    // Using a ThreadLocal makes it easy to have one accumulator set per transaction, since
    // transactions are also associated with the thread they run on
    private ThreadLocal<Set<DataChangedNotifier<?>>> notifierAccumulator
            = new ThreadLocal<Set<DataChangedNotifier<?>>>() {
        protected Set<DataChangedNotifier<?>> initialValue() {
            return new HashSet<DataChangedNotifier<?>>();
        }
    };

    /**
     * Register a {@link DataChangedNotifier} to listen for database changes. The DataChangedNotifier object will be
     * notified whenever a table it is interested is modified, and can accumulate a set of notifications to send when
     * the current transaction or statement completes successfully.
     *
     * @param notifier the DataChangedNotifier to register
     */
    public void registerDataChangedNotifier(DataChangedNotifier<?> notifier) {
        if (notifier == null) {
            return;
        }
        synchronized (notifiersLock) {
            Collection<SqlTable<?>> tables = notifier.whichTables();
            if (tables == null || tables.isEmpty()) {
                globalNotifiers.add(notifier);
            } else {
                for (SqlTable<?> table : tables) {
                    List<DataChangedNotifier<?>> notifiersForTable = tableNotifiers.get(table);
                    if (notifiersForTable == null) {
                        notifiersForTable = new ArrayList<DataChangedNotifier<?>>();
                        tableNotifiers.put(table, notifiersForTable);
                    }
                    notifiersForTable.add(notifier);
                }
            }
        }
    }

    /**
     * Unregister a {@link DataChangedNotifier} previously registered by
     * {@link #registerDataChangedNotifier(DataChangedNotifier)}
     *
     * @param notifier the DataChangedNotifier to unregister
     */
    public void unregisterDataChangedNotifier(DataChangedNotifier<?> notifier) {
        if (notifier == null) {
            return;
        }
        synchronized (notifiersLock) {
            Collection<SqlTable<?>> tables = notifier.whichTables();
            if (tables == null || tables.isEmpty()) {
                globalNotifiers.remove(notifier);
            } else {
                for (SqlTable<?> table : tables) {
                    List<DataChangedNotifier<?>> notifiersForTable = tableNotifiers.get(table);
                    if (notifiersForTable != null) {
                        notifiersForTable.remove(notifier);
                    }
                }
            }
        }
    }

    /**
     * Unregister all {@link DataChangedNotifier}s previously registered by
     * {@link #registerDataChangedNotifier(DataChangedNotifier)}
     */
    public void unregisterAllDataChangedNotifiers() {
        synchronized (notifiersLock) {
            globalNotifiers.clear();
            tableNotifiers.clear();
        }
    }

    /**
     * Set a flag to enable or disable data change notifications. No {@link DataChangedNotifier}s will be notified
     * (or accumulated during transactions) while the flag is set to false.
     */
    public void setDataChangedNotificationsEnabled(boolean enabled) {
        dataChangedNotificationsEnabled = enabled;
    }

    private void notifyForTable(DataChangedNotifier.DBOperation op, AbstractModel modelValues, SqlTable<?> table,
            long rowId) {
        if (!dataChangedNotificationsEnabled) {
            return;
        }
        synchronized (notifiersLock) {
            onDataChanged(globalNotifiers, op, modelValues, table, rowId);
            onDataChanged(tableNotifiers.get(table), op, modelValues, table, rowId);
        }
        if (!inTransaction()) {
            flushAccumulatedNotifications(true);
        }
    }

    private void onDataChanged(List<DataChangedNotifier<?>> notifiers, DataChangedNotifier.DBOperation op,
            AbstractModel modelValues, SqlTable<?> table, long rowId) {
        if (notifiers != null) {
            for (DataChangedNotifier<?> notifier : notifiers) {
                if (notifier.onDataChanged(table, this, op, modelValues, rowId)) {
                    notifierAccumulator.get().add(notifier);
                }
            }
        }
    }

    private void flushAccumulatedNotifications(boolean transactionSuccess) {
        Set<DataChangedNotifier<?>> accumulatedNotifiers = notifierAccumulator.get();
        if (!accumulatedNotifiers.isEmpty()) {
            for (DataChangedNotifier<?> notifier : accumulatedNotifiers) {
                notifier.flushAccumulatedNotifications(this, transactionSuccess && dataChangedNotificationsEnabled);
            }
            accumulatedNotifiers.clear();
        }
    }
}
