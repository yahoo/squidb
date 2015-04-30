/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.data;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.database.sqlite.SQLiteTransactionListener;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.yahoo.squidb.Beta;
import com.yahoo.squidb.sql.CompiledStatement;
import com.yahoo.squidb.sql.Delete;
import com.yahoo.squidb.sql.Index;
import com.yahoo.squidb.sql.Insert;
import com.yahoo.squidb.sql.Property;
import com.yahoo.squidb.sql.Property.PropertyVisitor;
import com.yahoo.squidb.sql.SqlStatement;
import com.yahoo.squidb.sql.SqlTable;
import com.yahoo.squidb.sql.Table;
import com.yahoo.squidb.sql.Update;
import com.yahoo.squidb.sql.View;
import com.yahoo.squidb.sql.VirtualTable;
import com.yahoo.squidb.utility.SquidCursorFactory;
import com.yahoo.squidb.utility.VersionCode;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * AbstractDatabase is a database abstraction which wraps a SQLite database.
 * <p>
 * Use this class to control the lifecycle of your database where you would normally use a {@link SQLiteOpenHelper}.
 * Call {@link #getDatabase()} to open the database and {@link #close()} to close the database. Direct querying is not
 * recommended for type safety reasons. Instead, use an instance of {@link DatabaseDao} to issue the request and return
 * a {@link SquidCursor}.
 * <p>
 * By convention, methods beginning with "try" (e.g. {@link #tryCreateTable(Table) tryCreateTable}) return true if the
 * operation succeeded and false if it failed for any reason. If it fails, there will also be a call to
 * {@link #onError(String, Throwable) onError}.
 * <p>
 * Methods that use String arrays for where clause arguments ({@link #update(String, ContentValues, String, String[])
 * update}, {@link #updateWithOnConflict(String, ContentValues, String, String[], int) updateWithOnConflict}, and
 * {@link #delete(String, String, String[]) delete}) are wrappers around Android's {@link SQLiteDatabase} methods, and
 * so are left intact. However, Android's default behavior of binding all arguments as strings can have unexpected
 * bugs, particularly when working with SQLite functions. For example:
 *
 * <pre>
 * select * from t where _id = '1'; // Returns the first row
 * select * from t where abs(_id) = '1'; // Always returns empty set
 * </pre>
 *
 * {@link DatabaseDao} contains workarounds for this behavior, so all users are encouraged to use DatabaseDao for these
 * operations. If you choose to call these methods directly on AbstractDatabase instead, you have been warned!
 */
public abstract class AbstractDatabase {

    /**
     * @return the database name
     */
    protected abstract String getName();

    /**
     * @return the database version
     */
    protected abstract int getVersion();

    /**
     * @return all {@link Table Tables} that should be created when the database is created
     */
    protected abstract Table[] getTables();

    /**
     * @return all {@link VirtualTable VirtualTables} that should be created when the database is created
     */
    protected VirtualTable[] getVirtualTables() {
        return null;
    }

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
     * Called after the database has been created. At this time, all {@link Table Tables} returned from
     * {@link #getTables()} and {@link #getVirtualTables()}, all {@link View Views} from {@link #getViews()}, and all
     * {@link Index Indexes} from {@link #getIndexes()} will have been created. Any additional database setup should be
     * done here, e.g. creating other views, indexes, triggers, or inserting data.
     *
     * @param db the {@link SQLiteDatabase} being created
     */
    protected void onTablesCreated(SQLiteDatabase db) {
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
     * @param db the {@link SQLiteDatabase} being upgraded
     * @param oldVersion the current database version
     * @param newVersion the database version being upgraded to
     * @return true if the upgrade was handled successfully, false otherwise
     */
    protected abstract boolean onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion);

    /**
     * Called when the database should be downgraded from one version to another
     *
     * @param db the {@link SQLiteDatabase} being upgraded
     * @param oldVersion the current database version
     * @param newVersion the database version being downgraded to
     * @return true if the downgrade was handled successfully, false otherwise. The default implementation returns true.
     */
    protected boolean onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        return true;
    }

    /**
     * Called to notify of a failure in {@link #onUpgrade(SQLiteDatabase, int, int) onUpgrade()} or
     * {@link #onDowngrade(SQLiteDatabase, int, int) onDowngrade()}, either because it returned false or because an
     * unexpected exception occurred. Subclasses can take drastic corrective action here, e.g. recreating the database
     * with {@link #recreate()}. The default implementation does nothing.
     * <p>
     * Note that taking no action here leaves the database in whatever state it was in when the error occurred, which
     * can result in unexpected errors if callers are allowed to invoke further operations on the database.
     *
     * @param oldVersion the current database version
     * @param newVersion the database version being upgraded to
     */
    protected void onMigrationFailed(int oldVersion, int newVersion) {
    }

    /**
     * Called when the database connection is being configured, to enable features such as write-ahead logging or
     * foreign key support. This method is called before {@link #onTablesCreated(SQLiteDatabase) onTablesCreated},
     * {@link #onUpgrade(SQLiteDatabase, int, int) onUpgrade}, {@link #onDowngrade(SQLiteDatabase, int, int)
     * onDowngrade}, and {@link #onOpen(SQLiteDatabase) onOpen}.
     * <p>
     * This method should only call methods that configure the parameters of the database connection, such as
     * {@link SQLiteDatabase#enableWriteAheadLogging} {@link SQLiteDatabase#setForeignKeyConstraintsEnabled},
     * {@link SQLiteDatabase#setLocale}, {@link SQLiteDatabase#setMaximumSize}, or executing PRAGMA statements.
     *
     * @param db the {@link SQLiteDatabase} being configured
     */
    protected void onConfigure(SQLiteDatabase db) {
    }

    /**
     * Called when the database has been opened. This method is called after the database connection has been
     * configured and after the database schema has been created, upgraded, or downgraded as necessary.
     *
     * @param db the {@link SQLiteDatabase} being opened
     */
    protected void onOpen(SQLiteDatabase db) {
    }

    /**
     * Called when an error occurs. This is primarily for clients to log notable errors, not for taking corrective
     * action on them. The default implementation prints a warning log.
     *
     * @param message an error message
     * @param error the error that was encountered
     */
    protected void onError(String message, Throwable error) {
        Log.w(getClass().getSimpleName(), message, error);
    }

    // --- internal implementation

    private static final int STRING_BUILDER_INITIAL_CAPACITY = 128;

    private final Context context;

    private AbstractDatabase attachedTo = null;
    private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();

    /**
     * SQLiteOpenHelper that takes care of database operations
     */
    private SQLiteOpenHelper helper = null;

    /**
     * Internal pointer to open database. Hides the fact that there is a database and a wrapper by making a single
     * monolithic interface
     */
    private SQLiteDatabase database = null;

    /**
     * Map of class objects to corresponding tables
     */
    private Map<Class<? extends AbstractModel>, SqlTable<?>> tableMap;

    private boolean isInMigration;

    /**
     * Create a new AbstractDatabase
     *
     * @param context the Context, must not be null
     */
    public AbstractDatabase(Context context) {
        if (context == null) {
            throw new NullPointerException("Null context creating AbstractDatabase");
        }
        this.context = context.getApplicationContext();
        initializeTableMap();
    }

    private void initializeTableMap() {
        tableMap = new HashMap<Class<? extends AbstractModel>, SqlTable<?>>();
        registerTableModels(getTables());
        registerTableModels(getVirtualTables());
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

    /**
     * @return the path to the underlying database file.
     */
    public String getDatabasePath() {
        return context.getDatabasePath(getName()).getAbsolutePath();
    }

    /**
     * Return the {@link SqlTable} corresponding to the specified model type
     *
     * @param modelType the model class
     * @return the corresponding table for the model
     * @throws UnsupportedOperationException if the model class is unknown to this database
     */
    public final SqlTable<?> getTable(Class<? extends AbstractModel> modelType) {
        Class<?> type = modelType;
        SqlTable<?> table;
        //noinspection SuspiciousMethodCalls
        while ((table = tableMap.get(type)) == null && type != AbstractModel.class && type != Object.class) {
            type = type.getSuperclass();
        }
        if (table != null) {
            return table;
        }
        throw new UnsupportedOperationException("Unknown model class " + modelType);
    }

    /**
     * @return the underlying {@link SQLiteDatabase}, which will be opened if it is not yet opened
     */
    protected synchronized final SQLiteDatabase getDatabase() {
        if (database == null) {
            openForWriting();
        }
        return database;
    }

    /**
     * Attaches another database to this database using the SQLite ATTACH command. For now,
     * a database can only be attached to one other database. This method will throw an exception
     * if the other database is already attached somewhere.
     * <p>
     * Caveats:
     * Make sure you call {@link #detachDatabase(AbstractDatabase)} when you are done! Otherwise, the other
     * database will not be unlocked.
     * <p>
     * If the database being attached is in a transaction that was started on this thread, an exception will be thrown.
     *
     * @return the alias used to attach the database; this can be used to qualify tables using
     * {@link Table#qualifiedFromDatabase(String)}
     */
    @Beta
    public final String attachDatabase(AbstractDatabase other) {
        if (attachedTo != null) {
            throw new IllegalStateException("Can't attach a database to a database that is itself attached");
        }

        return other.attachTo(this);
    }

    /**
     * Detaches a database previously attached with {@link #attachDatabase(AbstractDatabase)}
     *
     * @return true if the other database was successfully detached
     */
    @Beta
    public final boolean detachDatabase(AbstractDatabase other) {
        if (other.attachedTo != this) {
            throw new IllegalArgumentException("Database " + other.getName() + " is not attached to " + getName());
        }

        return other.detachFrom(this);
    }

    private String attachTo(AbstractDatabase attachTo) {
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

    private boolean detachFrom(AbstractDatabase detachFrom) {
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
        initializeHelper();

        boolean performRecreate = false;
        try {
            database = helper.getWritableDatabase();
        } catch (RecreateDuringMigrationException recreate) {
            performRecreate = true;
        } catch (MigrationFailedException fail) {
            onError(fail.getMessage(), fail);
            onMigrationFailed(fail.oldVersion, fail.newVersion);
        } catch (RuntimeException e) {
            onError("Failed to open database: " + getName(), e);
            throw e;
        }

        if (performRecreate) {
            recreate();
        }
    }

    private void initializeHelper() {
        if (helper == null) {
            helper = new DatabaseHelper(context, getName(), null, getVersion());
        }
    }

    /**
     * @return true if a connection to the {@link SQLiteDatabase} is open, false otherwise
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
        helper = null;
        database = null;
    }

    /**
     * Clear all data in the database.
     * <p>
     * WARNING: Any open database resources will be abruptly closed. Do not call this method if other threads may be
     * accessing the database. The existing database file will be deleted and all data will be lost.
     */
    public synchronized final void clear() {
        close();
        context.deleteDatabase(getName());
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
     * @return a {@link Cursor} containing results of the query
     */
    public Cursor rawQuery(String sql, Object[] sqlArgs) {
        acquireNonExclusiveLock();
        try {
            return getDatabase().rawQueryWithFactory(new SquidCursorFactory(sqlArgs), sql, null, null);
        } finally {
            releaseNonExclusiveLock();
        }
    }

    // For use only by DatabaseDao when validating queries
    SQLiteStatement compileStatement(String sql) {
        acquireNonExclusiveLock();
        try {
            return getDatabase().compileStatement(sql);
        } finally {
            releaseNonExclusiveLock();
        }
    }

    /**
     * @see android.database.sqlite.SQLiteDatabase#insert(String table, String nullColumnHack, ContentValues values)
     */
    public long insert(String table, String nullColumnHack, ContentValues values) {
        acquireNonExclusiveLock();
        try {
            return getDatabase().insertOrThrow(table, nullColumnHack, values);
        } finally {
            releaseNonExclusiveLock();
        }
    }

    /**
     * @see android.database.sqlite.SQLiteDatabase#insertWithOnConflict(String, String, android.content.ContentValues,
     * int)
     */
    public long insertWithOnConflict(String table, String nullColumnHack, ContentValues values, int conflictAlgorithm) {
        acquireNonExclusiveLock();
        try {
            return getDatabase().insertWithOnConflict(table, nullColumnHack, values, conflictAlgorithm);
        } finally {
            releaseNonExclusiveLock();
        }
    }

    /**
     * Execute a SQL {@link com.yahoo.squidb.sql.Update} statement
     *
     * @return the row id of the last row inserted on success, -1 on failure
     */
    public long insert(Insert insert) {
        CompiledStatement compiled = insert.compile();
        acquireNonExclusiveLock();
        try {
            SQLiteStatement statement = getDatabase().compileStatement(compiled.sql);
            SquidCursorFactory.bindArgumentsToProgram(statement, compiled.sqlArgs);
            return statement.executeInsert();
        } finally {
            releaseNonExclusiveLock();
        }
    }

    /**
     * See the note at the top of this file about the potential bugs when using String[] whereArgs
     *
     * @see android.database.sqlite.SQLiteDatabase#delete(String, String, String[])
     * conflictAlgorithm)
     */
    public int delete(String table, String whereClause, String[] whereArgs) {
        acquireNonExclusiveLock();
        try {
            return getDatabase().delete(table, whereClause, whereArgs);
        } finally {
            releaseNonExclusiveLock();
        }
    }

    /**
     * Execute a SQL {@link com.yahoo.squidb.sql.Delete} statement
     *
     * @return the number of rows deleted on success, -1 on failure
     */
    public int delete(Delete delete) {
        CompiledStatement compiled = delete.compile();
        acquireNonExclusiveLock();
        try {
            SQLiteStatement statement = getDatabase().compileStatement(compiled.sql);
            SquidCursorFactory.bindArgumentsToProgram(statement, compiled.sqlArgs);
            return statement.executeUpdateDelete();
        } finally {
            releaseNonExclusiveLock();
        }
    }

    /**
     * See the note at the top of this file about the potential bugs when using String[] whereArgs
     *
     * @see android.database.sqlite.SQLiteDatabase#update(String table, ContentValues values, String whereClause,
     * String[] whereArgs)
     */
    public int update(String table, ContentValues values, String whereClause, String[] whereArgs) {
        acquireNonExclusiveLock();
        try {
            return getDatabase().update(table, values, whereClause, whereArgs);
        } finally {
            releaseNonExclusiveLock();
        }
    }

    /**
     * See the note at the top of this file about the potential bugs when using String[] whereArgs
     *
     * @see android.database.sqlite.SQLiteDatabase#updateWithOnConflict(String table, ContentValues values, String
     * whereClause, String[] whereArgs, int conflictAlgorithm)
     */
    public int updateWithOnConflict(String table, ContentValues values, String whereClause, String[] whereArgs,
            int conflictAlgorithm) {
        acquireNonExclusiveLock();
        try {
            return getDatabase().updateWithOnConflict(table, values, whereClause, whereArgs, conflictAlgorithm);
        } finally {
            releaseNonExclusiveLock();
        }
    }

    /**
     * Execute a SQL {@link com.yahoo.squidb.sql.Update} statement
     *
     * @return the number of rows updated on success, -1 on failure
     */
    public int update(Update update) {
        CompiledStatement compiled = update.compile();
        acquireNonExclusiveLock();
        try {
            SQLiteStatement statement = getDatabase().compileStatement(compiled.sql);
            SquidCursorFactory.bindArgumentsToProgram(statement, compiled.sqlArgs);
            return statement.executeUpdateDelete();
        } finally {
            releaseNonExclusiveLock();
        }
    }

    /**
     * Begin a transaction. This acquires a non-exclusive lock.
     *
     * @see #acquireNonExclusiveLock()
     * @see android.database.sqlite.SQLiteDatabase#beginTransaction()
     */
    public void beginTransaction() {
        acquireNonExclusiveLock();
        getDatabase().beginTransaction();
    }

    /**
     * Begin a non-exclusive transaction. This acquires a non-exclusive lock.
     *
     * @see #acquireNonExclusiveLock()
     * @see android.database.sqlite.SQLiteDatabase#beginTransactionNonExclusive()
     */
    public void beginTransactionNonExclusive() {
        acquireNonExclusiveLock();
        getDatabase().beginTransactionNonExclusive();
    }

    /**
     * Begin a transaction with a listener. This acquires a non-exclusive lock.
     *
     * @param listener the transaction listener
     * @see #acquireNonExclusiveLock()
     * @see android.database.sqlite.SQLiteDatabase#beginTransactionWithListener(android.database.sqlite.SQLiteTransactionListener)
     */
    public void beginTransactionWithListener(SQLiteTransactionListener listener) {
        acquireNonExclusiveLock();
        getDatabase().beginTransactionWithListener(listener);
    }

    /**
     * Begin a non-exclusive transaction with a listener. This acquires a non-exclusive lock.
     *
     * @param listener the transaction listener
     * @see #acquireNonExclusiveLock()
     * @see android.database.sqlite.SQLiteDatabase#beginTransactionWithListenerNonExclusive(android.database.sqlite.SQLiteTransactionListener)
     */
    public void beginTransactionWithListenerNonExclusive(SQLiteTransactionListener listener) {
        acquireNonExclusiveLock();
        getDatabase().beginTransactionWithListenerNonExclusive(listener);
    }

    /**
     * Mark the current transaction as successful
     *
     * @see android.database.sqlite.SQLiteDatabase#setTransactionSuccessful()
     */
    public void setTransactionSuccessful() {
        getDatabase().setTransactionSuccessful();
    }

    /**
     * @return true if a transaction is active
     * @see android.database.sqlite.SQLiteDatabase#inTransaction()
     */
    public synchronized boolean inTransaction() {
        return database != null && database.inTransaction();
    }

    /**
     * End the current transaction
     *
     * @see android.database.sqlite.SQLiteDatabase#endTransaction()
     */
    public void endTransaction() {
        getDatabase().endTransaction();
        releaseNonExclusiveLock();
    }

    /**
     * Yield the current transaction
     *
     * @see android.database.sqlite.SQLiteDatabase#yieldIfContendedSafely()
     */
    public boolean yieldIfContendedSafely() {
        return getDatabase().yieldIfContendedSafely();
    }

    /**
     * Convenience method for calling {@link ContentResolver#notifyChange(Uri, android.database.ContentObserver)
     * ContentResolver.notifyChange(uri, null)}.
     *
     * @param uri the Uri to notify
     */
    public void notifyChange(Uri uri) {
        context.getContentResolver().notifyChange(uri, null);
    }

    /**
     * Convenience method for calling {@link ContentResolver#notifyChange(Uri, android.database.ContentObserver)
     * ContentResolver.notifyChange(uri, null)} on all the provided Uris.
     *
     * @param uris the Uris to notify
     */
    public void notifyChange(Collection<Uri> uris) {
        if (uris != null && !uris.isEmpty()) {
            ContentResolver resolver = context.getContentResolver();
            for (Uri uri : uris) {
                resolver.notifyChange(uri, null);
            }
        }
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
     * SQLiteOpenHelper implementation that takes care of creating tables and views on database creation. Also handles
     * upgrades by calling into abstract upgrade hooks implemented by concrete database class.
     */
    private class DatabaseHelper extends SQLiteOpenHelper {

        public DatabaseHelper(Context context, String name, CursorFactory factory, int version) {
            super(context, name, factory, version);
        }

        /**
         * Called to create the database tables
         */
        @Override
        public void onCreate(SQLiteDatabase db) {
            StringBuilder sql = new StringBuilder(STRING_BUILDER_INITIAL_CAPACITY);
            SqlConstructorVisitor sqlVisitor = new SqlConstructorVisitor();

            // create tables
            Table[] tables = getTables();
            if (tables != null) {
                for (Table table : tables) {
                    table.appendCreateTableSql(sql, sqlVisitor);
                    db.execSQL(sql.toString());
                    sql.setLength(0);
                }
            }

            VirtualTable[] virtualTables = getVirtualTables();
            if (virtualTables != null) {
                for (VirtualTable table : virtualTables) {
                    table.appendCreateTableSql(sql);
                    db.execSQL(sql.toString());
                    sql.setLength(0);
                }
            }

            View[] views = getViews();
            if (views != null) {
                for (View view : views) {
                    view.createViewSql(sql);
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
            database = db;
            onTablesCreated(db);
        }

        /**
         * Called to upgrade the database to a new version
         */
        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            database = db;
            boolean success = false;
            Throwable thrown = null;
            isInMigration = true;
            try {
                success = AbstractDatabase.this.onUpgrade(db, oldVersion, newVersion);
            } catch (Throwable t) {
                thrown = t;
                success = false;
            } finally {
                isInMigration = false;
            }

            if (thrown instanceof RecreateDuringMigrationException) {
                throw (RecreateDuringMigrationException) thrown;
            } else if (!success) {
                throw new MigrationFailedException(getName(), oldVersion, newVersion, thrown);
            }
        }

        /**
         * Called to downgrade the database to an older version
         */
        @Override
        public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            database = db;
            boolean success = false;
            Throwable thrown = null;
            isInMigration = true;
            try {
                success = AbstractDatabase.this.onDowngrade(db, oldVersion, newVersion);
            } catch (Throwable t) {
                thrown = t;
                success = false;
            } finally {
                isInMigration = false;
            }

            if (thrown instanceof RecreateDuringMigrationException) {
                throw (RecreateDuringMigrationException) thrown;
            } else if (!success) {
                throw new MigrationFailedException(getName(), oldVersion, newVersion, thrown);
            }
        }

        @Override
        public void onConfigure(SQLiteDatabase db) {
            database = db;
            AbstractDatabase.this.onConfigure(db);
        }

        @Override
        public void onOpen(SQLiteDatabase db) {
            database = db;
            AbstractDatabase.this.onOpen(db);
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
     * Create a new {@link Table} in the database
     *
     * @param table the Table to create
     * @return true if the statement executed without error, false otherwise
     */
    protected boolean tryCreateTable(Table table) {
        SqlConstructorVisitor sqlVisitor = new SqlConstructorVisitor();
        StringBuilder sql = new StringBuilder(STRING_BUILDER_INITIAL_CAPACITY);
        table.appendCreateTableSql(sql, sqlVisitor);
        return tryExecSql(sql.toString());
    }

    /**
     * Create a new {@link VirtualTable} in the database
     *
     * @param table the VirtualTable to create
     * @return true if the statement executed without error, false otherwise
     */
    protected boolean tryCreateTable(VirtualTable table) {
        StringBuilder sql = new StringBuilder(STRING_BUILDER_INITIAL_CAPACITY);
        table.appendCreateTableSql(sql);
        return tryExecSql(sql.toString());
    }

    /**
     * Drop a {@link Table} in the database if it exists
     *
     * @param table the Table to drop
     * @return true if the statement executed without error, false otherwise
     */
    protected boolean tryDropTable(Table table) {
        return tryDropTable(table.getExpression());
    }

    /**
     * Drop a {@link VirtualTable} in the database if it exists
     *
     * @param table the VirtualTable to drop
     * @return true if the statement executed without error, false otherwise
     */
    protected boolean tryDropTable(VirtualTable table) {
        return tryDropTable(table.getExpression());
    }

    private boolean tryDropTable(String tableName) {
        return tryExecSql("DROP TABLE IF EXISTS " + tableName);
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
        view.createViewSql(sql);
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
        CompiledStatement compiled = statement.compile();
        return tryExecSql(compiled.sql, compiled.sqlArgs);
    }

    /**
     * Execute a raw SQL statement
     *
     * @param sql the statement to execute
     * @return true if the statement executed without an error
     * @see android.database.sqlite.SQLiteDatabase#execSQL(String)
     */
    public boolean tryExecSql(String sql) {
        acquireNonExclusiveLock();
        try {
            getDatabase().execSQL(sql);
            return true;
        } catch (SQLException e) {
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
     * @throws SQLException if there is an error parsing the SQL or some other error
     * @see android.database.sqlite.SQLiteDatabase#execSQL(String)
     */
    public void execSqlOrThrow(String sql) throws SQLException {
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
     * @see android.database.sqlite.SQLiteDatabase#execSQL(String, Object[])
     */
    public boolean tryExecSql(String sql, Object[] bindArgs) {
        acquireNonExclusiveLock();
        try {
            getDatabase().execSQL(sql, bindArgs);
            return true;
        } catch (SQLException e) {
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
     * @throws SQLException if there is an error parsing the SQL or some other error
     * @see android.database.sqlite.SQLiteDatabase#execSQL(String, Object[])
     */
    public void execSqlOrThrow(String sql, Object[] bindArgs) throws SQLException {
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
    protected VersionCode getSqliteVersion() {
        acquireNonExclusiveLock();
        try {
            SQLiteStatement stmt = getDatabase().compileStatement("select sqlite_version()");
            String versionString = stmt.simpleQueryForString();
            return VersionCode.parse(versionString);
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
            if (!TextUtils.isEmpty(property.getColumnDefinition())) {
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

    private static class MigrationFailedException extends RuntimeException {

        /* suppress compiler warning */
        private static final long serialVersionUID = 2949995666882182744L;

        final String dbName;
        final int oldVersion;
        final int newVersion;

        public MigrationFailedException(String dbName, int oldVersion, int newVersion, Throwable throwable) {
            super(throwable);
            this.dbName = dbName;
            this.oldVersion = oldVersion;
            this.newVersion = newVersion;
        }

        @Override
        @SuppressLint("DefaultLocale")
        public String getMessage() {
            return String.format("Failed to migrate db \"%s\" from version %d to %d", dbName, oldVersion, newVersion);
        }

    }
}
