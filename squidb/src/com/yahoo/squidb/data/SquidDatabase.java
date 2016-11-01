/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.data;

import com.yahoo.squidb.Beta;
import com.yahoo.squidb.sql.CompileContext;
import com.yahoo.squidb.sql.CompiledStatement;
import com.yahoo.squidb.sql.Criterion;
import com.yahoo.squidb.sql.Delete;
import com.yahoo.squidb.sql.Field;
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
import com.yahoo.squidb.utility.SquidUtilities;
import com.yahoo.squidb.utility.VersionCode;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * SquidDatabase is a database abstraction which wraps a SQLite database.
 * <p>
 * Use this class to control the lifecycle of your database where you would normally use a
 * android.database.sqlite.SQLiteOpenHelper. The first call to a read or write operation will open the database.
 * You can close it again using {@link #close()}. For information about writing migrations or pre-populating a new
 * database see the {@link #onUpgrade(ISQLiteDatabase, int, int)} and
 * {@link #onTablesCreated(ISQLiteDatabase)} hooks.
 * <p>
 * SquidDatabase provides type safe reads and writes using model classes. For example, rather than using rawQuery to
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
 * <p>
 * When implementing your own database access methods in your SquidDatabase subclass, you should use
 * {@link #acquireExclusiveLock()} or {@link #acquireNonExclusiveLock()} to indicate your intent to access the database
 * connection. The non-exclusive lock simply indicates your intent to use the database, and prevents other threads
 * from e.g. closing the database while you are still using it. The exclusive lock prevents any other threads from
 * accessing the database, so should only be used when you want to ensure that your thread is the only one using the
 * database connection -- it should be used sparingly, if at all. The non-exclusive lock is sufficient for all basic
 * read/write use cases. See {@link #getDatabase()}.
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
     * boolean result = true;
     * case 1:
     *     result &amp;= tryAddColumn(MyModel.NEW_COL_1);
     * case 2:
     *     result &amp;= tryCreateTable(MyNewModel.TABLE);
     * }
     * return result;
     * </pre>
     *
     * If this method returns false or throws an exception, a call to
     * {@link #onMigrationFailed(MigrationFailedException)} is triggered. The default implementation of
     * onMigrationFailed rethrows the exception. It is highly recommended that you override onMigrationFailed to handle
     * errors, for example by calling {@link #recreate()} to delete all data in the database and start from scratch.
     * More sophisticated recovery logic would require a different means of opening the database file.
     *
     * @param db the {@link ISQLiteDatabase} being upgraded
     * @param oldVersion the current database version
     * @param newVersion the database version being upgraded to
     * @return true if the upgrade was handled successfully, false otherwise
     * @see #onMigrationFailed(MigrationFailedException)
     */
    protected abstract boolean onUpgrade(ISQLiteDatabase db, int oldVersion, int newVersion);

    /**
     * Called when the database should be downgraded from one version to another. If this method returns false or
     * throws an exception, a call to {@link #onMigrationFailed(MigrationFailedException)} is triggered. The default
     * implementation of onMigrationFailed rethrows the exception. It is highly recommended that you override
     * onMigrationFailed to handle errors, for example by calling {@link #recreate()} to delete all data in the
     * database and start from scratch. More sophisticated recovery logic would require a different means of opening
     * the database file.
     *
     * @param db the {@link ISQLiteDatabase} being upgraded
     * @param oldVersion the current database version
     * @param newVersion the database version being downgraded to
     * @return true if the downgrade was handled successfully, false otherwise. The default implementation returns true.
     * @see #onMigrationFailed(MigrationFailedException)
     */
    protected boolean onDowngrade(ISQLiteDatabase db, int oldVersion, int newVersion) {
        return true;
    }

    /**
     * Called to notify of a failure in {@link #onUpgrade(ISQLiteDatabase, int, int) onUpgrade()} or
     * {@link #onDowngrade(ISQLiteDatabase, int, int) onDowngrade()}, either because it returned false or because
     * an unexpected exception occurred.
     * <p>
     * The default implementation of this method rethrows the MigrationFailedException parameter. Subclasses can take
     * drastic corrective action here, e.g. recreating the database with {@link #recreate()}. If instead of calling
     * recreate() you choose to take other corrective action, you should finish by calling
     * {@link ISQLiteDatabase#setVersion(int)} to reflect that you were able to recover and complete the migration
     * successfully.
     * <p>
     * Calling {@link #getDatabase()} or any other DB access method from within this hook is generally unsafe, as the
     * database is not open when this hook is called, and attempting to reopen it without correcting the problem may
     * result in recursion, unless you specifically write your {@link #onUpgrade(ISQLiteDatabase, int, int)}
     * or {@link #onDowngrade(ISQLiteDatabase, int, int)} logic to handle such cases. ({@link #recreate()} is
     * still safe however). You also should not suppress this exception without taking any action. If this method
     * exits without throwing but the database is not open, another exception will be thrown that is likely to cause
     * a crash.
     * <p>
     * Failures to open the database not caused by an error in the migration flow are handled by
     * the {@link #onDatabaseOpenFailed(RuntimeException, int)} hook.
     *
     * @param failure details about the upgrade or downgrade that failed
     */
    protected void onMigrationFailed(MigrationFailedException failure) {
        throw failure;
    }

    /**
     * Called if the database has failed to open for any reason. Migration failures should be handled by the
     * {@link #onMigrationFailed(MigrationFailedException)} hook, but if you do not override that method, migration
     * failures will be forwarded to this hook instead. Non-migration failures that trigger this hook should be rare:
     * the result of programming errors, disk I/O failures, or corrupt databases. The default implementation of this
     * hook rethrows the exception, which will likely cause a crash. If you want to implement more sophisticated
     * failure handling, reasonable actions might be one or both of the following:
     * <ul>
     * <li>Call {@link #getDatabase()} to attempt reopening the database. This is a recursive operation, so it will
     * increment the retryCount argument if opening fails again. You shouldn't let the retry count grow infinitely,
     * lest you risk stack overflows.</li>
     * <li>Call {@link #recreate()} to delete the database file and recreate an empty one</li>
     * </ul>
     *
     * @param openFailureCount the number times this hook has been called, if you've called getDatabase() recursively.
     * The value will be 1 the first time this hook is called, 2 the second time, and so on.
     * @param failure the exception that caused opening the database to fail
     */
    @Beta
    protected void onDatabaseOpenFailed(RuntimeException failure, int openFailureCount) {
        throw failure;
    }

    /**
     * Called when the database connection is being configured, to enable features such as write-ahead logging or
     * foreign key support.
     *
     * This method may be called at different points in the database lifecycle depending on the environment. When using
     * a custom SQLite build with the squidb-sqlite-bindings project, or when running on Android API &gt;= 16, it is
     * called before {@link #onTablesCreated(ISQLiteDatabase) onTablesCreated},
     * {@link #onUpgrade(ISQLiteDatabase, int, int) onUpgrade},
     * {@link #onDowngrade(ISQLiteDatabase, int, int) onDowngrade},
     * and {@link #onOpen(ISQLiteDatabase) onOpen}. If it is running on stock Android SQLite and API &lt; 16, it
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
     * Called when the database is about to be closed. This method is called immediately before the database is closed,
     * so the ISQLiteDatabase parameter is still valid to use for executing any cleanup SQL that might be necessary.
     *
     * @param db the {@link ISQLiteDatabase} that is about to close
     */
    protected void onClose(ISQLiteDatabase db) {
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

    private Set<ISQLitePreparedStatement> trackedPreparedInserts = Collections.newSetFromMap(
            new ConcurrentHashMap<ISQLitePreparedStatement, Boolean>());
    private ThreadLocal<PreparedInsertCache> preparedInsertCache = newPreparedInsertCache(trackedPreparedInserts);
    private boolean preparedInsertCacheEnabled = false;

    private SquidDatabase attachedTo = null;
    private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private final Object databaseInstanceLock = new Object();

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
    private Map<Class<? extends AbstractModel>, SqlTable<?>> tableMap = new HashMap<>();

    private boolean isInMigration = false;
    private boolean isInMigrationFailedHook = false;
    private int databaseOpenFailedRetryCount = 0;

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

    /**
     * Subclasses of SquidDatabase override this method to create an {@link ISQLiteOpenHelper} suitable for the current
     * platform. For example, if the library is being used only on Android, you can simply return a new
     * AndroidOpenHelper instance. If on the other hand the library is being used on Android and iOS via j2objc,
     * you will need to define logic for instantiating the appropriate open helper -- AndroidOpenHelper from the
     * squidb-android module when on Android, or IOSOpenHelper from the squidb-ios module when on iOS.
     *
     * @param databaseName the name of the database being created/opened
     * @param delegate a delegate object for database lifecycle callbacks
     * @param version the current database version
     * @return an object suitable for the current platform that implements the {@link ISQLiteOpenHelper} interface
     */
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
     * from your SquidDatabase subclass with the intention of executing SQL, you should wrap the calls with a lock,
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
    protected final ISQLiteDatabase getDatabase() {
        // If we get here, we should already have the non-exclusive lock
        synchronized (databaseInstanceLock) {
            if (database == null) {
                openForWritingLocked();
            }
            return database;
        }
    }

    private void openForWritingLocked() {
        boolean areDataChangedNotificationsEnabled = areDataChangedNotificationsEnabled();
        setDataChangedNotificationsEnabled(false);
        try {
            try {
                ISQLiteDatabase db = getOpenHelper().openForWriting();
                setDatabase(db);
            } catch (RecreateDuringMigrationException recreate) {
                recreateLocked();
            } catch (MigrationFailedException fail) {
                onError(fail.getMessage(), fail);
                isInMigrationFailedHook = true;
                try {
                    // We don't want to be holding on to an invalid DB instance here
                    if (!isOpen()) {
                        closeLocked();
                    }
                    onMigrationFailed(fail);
                } finally {
                    isInMigrationFailedHook = false;
                }
            }
            if (!isOpen()) {
                closeLocked();
                throw new RuntimeException("Failed to open database");
            }
        } catch (RuntimeException e) {
            onError("Failed to open database: " + getName(), e);

            // If any runtime exception occurs, make sure we aren't holding on to a partially open DB instance.
            // It would be invalid if the exception were suppressed accidentally
            closeLocked();

            int retryCount = ++databaseOpenFailedRetryCount;
            try {
                onDatabaseOpenFailed(e, retryCount);
                // If this hook exits cleanly but the db still isn't open, the user probably did something bad in
                // the hook, so we should clean up and rethrow
                if (!isOpen()) {
                    closeLocked();
                    throw e;
                }
            } finally {
                databaseOpenFailedRetryCount = 0;
            }
        } finally {
            setDataChangedNotificationsEnabled(areDataChangedNotificationsEnabled);
        }
    }

    /**
     * Enables or disables the prepared insert cache. Generally speaking, enabling this cache will result in a
     * performance improvement when inserting rows, especially in large transactions. Under ideal conditions,
     * performance may be improved up to 70%, and a 25-50% gain is a reasonable expectation for most cases. However,
     * the gains may not be noticeable on some older devices or in low-memory environments. The feature is experimental
     * and is disabled by default.
     *
     * @param enabled true to enable the prepared insert cache, false to disable it
     */
    @Beta
    protected void setPreparedInsertCacheEnabled(boolean enabled) {
        preparedInsertCacheEnabled = enabled;
    }

    private ThreadLocal<PreparedInsertCache> newPreparedInsertCache(
            final Set<ISQLitePreparedStatement> openStatementTracking) {
        return new ThreadLocal<PreparedInsertCache>() {
            @Override
            protected PreparedInsertCache initialValue() {
                return new PreparedInsertCache(openStatementTracking);
            }
        };
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
     * @return true if a connection to the {@link ISQLiteDatabase} is open, false otherwise
     */
    public final boolean isOpen() {
        synchronized (databaseInstanceLock) {
            return database != null && database.isOpen();
        }
    }

    /**
     * Close the database if it has been opened previously. This method acquires the exclusive lock before closing the
     * db -- it will block if other threads are in transactions. This method will throw an exception if called from
     * within a transaction.
     * <p>
     * It is not safe to call this method from within any of the database open or migration hooks (e.g.
     * {@link #onUpgrade(ISQLiteDatabase, int, int)}, {@link #onOpen(ISQLiteDatabase)},
     * {@link #onMigrationFailed(MigrationFailedException)}), etc.
     * <p>
     * WARNING: Any open database resources (e.g. cursors) will be invalid after calling this method. Do not call this
     * method if any open cursors may be in use.
     */
    public final void close() {
        acquireExclusiveLock();
        try {
            closeLocked();
        } finally {
            releaseExclusiveLock();
        }
    }

    /**
     * Clear all data in the database. This method acquires the exclusive lock before closing the db -- it will block
     * if other threads are in transactions. This method will throw an exception if called from within a transaction.
     * <p>
     * It is not safe to call this method from within any of the database open or migration hooks (e.g.
     * {@link #onUpgrade(ISQLiteDatabase, int, int)}, {@link #onOpen(ISQLiteDatabase)},
     * {@link #onMigrationFailed(MigrationFailedException)}), etc.
     * <p>
     * WARNING: Any open database resources (e.g. cursors) will be invalid after calling this method. Do not call this
     * method if any open cursors may be in use. The existing database file will be deleted and all data will be lost.
     */
    public final void clear() {
        acquireExclusiveLock();
        try {
            closeAndDeleteLocked();
        } finally {
            releaseExclusiveLock();
        }
    }

    /**
     * Clears the database and recreates an empty version of it. This method acquires the exclusive lock before closing
     * the db -- it will block if other threads are in transactions. This method will throw an exception if called from
     * within a transaction.
     * <p>
     * If called from within the {@link #onUpgrade(ISQLiteDatabase, int, int)} or
     * {@link #onDowngrade(ISQLiteDatabase, int, int)} hooks, this method will abort the remainder of the
     * migration and simply clear the database. This method is also safe to call from within
     * {@link #onMigrationFailed(MigrationFailedException)}
     * <p>
     * WARNING: Any open database resources (e.g. cursors) will be invalid after calling this method. Do not call this
     * method if any open cursors may be in use. The existing database file will be deleted and all data will be lost,
     * with a new empty database taking its place.
     *
     * @see #clear()
     */
    public final void recreate() {
        if (isInMigration) {
            throw new RecreateDuringMigrationException();
        } else if (isInMigrationFailedHook || databaseOpenFailedRetryCount > 0) {
            recreateLocked(); // Safe to call here, necessary locks are already held in this case
        } else {
            acquireExclusiveLock();
            try {
                recreateLocked();
            } finally {
                releaseExclusiveLock();
            }
        }
    }

    private void recreateLocked() {
        synchronized (databaseInstanceLock) {
            closeAndDeleteLocked();
            getDatabase();
        }
    }

    private void closeLocked() {
        synchronized (databaseInstanceLock) {
            closeAndDeleteInternal(false);
        }
    }

    private void closeAndDeleteLocked() {
        synchronized (databaseInstanceLock) {
            closeAndDeleteInternal(true);
        }
    }

    private void closeAndDeleteInternal(boolean deleteAfterClose) {
        clearPreparedStatementCache();
        if (isOpen()) {
            onClose(database);
            database.close();
        }
        setDatabase(null);
        if (deleteAfterClose) {
            getOpenHelper().deleteDatabase();
        }
        helper = null;
    }

    private void clearPreparedStatementCache() {
        for (ISQLitePreparedStatement statement : trackedPreparedInserts) {
            statement.close();
        }
        trackedPreparedInserts.clear();
        preparedInsertCache = newPreparedInsertCache(trackedPreparedInserts);
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
     * Execute a raw SQLite query. This method takes an Object[] for the arguments because Android's default behavior
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

    /**
     * Execute a statement that returns a 1x1 String result. If you know your result set will only have one row and
     * column, this is much more efficient than calling {@link #rawQuery(String, Object[])} and parsing the cursor.
     * <br>
     * Note: This will throw an exception if the given SQL query returns a result that is not a single column
     *
     * @param sql a sql statement
     * @param sqlArgs arguments to bind to the sql statement
     * @return the String result of the query
     */
    public String simpleQueryForString(String sql, Object[] sqlArgs) {
        acquireNonExclusiveLock();
        try {
            return getDatabase().simpleQueryForString(sql, sqlArgs);
        } finally {
            releaseNonExclusiveLock();
        }
    }

    /**
     * Execute a statement that returns a 1x1 long result. If you know your result set will only have one row and
     * column, this is much more efficient than calling {@link #rawQuery(String, Object[])} and parsing the cursor.
     * <br>
     * Note: This will throw an exception if the given SQL query returns a result that is not a single column
     *
     * @param sql a sql statement
     * @param sqlArgs arguments to bind to the sql statement
     * @return the long result of the query
     */
    public long simpleQueryForLong(String sql, Object[] sqlArgs) {
        acquireNonExclusiveLock();
        try {
            return getDatabase().simpleQueryForLong(sql, sqlArgs);
        } finally {
            releaseNonExclusiveLock();
        }
    }

    /**
     * Execute a statement that returns a 1x1 String result. If you know your result set will only have one row and
     * column, this is much more efficient than calling {@link #rawQuery(String, Object[])} and parsing the cursor.
     * <br>
     * Note: This will throw an exception if the given SQL query returns a result that is not a single column
     *
     * @param query a sql query
     * @return the String result of the query
     */
    public String simpleQueryForString(Query query) {
        CompiledStatement compiled = query.compile(getCompileContext());
        return simpleQueryForString(compiled.sql, compiled.sqlArgs);
    }

    /**
     * Execute a statement that returns a 1x1 long result. If you know your result set will only have one row and
     * column, this is much more efficient than calling {@link #rawQuery(String, Object[])} and parsing the cursor.
     * <br>
     * Note: This will throw an exception if the given SQL query returns a result that is not a single column
     *
     * @param query a sql query
     * @return the long result of the query
     */
    public long simpleQueryForLong(Query query) {
        CompiledStatement compiled = query.compile(getCompileContext());
        return simpleQueryForLong(compiled.sql, compiled.sqlArgs);
    }

    /**
     * Execute a SQL {@link com.yahoo.squidb.sql.Insert} statement
     *
     * @return the row id of the last row inserted on success, -1 on failure
     */
    private long insertInternal(Insert insert) {
        CompiledStatement compiled = insert.compile(getCompileContext());
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
        CompiledStatement compiled = delete.compile(getCompileContext());
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
        CompiledStatement compiled = update.compile(getCompileContext());
        acquireNonExclusiveLock();
        try {
            return getDatabase().executeUpdateDelete(compiled.sql, compiled.sqlArgs);
        } finally {
            releaseNonExclusiveLock();
        }
    }

    // --- transaction management

    /**
     * Begin a transaction in EXCLUSIVE mode. Other reader and writer threads will not be able to access the database
     * (i.e. will block) while this transaction is active.
     * <p>
     * As with Android's SQLiteDatabase, transactions can be nested. If any inner transaction is not marked as
     * successful, the entire outer transaction is considered to have failed and will be rolled back. Otherwise all
     * changes will be committed.
     * <p>
     * This method acquires the SquidDatabase's non-exclusive lock to prevent the database from being closed while
     * the transaction is active.
     * <p>
     * The recommended pattern for beginning and ending transactions is this:
     *
     * <pre>
     *   db.beginTransaction();
     *   try {
     *     ...
     *     db.setTransactionSuccessful();
     *   } finally {
     *     db.endTransaction();
     *   }
     * </pre>
     *
     * @see #acquireNonExclusiveLock()
     * @see ISQLiteDatabase#beginTransaction()
     */
    public void beginTransaction() {
        acquireNonExclusiveLock();
        try {
            getDatabase().beginTransaction();
            transactionSuccessState.get().beginTransaction();
        } catch (RuntimeException e) {
            // Only release lock if begin xact was not successful
            releaseNonExclusiveLock();
            throw e;
        }
    }

    /**
     * Begin a transaction in IMMEDIATE mode. Other writer threads will not be able to access the database (i.e. will
     * block) while this transaction is active, but reader threads may be able to read from the database if it is
     * configured to use write-ahead logging.
     * <p>
     * As with Android's SQLiteDatabase, transactions can be nested. If any inner transaction is not marked as
     * successful, the entire outer transaction is considered to have failed and will be rolled back. Otherwise all
     * changes will be committed.
     * <p>
     * This method acquires the SquidDatabase's non-exclusive lock to prevent the database from being closed while
     * the transaction is active.
     * <p>
     * The recommended pattern for beginning and ending transactions is this:
     *
     * <pre>
     *   db.beginTransactionNonExclusive();
     *   try {
     *     ...
     *     db.setTransactionSuccessful();
     *   } finally {
     *     db.endTransaction();
     *   }
     * </pre>
     *
     * @see #acquireNonExclusiveLock()
     * @see ISQLiteDatabase#beginTransactionNonExclusive()
     * @see ISQLiteDatabase#enableWriteAheadLogging()
     */
    public void beginTransactionNonExclusive() {
        acquireNonExclusiveLock();
        try {
            getDatabase().beginTransactionNonExclusive();
            transactionSuccessState.get().beginTransaction();
        } catch (RuntimeException e) {
            // Only release lock if begin xact was not successful
            releaseNonExclusiveLock();
            throw e;
        }
    }

    /**
     * Begin a transaction in EXCLUSIVE mode with the given listener. Other reader and writer threads will not be able
     * to access the database (i.e. will block) while this transaction is active.
     * <p>
     * As with Android's SQLiteDatabase, transactions can be nested. If any inner transaction is not marked as
     * successful, the entire outer transaction is considered to have failed and will be rolled back. Otherwise all
     * changes will be committed.
     * <p>
     * This method acquires the SquidDatabase's non-exclusive lock to prevent the database from being closed while
     * the transaction is active.
     * <p>
     * The recommended pattern for beginning and ending transactions is this:
     *
     * <pre>
     *   db.beginTransactionWithListener(listener);
     *   try {
     *     ...
     *     db.setTransactionSuccessful();
     *   } finally {
     *     db.endTransaction();
     *   }
     * </pre>
     *
     * @param listener the transaction listener
     * @see #acquireNonExclusiveLock()
     * @see ISQLiteDatabase#beginTransactionWithListener(SquidTransactionListener)
     */
    public void beginTransactionWithListener(SquidTransactionListener listener) {
        acquireNonExclusiveLock();
        try {
            getDatabase().beginTransactionWithListener(listener);
            transactionSuccessState.get().beginTransaction();
        } catch (RuntimeException e) {
            // Only release lock if begin xact was not successful
            releaseNonExclusiveLock();
            throw e;
        }
    }

    /**
     * Begin a transaction in IMMEDIATE mode with the given listener. Other writer threads will not be able to access
     * the database (i.e. will block) while this transaction is active, but reader threads may be able to read from the
     * database if it is configured to use write-ahead logging.
     * <p>
     * As with Android's SQLiteDatabase, transactions can be nested. If any inner transaction is not marked as
     * successful, the entire outer transaction is considered to have failed and will be rolled back. Otherwise all
     * changes will be committed.
     * <p>
     * This method acquires the SquidDatabase's non-exclusive lock to prevent the database from being closed while
     * the transaction is active.
     * <p>
     * The recommended pattern for beginning and ending transactions is this:
     *
     * <pre>
     *   db.beginTransactionWithListenerNonExclusive(listener);
     *   try {
     *     ...
     *     db.setTransactionSuccessful();
     *   } finally {
     *     db.endTransaction();
     *   }
     * </pre>
     *
     * @param listener the transaction listener
     * @see #acquireNonExclusiveLock()
     * @see ISQLiteDatabase#beginTransactionWithListenerNonExclusive(SquidTransactionListener)
     * @see ISQLiteDatabase#enableWriteAheadLogging()
     */
    public void beginTransactionWithListenerNonExclusive(SquidTransactionListener listener) {
        acquireNonExclusiveLock();
        try {
            getDatabase().beginTransactionWithListenerNonExclusive(listener);
            transactionSuccessState.get().beginTransaction();
        } catch (RuntimeException e) {
            // Only release lock if begin xact was not successful
            releaseNonExclusiveLock();
            throw e;
        }
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
     * @return true if a transaction is active on the current thread
     * @see ISQLiteDatabase#inTransaction()
     */
    public final boolean inTransaction() {
        synchronized (databaseInstanceLock) {
            return database != null && database.inTransaction();
        }
    }

    /**
     * End the current transaction
     *
     * @see ISQLiteDatabase#endTransaction()
     */
    public void endTransaction() {
        TransactionSuccessState successState = transactionSuccessState.get();
        try {
            getDatabase().endTransaction();
        } catch (RuntimeException e) {
            successState.unsetTransactionSuccessful();
            throw e;
        } finally {
            releaseNonExclusiveLock();

            successState.endTransaction();
            if (!successState.inTransaction()) {
                flushAccumulatedNotifications(successState.outerTransactionSuccess);
                successState.reset();
            }
        }
    }

    // Tracks nested transaction success or failure state. If any
    // nested transaction fails, the entire outer transaction
    // is also considered to have failed.
    private static class TransactionSuccessState {

        Deque<Boolean> nestedSuccessStack = new LinkedList<>();
        boolean outerTransactionSuccess = true;

        private void beginTransaction() {
            nestedSuccessStack.push(false);
        }

        private boolean inTransaction() {
            return nestedSuccessStack.size() > 0;
        }

        private void setTransactionSuccessful() {
            nestedSuccessStack.pop();
            nestedSuccessStack.push(true);
        }

        // For when endTransaction throws
        private void unsetTransactionSuccessful() {
            nestedSuccessStack.pop();
            nestedSuccessStack.push(false);
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
     * exclusive lock is an error! We will throw an exception if this method is called while the
     * calling thread is in a transaction or otherwise holds the non-exclusive lock. Otherwise, this method will block
     * until all non-exclusive locks acquired with {@link #acquireNonExclusiveLock()} have been released, but will
     * prevent any new non-exclusive locks from being acquired while it blocks.
     */
    protected void acquireExclusiveLock() {
        if (readWriteLock.getReadHoldCount() > 0 && readWriteLock.getWriteHoldCount() == 0) {
            throw new IllegalStateException("Can't acquire an exclusive lock when the calling thread is in a "
                    + "transaction or otherwise holds a non-exclusive lock and not the exclusive lock");
        }
        readWriteLock.writeLock().lock();
    }

    /**
     * Release the exclusive lock acquired by {@link #acquireExclusiveLock()}
     */
    protected void releaseExclusiveLock() {
        readWriteLock.writeLock().unlock();
    }

    /**
     * Acquire a non-exclusive lock on the database. This is semantically similar to acquiring a read lock in a {@link
     * java.util.concurrent.locks.ReadWriteLock ReadWriteLock} but may also be used in most cases to protect database
     * writes (see {@link #acquireExclusiveLock()} for why this is true). This will block if the exclusive lock is held
     * by some other thread. Many threads can hold non-exclusive locks as long as no thread holds the exclusive lock.
     */
    protected void acquireNonExclusiveLock() {
        readWriteLock.readLock().lock();
    }

    /**
     * Releases a non-exclusive lock acquired with {@link #acquireNonExclusiveLock()}
     */
    protected void releaseNonExclusiveLock() {
        readWriteLock.readLock().unlock();
    }

    // --- helper classes

    /**
     * Delegate class passed to a {@link ISQLiteOpenHelper} instance that allows the {@link ISQLiteOpenHelper}
     * implementation to call back into its owning SquidDatabase after the database has been created or opened.
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
                    table.appendCreateTableSql(getCompileContext(), sql, sqlVisitor);
                    db.execSQL(sql.toString());
                    sql.setLength(0);
                }
            }

            View[] views = getViews();
            if (views != null) {
                for (View view : views) {
                    view.createViewSql(getCompileContext(), sql);
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
            Exception thrown = null;
            isInMigration = true;
            try {
                success = SquidDatabase.this.onUpgrade(db, oldVersion, newVersion);
            } catch (Exception e) {
                thrown = e;
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
            Exception thrown = null;
            isInMigration = true;
            try {
                success = SquidDatabase.this.onDowngrade(db, oldVersion, newVersion);
            } catch (Exception e) {
                thrown = e;
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

    private void setDatabase(ISQLiteDatabase db) {
        synchronized (databaseInstanceLock) {
            // If we're already holding a reference to the same object, don't need to update or recalculate the version
            if (database != null && db != null && db.getWrappedObject() == database.getWrappedObject()) {
                return;
            }
            sqliteVersion = db != null ? readSqliteVersionLocked(db) : null;
            database = db;
        }
    }

    private VersionCode readSqliteVersionLocked(ISQLiteDatabase db) {
        try {
            String versionString = db.simpleQueryForString("select sqlite_version()", null);
            return VersionCode.parse(versionString);
        } catch (RuntimeException e) {
            onError("Failed to read sqlite version", e);
            throw e;
        }
    }

    // --- utility methods

    /**
     * Add a column to a table by specifying the corresponding {@link Property}
     *
     * @param property the Property associated with the column to add
     * @return true if the statement executed without error, false otherwise
     */
    protected boolean tryAddColumn(Property<?> property) {
        if (!propertyBelongsToTable(property)) {
            throw new IllegalArgumentException("Can't alter table: property does not belong to a Table");
        }
        SqlConstructorVisitor visitor = new SqlConstructorVisitor();
        StringBuilder sql = new StringBuilder(STRING_BUILDER_INITIAL_CAPACITY);
        sql.append("ALTER TABLE ").append(property.tableModelName.tableName).append(" ADD ");
        property.accept(visitor, sql);
        return tryExecSql(sql.toString());
    }

    private boolean propertyBelongsToTable(Property<?> property) {
        return property.tableModelName.modelClass != null &&
                TableModel.class.isAssignableFrom(property.tableModelName.modelClass) &&
                !SqlUtils.isEmpty(property.tableModelName.tableName);
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
        table.appendCreateTableSql(getCompileContext(), sql, sqlVisitor);
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
        view.createViewSql(getCompileContext(), sql);
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
        CompiledStatement compiled = statement.compile(getCompileContext());
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
        } catch (RuntimeException e) {
            onError("Failed to execute statement: " + sql, e);
            return false;
        } finally {
            releaseNonExclusiveLock();
        }
    }

    /**
     * Execute a raw SQL statement. May throw a runtime exception if there is an error parsing the SQL or some other
     * error
     *
     * @param sql the statement to execute
     * @see ISQLiteDatabase#execSQL(String)
     */
    public void execSqlOrThrow(String sql) {
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
        } catch (RuntimeException e) {
            onError("Failed to execute statement: " + sql, e);
            return false;
        } finally {
            releaseNonExclusiveLock();
        }
    }

    /**
     * Execute a raw SQL statement with optional arguments. The sql string may contain '?' placeholders for the
     * arguments. May throw a runtime exception if there is an error parsing the SQL or some other error
     *
     * @param sql the statement to execute
     * @param bindArgs the arguments to bind to the statement
     * @see ISQLiteDatabase#execSQL(String, Object[])
     */
    public void execSqlOrThrow(String sql, Object[] bindArgs) {
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
        VersionCode toReturn = sqliteVersion;
        if (toReturn == null) {
            acquireNonExclusiveLock();
            try {
                synchronized (databaseInstanceLock) {
                    getDatabase(); // Opening the database will populate the sqliteVersion field
                    return sqliteVersion;
                }
            } finally {
                releaseNonExclusiveLock();
            }
        }
        return toReturn;
    }

    /**
     * @return a CompileContext that this SquidDatabase should use when compiling SQL statements like {@link Query},
     * {@link Insert}, {@link Update}, and {@link Delete}. If necessary, users can customize the returned
     * CompileContext object by overriding {@link #buildCompileContext(CompileContext.Builder)} to e.g. specify a
     * different implementation of {@link com.yahoo.squidb.sql.ArgumentResolver} to use.
     */
    public final CompileContext getCompileContext() {
        CompileContext.Builder builder = new CompileContext.Builder(getSqliteVersion());
        buildCompileContext(builder);
        return builder.build();
    }

    /**
     * Users can override this method to customize the CompileContext object that will be used when compiling SQL
     * statements, e.g. by setting a custom {@link com.yahoo.squidb.sql.ArgumentResolver} to handle non-primitive
     * arguments in the SQL. Most users will not need to use this hook.
     *
     * @param builder a builder for a {@link CompileContext} object to be returned by {@link #getCompileContext()}
     * @see #getCompileContext()
     */
    protected void buildCompileContext(CompileContext.Builder builder) {
        // Subclasses can override to change the basic parameters of the CompileContext
    }

    /**
     * Prepares a low-level SQLite statement, represented as an instance of {@link ISQLitePreparedStatement}. The
     * statement should either be a non-query (e.g. an INSERT or UPDATE) or a query that returns only a 1x1 result.
     * You should call {@link ISQLitePreparedStatement#close()} when you are finished with the prepared statement.
     * <p>
     * The returned object is only safe to use while this database is still open. The easiest/recommended way to
     * manage prepared statements is to avoid keeping any cached instances in memory for a long time. For example, this
     * could be accomplished by only acquiring prepared statements within a transaction, using them to do work within
     * that transaction only, and then closing the prepared statement when the transaction is about to end.
     * <p>
     * If you are acquiring/using a prepared statement only within the duration of a transaction, no additional locking
     * is necessary. If you do choose to keep a prepared statement alive outside the scope you created it in, you may
     * require additional locking if any code path in your app may close/re-open the database. If you wish to
     * prevent the database from being closed while the prepared statement is open/in use, you can acquire the
     * database's non-exclusive lock using {@link #acquireNonExclusiveLock()}, which will prevent the DB from being
     * closed while the lock is held. You can release such a lock with {@link #releaseNonExclusiveLock()}. Note that
     * any thread attempting to close the DB will block if such a lock is held, so use them carefully. Failure to
     * implement such locking in an app that may close the database could lead to race conditions.
     * <p>
     * If you keep long-lived references to prepared statements alive and some code path in your app may close/re-open
     * the database, you should take care to clean up any such references by closing any open statements and nulling
     * out any references to them so they can't accidentally be used after the database has been closed. Such
     * bookkeeping can be done by taking advantage of the {@link #onClose(ISQLiteDatabase)} hook, which is called
     * immediately before the database connection is about to be closed.
     *
     * @param sql the SQL to compile into a prepared statement
     * @return a {@link ISQLitePreparedStatement} object representing the compiled SQL
     */
    public ISQLitePreparedStatement prepareStatement(String sql) {
        acquireNonExclusiveLock();
        try {
            return getDatabase().prepareStatement(sql);
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
            super("Failed to migrate db " + dbName + " from version " + oldVersion + " to " + newVersion, throwable);
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
        query = inferTableForQuery(modelClass, query);
        CompiledStatement compiled = query.compile(getCompileContext());
        if (compiled.needsValidation) {
            String validateSql = query.sqlForValidation(getCompileContext());
            ensureSqlCompiles(validateSql); // throws if the statement fails to compile
        }
        ICursor cursor = rawQuery(compiled.sql, compiled.sqlArgs);
        return new SquidCursor<>(cursor, modelClass, query.getFields());
    }

    // If the query does not have a from clause, look up the table by model object and add it to the query. May
    // return a new query object if the argument passed was frozen.
    private Query inferTableForQuery(Class<? extends AbstractModel> modelClass, Query query) {
        if (!query.hasTable() && modelClass != null) {
            SqlTable<?> table = getSqlTable(modelClass);
            if (table == null) {
                throw new IllegalArgumentException("Query has no FROM clause and model class "
                        + modelClass.getSimpleName() + " has no associated table");
            }
            query = query.from(table); // If argument was frozen, we may get a new object
        }
        return query;
    }

    // For use only when validating queries
    private void ensureSqlCompiles(String sql) {
        acquireNonExclusiveLock();
        try {
            getDatabase().ensureSqlCompiles(sql);
        } finally {
            releaseNonExclusiveLock();
        }
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
        } catch (Exception e) {
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
        int rowsUpdated = deleteInternal(Delete.from(table).where(table.getRowIdProperty().eq(id)));
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
     * constraint violations will be resolved using the specified
     * {@link com.yahoo.squidb.sql.TableStatement.ConflictAlgorithm ConflictAlgorithm}.
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
     * Any constraint violations will be resolved using the specified
     * {@link com.yahoo.squidb.sql.TableStatement.ConflictAlgorithm ConflictAlgorithm}.
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
        item.setRowId(TableModel.NO_ID);
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

        long newRow;
        if (preparedInsertCacheEnabled) {
            acquireNonExclusiveLock();
            try {
                PreparedInsertCache insertCache = preparedInsertCache.get();
                ISQLitePreparedStatement preparedStatement =
                        insertCache.getPreparedInsert(this, table, conflictAlgorithm);
                item.bindValuesForInsert(table, preparedStatement);
                newRow = preparedStatement.executeInsert();
            } finally {
                releaseNonExclusiveLock();
            }
        } else {
            newRow = insertRowLegacy(item, table, conflictAlgorithm);
        }

        boolean result = newRow > 0;
        if (result) {
            notifyForTable(DataChangedNotifier.DBOperation.INSERT, item, table, newRow);
            item.setRowId(newRow);
            item.markSaved();
        }
        return result;
    }

    private long insertRowLegacy(TableModel item, Table table, TableStatement.ConflictAlgorithm conflictAlgorithm) {
        ValuesStorage mergedValues = item.getMergedValues();
        if (mergedValues.size() == 0) {
            return -1;
        }
        Insert insert = Insert.into(table).fromValues(mergedValues);
        if (conflictAlgorithm != null) {
            insert.onConflict(conflictAlgorithm);
        }
        return insertInternal(insert);
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
        Update update = Update.table(table).fromTemplate(item).where(table.getRowIdProperty().eq(item.getRowId()));
        if (conflictAlgorithm != null) {
            update.onConflict(conflictAlgorithm);
        }
        boolean result = updateInternal(update) > 0;
        if (result) {
            notifyForTable(DataChangedNotifier.DBOperation.UPDATE, item, table, item.getRowId());
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
        return fetchFirstItem(modelClass, table.getRowIdProperty().eq(id), properties);
    }

    protected <TYPE extends AbstractModel> SquidCursor<TYPE> fetchFirstItem(Class<TYPE> modelClass,
            Criterion criterion, Property<?>... properties) {
        return fetchFirstItem(modelClass, Query.select(properties).where(criterion));
    }

    protected <TYPE extends AbstractModel> SquidCursor<TYPE> fetchFirstItem(Class<TYPE> modelClass, Query query) {
        boolean immutableQuery = query.isImmutable();
        Field<Integer> beforeLimit = query.getLimit();
        SqlTable<?> beforeTable = query.getTable();
        query = query.limit(1); // If argument was frozen, we may get a new object
        SquidCursor<TYPE> cursor = query(modelClass, query);
        if (!immutableQuery) {
            query.from(beforeTable).limit(beforeLimit); // Reset for user
        }
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
        query = inferTableForQuery(modelClass, query);
        CompiledStatement compiled = query.compile(getCompileContext());
        acquireNonExclusiveLock();
        try {
            return (int) getDatabase().simpleQueryForLong(compiled.sql, compiled.sqlArgs);
        } finally {
            releaseNonExclusiveLock();
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
    private List<DataChangedNotifier<?>> globalNotifiers = new ArrayList<>();
    private Map<SqlTable<?>, List<DataChangedNotifier<?>>> tableNotifiers = new HashMap<>();

    // Using a ThreadLocal makes it easy to have one accumulator set per transaction, since
    // transactions are also associated with the thread they run on
    private ThreadLocal<Set<DataChangedNotifier<?>>> notifierAccumulator
            = new ThreadLocal<Set<DataChangedNotifier<?>>>() {
        protected Set<DataChangedNotifier<?>> initialValue() {
            return new HashSet<>();
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
                        notifiersForTable = new ArrayList<>();
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

    /**
     * @return true if data change notifications are enabled for this database; false otherwise
     */
    public boolean areDataChangedNotificationsEnabled() {
        return dataChangedNotificationsEnabled;
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

    // -- debugging utilities

    /**
     * Directly analogous to {@link #query(Class, Query)}, but instead of returning a result, this method just logs the
     * output of EXPLAIN QUERY PLAN for the given query. This is method is intended for debugging purposes only.
     */
    public void explainQueryPlan(Class<? extends AbstractModel> modelClass, Query query) {
        query = inferTableForQuery(modelClass, query);
        CompiledStatement compiled = query.compile(getCompileContext());
        ICursor cursor = rawQuery("EXPLAIN QUERY PLAN " + compiled.sql, compiled.sqlArgs);
        try {
            Logger.d(Logger.LOG_TAG, "Query plan for: " + compiled.sql);
            SquidUtilities.dumpCursor(cursor, -1);
        } finally {
            cursor.close();
        }
    }

    /**
     * Copies the database file and any supporting journal or WAL files needed to open the DB to the given directory.
     * This method acquires the exclusive lock on the database before copying, which will prevent any other threads
     * from reading or writing to the database while the copying is in progress. If this method is called from within
     * a transaction, an exception will be thrown. This method is intended for debugging purposes only.
     *
     * @param toDir the directory to copy the database files to
     * @return true if copying the database files succeeded, false otherwise
     */
    public boolean copyDatabase(File toDir) {
        acquireExclusiveLock();
        try {
            return copyDatabaseLocked(toDir);
        } finally {
            releaseExclusiveLock();
        }
    }

    private boolean copyDatabaseLocked(File toDir) {
        if (!(toDir.mkdirs() || toDir.isDirectory())) {
            Logger.e(Logger.LOG_TAG, "Error creating directories for database copy");
            return false;
        }
        File dbFile = new File(getDatabasePath());
        try {
            if (copyFileIfExists(dbFile, toDir)) {
                copyFileIfExists(new File(dbFile.getPath() + "-journal"), toDir);
                copyFileIfExists(new File(dbFile.getPath() + "-shm"), toDir);
                copyFileIfExists(new File(dbFile.getPath() + "-wal"), toDir);
            } else {
                Logger.e(Logger.LOG_TAG, "Attempted to copy database " + getName() + " but it doesn't exist yet");
                return false;
            }
        } catch (IOException e) {
            Logger.e(Logger.LOG_TAG, "Error copying database " + getName(), e);
            return false;
        }
        return true;
    }

    private boolean copyFileIfExists(File in, File toDir) throws IOException {
        if (in.exists()) {
            SquidUtilities.copyFile(in, new File(toDir.getAbsolutePath() + File.separator + in.getName()));
            return true;
        }
        return false;
    }
}
