/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.yahoo.android.sqlite;

/*-[
#import <sqlite3.h>

#define SOFT_HEAP_LIMIT 8 * 1024 * 1024
]-*/

/**
 * Provides access to SQLite functions that affect all database connection,
 * such as memory management.
 *
 * The native code associated with SQLiteGlobal is also sets global configuration options
 * using sqlite3_config() then calls sqlite3_initialize() to ensure that the SQLite
 * library is properly initialized exactly once before any other framework or application
 * code has a chance to run.
 *
 * Verbose SQLite logging is enabled if the "log.tag.SQLiteLog" property is set to "V".
 * (per {@link SQLiteDebug#DEBUG_SQL_LOG}).
 *
 * @hide
 */
public final class SQLiteGlobal {

    private static final String TAG = "SQLiteGlobal";

//    private static final Object sLock = new Object();
//    private static int sDefaultPageSize;

    public static native void sqlite3_initialize() /*-[
        // Enable multi-threaded mode.  In this mode, SQLite is safe to use by multiple
        // threads as long as no two threads use the same database connection at the same
        // time (which we guarantee in the SQLite database wrappers).
        sqlite3_config(SQLITE_CONFIG_MULTITHREAD);

        // The soft heap limit prevents the page cache allocations from growing
        // beyond the given limit, no matter what the max page cache sizes are
        // set to. The limit does not, as of 3.5.0, affect any other allocations.
        sqlite3_soft_heap_limit(SOFT_HEAP_LIMIT);

        // Initialize SQLite.
        sqlite3_initialize();
    ]-*/;

    private static native int nativeReleaseMemory() /*-[
        return sqlite3_release_memory(SOFT_HEAP_LIMIT);
    ]-*/;

    private SQLiteGlobal() {
    }

    /**
     * Attempts to release memory by pruning the SQLite page cache and other
     * internal data structures.
     *
     * @return The number of bytes that were freed.
     */
    public static int releaseMemory() {
        return nativeReleaseMemory();
    }
//
//    /**
//     * Gets the default page size to use when creating a database.
//     */
//    public static int getDefaultPageSize() {
//        synchronized (sLock) {
//            if (sDefaultPageSize == 0) {
//                sDefaultPageSize = new StatFs("/data").getBlockSize();
//            }
//            return SystemProperties.getInt("debug.sqlite.pagesize", sDefaultPageSize);
//        }
//    }
//
//    /**
//     * Gets the default journal mode when WAL is not in use.
//     */
//    public static String getDefaultJournalMode() {
//        return SystemProperties.get("debug.sqlite.journalmode",
//                Resources.getSystem().getString(
//                        com.android.internal.R.string.db_default_journal_mode));
//    }
//
//    /**
//     * Gets the journal size limit in bytes.
//     */
//    public static int getJournalSizeLimit() {
//        return SystemProperties.getInt("debug.sqlite.journalsizelimit",
//                Resources.getSystem().getInteger(
//                        com.android.internal.R.integer.db_journal_size_limit));
//    }
//
//    /**
//     * Gets the default database synchronization mode when WAL is not in use.
//     */
//    public static String getDefaultSyncMode() {
//        return SystemProperties.get("debug.sqlite.syncmode",
//                Resources.getSystem().getString(
//                        com.android.internal.R.string.db_default_sync_mode));
//    }
//
//    /**
//     * Gets the database synchronization mode when in WAL mode.
//     */
//    public static String getWALSyncMode() {
//        return SystemProperties.get("debug.sqlite.wal.syncmode",
//                Resources.getSystem().getString(
//                        com.android.internal.R.string.db_wal_sync_mode));
//    }
//
//    /**
//     * Gets the WAL auto-checkpoint integer in database pages.
//     */
//    public static int getWALAutoCheckpoint() {
//        int value = SystemProperties.getInt("debug.sqlite.wal.autocheckpoint",
//                Resources.getSystem().getInteger(
//                        com.android.internal.R.integer.db_wal_autocheckpoint));
//        return Math.max(1, value);
//    }
//
//    /**
//     * Gets the connection pool size when in WAL mode.
//     */
//    public static int getWALConnectionPoolSize() {
//        int value = SystemProperties.getInt("debug.sqlite.wal.poolsize",
//                Resources.getSystem().getInteger(
//                        com.android.internal.R.integer.db_connection_pool_size));
//        return Math.max(2, value);
//    }

    /**
     * Gets the default page size to use when creating a database.
     */
    public static int getDefaultPageSize() {
//        synchronized (sLock) {
//            if (sDefaultPageSize == 0) {
//                sDefaultPageSize = new StatFs("/data").getBlockSize();
//            }
        return 1024;
//        }
    }

    /**
     * Gets the default journal mode when WAL is not in use.
     */
    public static String getDefaultJournalMode() {
        return "delete";
    }

    /**
     * Gets the journal size limit in bytes.
     */
    public static int getJournalSizeLimit() {
        return 10000;
    }

    /**
     * Gets the default database synchronization mode when WAL is not in use.
     */
    public static String getDefaultSyncMode() {
        return "normal";
    }

    /**
     * Gets the database synchronization mode when in WAL mode.
     */
    public static String getWALSyncMode() {
        return "normal";
    }

    /**
     * Gets the WAL auto-checkpoint integer in database pages.
     */
    public static int getWALAutoCheckpoint() {
        int value = 1000;
        return Math.max(1, value);
    }

    /**
     * Gets the connection pool size when in WAL mode.
     */
    public static int getWALConnectionPoolSize() {
        int value = 10;
        return Math.max(2, value);
    }
}
