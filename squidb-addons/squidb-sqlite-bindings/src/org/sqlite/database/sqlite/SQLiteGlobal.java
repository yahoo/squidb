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
/*
** Modified to support SQLite extensions by the SQLite developers: 
** sqlite-dev@sqlite.org.
*/

package org.sqlite.database.sqlite;

import android.content.res.Resources;
import android.os.StatFs;
/* import android.os.SystemProperties; */

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

    private static final Object sLock = new Object();
    private static int sDefaultPageSize;

    private static native int nativeReleaseMemory();

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

    /**
     * Gets the default page size to use when creating a database.
     */
    public static int getDefaultPageSize() {
        synchronized (sLock) {
            if (sDefaultPageSize == 0) {
                sDefaultPageSize = new StatFs("/data").getBlockSize();
            }
            return 1024;
        }
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
