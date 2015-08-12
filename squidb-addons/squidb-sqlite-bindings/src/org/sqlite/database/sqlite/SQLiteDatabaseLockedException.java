/*
 * Copyright (C) 2010 The Android Open Source Project
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

/**
 * Thrown if  the database engine was unable to acquire the
 * database locks it needs to do its job.  If the statement is a [COMMIT]
 * or occurs outside of an explicit transaction, then you can retry the
 * statement.  If the statement is not a [COMMIT] and occurs within a
 * explicit transaction then you should rollback the transaction before
 * continuing.
 */
public class SQLiteDatabaseLockedException extends SQLiteException {
    public SQLiteDatabaseLockedException() {}

    public SQLiteDatabaseLockedException(String error) {
        super(error);
    }
}
