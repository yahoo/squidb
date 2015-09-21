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

#define LOG_TAG "SQLiteConnection"

#include <jni.h>
#include <JNIHelp.h>
#include "ALog-priv.h"


#include <sys/mman.h>
#include <string.h>
#include <unistd.h>
#include <assert.h>

#if 0
#include <androidfw/CursorWindow.h>
#endif

#include <sqlite3.h>
#if 0
#include <sqlite3_android.h>
#endif

#include "android_database_SQLiteCommon.h"

#include <string>

// Set to 1 to use UTF16 storage for localized indexes.
#define UTF16_STORAGE 0

namespace android {

/* Busy timeout in milliseconds.
 * If another connection (possibly in another process) has the database locked for
 * longer than this amount of time then SQLite will generate a SQLITE_BUSY error.
 * The SQLITE_BUSY error is then raised as a SQLiteDatabaseLockedException.
 *
 * In ordinary usage, busy timeouts are quite rare.  Most databases only ever
 * have a single open connection at a time unless they are using WAL.  When using
 * WAL, a timeout could occur if one connection is busy performing an auto-checkpoint
 * operation.  The busy timeout needs to be long enough to tolerate slow I/O write
 * operations but not so long as to cause the application to hang indefinitely if
 * there is a problem acquiring a database lock.
 */
static const int BUSY_TIMEOUT_MS = 2500;

static JavaVM *gpJavaVM = 0;

static struct {
    jfieldID name;
    jfieldID numArgs;
    jmethodID dispatchCallback;
} gSQLiteCustomFunctionClassInfo;

static struct {
    jclass clazz;
} gStringClassInfo;

struct SQLiteConnection {
    // Open flags.
    // Must be kept in sync with the constants defined in SQLiteDatabase.java.
    enum {
        OPEN_READWRITE          = 0x00000000,
        OPEN_READONLY           = 0x00000001,
        OPEN_READ_MASK          = 0x00000001,
        NO_LOCALIZED_COLLATORS  = 0x00000010,
        CREATE_IF_NECESSARY     = 0x10000000,
    };

    sqlite3* const db;
    const int openFlags;
    std::string path;
    std::string label;

    volatile bool canceled;

    SQLiteConnection(sqlite3* db, int openFlags, const std::string& path, const std::string& label) :
        db(db), openFlags(openFlags), path(path), label(label), canceled(false) { }
};

// Called each time a statement begins execution, when tracing is enabled.
static void sqliteTraceCallback(void *data, const char *sql) {
    SQLiteConnection* connection = static_cast<SQLiteConnection*>(data);
    ALOG(LOG_VERBOSE, SQLITE_TRACE_TAG, "%s: \"%s\"\n",
            connection->label.c_str(), sql);
}

// Called each time a statement finishes execution, when profiling is enabled.
static void sqliteProfileCallback(void *data, const char *sql, sqlite3_uint64 tm) {
    SQLiteConnection* connection = static_cast<SQLiteConnection*>(data);
    ALOG(LOG_VERBOSE, SQLITE_PROFILE_TAG, "%s: \"%s\" took %0.3f ms\n",
            connection->label.c_str(), sql, tm * 0.000001f);
}

// Called after each SQLite VM instruction when cancelation is enabled.
static int sqliteProgressHandlerCallback(void* data) {
    SQLiteConnection* connection = static_cast<SQLiteConnection*>(data);
    return connection->canceled;
}

/*
** This function is a collation sequence callback equivalent to the built-in
** BINARY sequence. 
**
** Stock Android uses a modified version of sqlite3.c that calls out to a module
** named "sqlite3_android" to add extra built-in collations and functions to
** all database handles. Specifically, collation sequence "LOCALIZED". For now,
** this module does not include sqlite3_android (since it is difficult to build
** with the NDK only). Instead, this function is registered as "LOCALIZED" for all
** new database handles. 
*/
static int coll_localized(
  void *not_used,
  int nKey1, const void *pKey1,
  int nKey2, const void *pKey2
){
  int rc, n;
  n = nKey1<nKey2 ? nKey1 : nKey2;
  rc = memcmp(pKey1, pKey2, n);
  if( rc==0 ){
    rc = nKey1 - nKey2;
  }
  return rc;
}

static jlong nativeOpen(JNIEnv* env, jclass clazz, jstring pathStr, jint openFlags,
        jstring labelStr, jboolean enableTrace, jboolean enableProfile) {
    int sqliteFlags;
    if (openFlags & SQLiteConnection::CREATE_IF_NECESSARY) {
        sqliteFlags = SQLITE_OPEN_READWRITE | SQLITE_OPEN_CREATE;
    } else if (openFlags & SQLiteConnection::OPEN_READONLY) {
        sqliteFlags = SQLITE_OPEN_READONLY;
    } else {
        sqliteFlags = SQLITE_OPEN_READWRITE;
    }

    const char* pathChars = env->GetStringUTFChars(pathStr, NULL);
    std::string path(pathChars);
    env->ReleaseStringUTFChars(pathStr, pathChars);

    const char* labelChars = env->GetStringUTFChars(labelStr, NULL);
    std::string label(labelChars);
    env->ReleaseStringUTFChars(labelStr, labelChars);

    sqlite3* db;
    int err = sqlite3_open_v2(path.c_str(), &db, sqliteFlags, NULL);
    if (err != SQLITE_OK) {
        throw_sqlite3_exception_errcode(env, err, "Could not open database");
        return 0;
    }
    err = sqlite3_create_collation(db, "localized", SQLITE_UTF8, 0, coll_localized);
    if (err != SQLITE_OK) {
        throw_sqlite3_exception_errcode(env, err, "Could not register collation");
        sqlite3_close(db);
        return 0;
    }

    // Check that the database is really read/write when that is what we asked for.
    if ((sqliteFlags & SQLITE_OPEN_READWRITE) && sqlite3_db_readonly(db, NULL)) {
        throw_sqlite3_exception(env, db, "Could not open the database in read/write mode.");
        sqlite3_close(db);
        return 0;
    }

    // Set the default busy handler to retry automatically before returning SQLITE_BUSY.
    err = sqlite3_busy_timeout(db, BUSY_TIMEOUT_MS);
    if (err != SQLITE_OK) {
        throw_sqlite3_exception(env, db, "Could not set busy timeout");
        sqlite3_close(db);
        return 0;
    }

    // Register custom Android functions.
#if 0
    err = register_android_functions(db, UTF16_STORAGE);
    if (err) {
        throw_sqlite3_exception(env, db, "Could not register Android SQL functions.");
        sqlite3_close(db);
        return 0;
    }
#endif

    // Create wrapper object.
    SQLiteConnection* connection = new SQLiteConnection(db, openFlags, path, label);

    // Enable tracing and profiling if requested.
    if (enableTrace) {
        sqlite3_trace(db, &sqliteTraceCallback, connection);
    }
    if (enableProfile) {
        sqlite3_profile(db, &sqliteProfileCallback, connection);
    }

    ALOGV("Opened connection %p with label '%s'", db, label.c_str());
    return reinterpret_cast<jlong>(connection);
}

static void nativeClose(JNIEnv* env, jclass clazz, jlong connectionPtr) {
    SQLiteConnection* connection = reinterpret_cast<SQLiteConnection*>(connectionPtr);

    if (connection) {
        ALOGV("Closing connection %p", connection->db);
        int err = sqlite3_close(connection->db);
        if (err != SQLITE_OK) {
            // This can happen if sub-objects aren't closed first.  Make sure the caller knows.
            ALOGE("sqlite3_close(%p) failed: %d", connection->db, err);
            throw_sqlite3_exception(env, connection->db, "Count not close db.");
            return;
        }

        delete connection;
    }
}

// Called each time a custom function is evaluated.
static void sqliteCustomFunctionCallback(sqlite3_context *context,
        int argc, sqlite3_value **argv) {

    JNIEnv* env = 0;
    gpJavaVM->GetEnv((void**)&env, JNI_VERSION_1_4);

    // Get the callback function object.
    // Create a new local reference to it in case the callback tries to do something
    // dumb like unregister the function (thereby destroying the global ref) while it is running.
    jobject functionObjGlobal = reinterpret_cast<jobject>(sqlite3_user_data(context));
    jobject functionObj = env->NewLocalRef(functionObjGlobal);

    jobjectArray argsArray = env->NewObjectArray(argc, gStringClassInfo.clazz, NULL);
    if (argsArray) {
        for (int i = 0; i < argc; i++) {
            const jchar* arg = static_cast<const jchar*>(sqlite3_value_text16(argv[i]));
            if (!arg) {
                ALOGW("NULL argument in custom_function_callback.  This should not happen.");
            } else {
                size_t argLen = sqlite3_value_bytes16(argv[i]) / sizeof(jchar);
                jstring argStr = env->NewString(arg, argLen);
                if (!argStr) {
                    goto error; // out of memory error
                }
                env->SetObjectArrayElement(argsArray, i, argStr);
                env->DeleteLocalRef(argStr);
            }
        }

        // TODO: Support functions that return values.
        env->CallVoidMethod(functionObj,
                gSQLiteCustomFunctionClassInfo.dispatchCallback, argsArray);

error:
        env->DeleteLocalRef(argsArray);
    }

    env->DeleteLocalRef(functionObj);

    if (env->ExceptionCheck()) {
        ALOGE("An exception was thrown by custom SQLite function.");
        /* LOGE_EX(env); */
        env->ExceptionClear();
    }
}

// Called when a custom function is destroyed.
static void sqliteCustomFunctionDestructor(void* data) {
    jobject functionObjGlobal = reinterpret_cast<jobject>(data);
    JNIEnv* env = 0;
    gpJavaVM->GetEnv((void**)&env, JNI_VERSION_1_4);
    env->DeleteGlobalRef(functionObjGlobal);
}

static void nativeRegisterCustomFunction(JNIEnv* env, jclass clazz, jlong connectionPtr,
        jobject functionObj) {
    SQLiteConnection* connection = reinterpret_cast<SQLiteConnection*>(connectionPtr);

    jstring nameStr = jstring(env->GetObjectField(
            functionObj, gSQLiteCustomFunctionClassInfo.name));
    jint numArgs = env->GetIntField(functionObj, gSQLiteCustomFunctionClassInfo.numArgs);

    jobject functionObjGlobal = env->NewGlobalRef(functionObj);

    const char* name = env->GetStringUTFChars(nameStr, NULL);
    int err = sqlite3_create_function_v2(connection->db, name, numArgs, SQLITE_UTF16,
            reinterpret_cast<void*>(functionObjGlobal),
            &sqliteCustomFunctionCallback, NULL, NULL, &sqliteCustomFunctionDestructor);
    env->ReleaseStringUTFChars(nameStr, name);

    if (err != SQLITE_OK) {
        ALOGE("sqlite3_create_function returned %d", err);
        env->DeleteGlobalRef(functionObjGlobal);
        throw_sqlite3_exception(env, connection->db);
        return;
    }
}

static void nativeRegisterLocalizedCollators(JNIEnv* env, jclass clazz, jlong connectionPtr,
        jstring localeStr) {
    SQLiteConnection* connection = reinterpret_cast<SQLiteConnection*>(connectionPtr);

    const char* locale = env->GetStringUTFChars(localeStr, NULL);
#if 0
    int err = register_localized_collators(connection->db, locale, UTF16_STORAGE);
    env->ReleaseStringUTFChars(localeStr, locale);

    if (err != SQLITE_OK) {
        throw_sqlite3_exception(env, connection->db);
    }
#endif
}

static jlong nativePrepareStatement(JNIEnv* env, jclass clazz, jlong connectionPtr,
        jstring sqlString) {
    SQLiteConnection* connection = reinterpret_cast<SQLiteConnection*>(connectionPtr);

    jsize sqlLength = env->GetStringLength(sqlString);
    const jchar* sql = env->GetStringCritical(sqlString, NULL);
    sqlite3_stmt* statement;
    int err = sqlite3_prepare16_v2(connection->db,
            sql, sqlLength * sizeof(jchar), &statement, NULL);
    env->ReleaseStringCritical(sqlString, sql);

    if (err != SQLITE_OK) {
        // Error messages like 'near ")": syntax error' are not
        // always helpful enough, so construct an error string that
        // includes the query itself.
        const char *query = env->GetStringUTFChars(sqlString, NULL);
        char *message = (char*) malloc(strlen(query) + 50);
        if (message) {
            strcpy(message, ", while compiling: "); // less than 50 chars
            strcat(message, query);
        }
        env->ReleaseStringUTFChars(sqlString, query);
        throw_sqlite3_exception(env, connection->db, message);
        free(message);
        return 0;
    }

    ALOGV("Prepared statement %p on connection %p", statement, connection->db);
    return reinterpret_cast<jlong>(statement);
}

static void nativeFinalizeStatement(JNIEnv* env, jclass clazz, jlong connectionPtr,
        jlong statementPtr) {
    SQLiteConnection* connection = reinterpret_cast<SQLiteConnection*>(connectionPtr);
    sqlite3_stmt* statement = reinterpret_cast<sqlite3_stmt*>(statementPtr);

    // We ignore the result of sqlite3_finalize because it is really telling us about
    // whether any errors occurred while executing the statement.  The statement itself
    // is always finalized regardless.
    ALOGV("Finalized statement %p on connection %p", statement, connection->db);
    sqlite3_finalize(statement);
}

static jint nativeGetParameterCount(JNIEnv* env, jclass clazz, jlong connectionPtr,
        jlong statementPtr) {
    SQLiteConnection* connection = reinterpret_cast<SQLiteConnection*>(connectionPtr);
    sqlite3_stmt* statement = reinterpret_cast<sqlite3_stmt*>(statementPtr);

    return sqlite3_bind_parameter_count(statement);
}

static jboolean nativeIsReadOnly(JNIEnv* env, jclass clazz, jlong connectionPtr,
        jlong statementPtr) {
    SQLiteConnection* connection = reinterpret_cast<SQLiteConnection*>(connectionPtr);
    sqlite3_stmt* statement = reinterpret_cast<sqlite3_stmt*>(statementPtr);

    return sqlite3_stmt_readonly(statement) != 0;
}

static jint nativeGetColumnCount(JNIEnv* env, jclass clazz, jlong connectionPtr,
        jlong statementPtr) {
    SQLiteConnection* connection = reinterpret_cast<SQLiteConnection*>(connectionPtr);
    sqlite3_stmt* statement = reinterpret_cast<sqlite3_stmt*>(statementPtr);

    return sqlite3_column_count(statement);
}

static jstring nativeGetColumnName(JNIEnv* env, jclass clazz, jlong connectionPtr,
        jlong statementPtr, jint index) {
    SQLiteConnection* connection = reinterpret_cast<SQLiteConnection*>(connectionPtr);
    sqlite3_stmt* statement = reinterpret_cast<sqlite3_stmt*>(statementPtr);

    const jchar* name = static_cast<const jchar*>(sqlite3_column_name16(statement, index));
    if (name) {
        size_t length = 0;
        while (name[length]) {
            length += 1;
        }
        return env->NewString(name, length);
    }
    return NULL;
}

static void nativeBindNull(JNIEnv* env, jclass clazz, jlong connectionPtr,
        jlong statementPtr, jint index) {
    SQLiteConnection* connection = reinterpret_cast<SQLiteConnection*>(connectionPtr);
    sqlite3_stmt* statement = reinterpret_cast<sqlite3_stmt*>(statementPtr);

    int err = sqlite3_bind_null(statement, index);
    if (err != SQLITE_OK) {
        throw_sqlite3_exception(env, connection->db, NULL);
    }
}

static void nativeBindLong(JNIEnv* env, jclass clazz, jlong connectionPtr,
        jlong statementPtr, jint index, jlong value) {
    SQLiteConnection* connection = reinterpret_cast<SQLiteConnection*>(connectionPtr);
    sqlite3_stmt* statement = reinterpret_cast<sqlite3_stmt*>(statementPtr);

    int err = sqlite3_bind_int64(statement, index, value);
    if (err != SQLITE_OK) {
        throw_sqlite3_exception(env, connection->db, NULL);
    }
}

static void nativeBindDouble(JNIEnv* env, jclass clazz, jlong connectionPtr,
        jlong statementPtr, jint index, jdouble value) {
    SQLiteConnection* connection = reinterpret_cast<SQLiteConnection*>(connectionPtr);
    sqlite3_stmt* statement = reinterpret_cast<sqlite3_stmt*>(statementPtr);

    int err = sqlite3_bind_double(statement, index, value);
    if (err != SQLITE_OK) {
        throw_sqlite3_exception(env, connection->db, NULL);
    }
}

static void nativeBindString(JNIEnv* env, jclass clazz, jlong connectionPtr,
        jlong statementPtr, jint index, jstring valueString) {
    SQLiteConnection* connection = reinterpret_cast<SQLiteConnection*>(connectionPtr);
    sqlite3_stmt* statement = reinterpret_cast<sqlite3_stmt*>(statementPtr);

    jsize valueLength = env->GetStringLength(valueString);
    const jchar* value = env->GetStringCritical(valueString, NULL);
    int err = sqlite3_bind_text16(statement, index, value, valueLength * sizeof(jchar),
            SQLITE_TRANSIENT);
    env->ReleaseStringCritical(valueString, value);
    if (err != SQLITE_OK) {
        throw_sqlite3_exception(env, connection->db, NULL);
    }
}

static void nativeBindBlob(JNIEnv* env, jclass clazz, jlong connectionPtr,
        jlong statementPtr, jint index, jbyteArray valueArray) {
    SQLiteConnection* connection = reinterpret_cast<SQLiteConnection*>(connectionPtr);
    sqlite3_stmt* statement = reinterpret_cast<sqlite3_stmt*>(statementPtr);

    jsize valueLength = env->GetArrayLength(valueArray);
    jbyte* value = static_cast<jbyte*>(env->GetPrimitiveArrayCritical(valueArray, NULL));
    int err = sqlite3_bind_blob(statement, index, value, valueLength, SQLITE_TRANSIENT);
    env->ReleasePrimitiveArrayCritical(valueArray, value, JNI_ABORT);
    if (err != SQLITE_OK) {
        throw_sqlite3_exception(env, connection->db, NULL);
    }
}

static void nativeResetStatementAndClearBindings(JNIEnv* env, jclass clazz, jlong connectionPtr,
        jlong statementPtr) {
    SQLiteConnection* connection = reinterpret_cast<SQLiteConnection*>(connectionPtr);
    sqlite3_stmt* statement = reinterpret_cast<sqlite3_stmt*>(statementPtr);

    int err = sqlite3_reset(statement);
    if (err == SQLITE_OK) {
        err = sqlite3_clear_bindings(statement);
    }
    if (err != SQLITE_OK) {
        throw_sqlite3_exception(env, connection->db, NULL);
    }
}

static int executeNonQuery(JNIEnv* env, SQLiteConnection* connection, sqlite3_stmt* statement) {
    int err = sqlite3_step(statement);
    if (err == SQLITE_ROW) {
        throw_sqlite3_exception(env,
                "Queries can be performed using SQLiteDatabase query or rawQuery methods only.");
    } else if (err != SQLITE_DONE) {
        throw_sqlite3_exception(env, connection->db);
    }
    return err;
}

static void nativeExecute(JNIEnv* env, jclass clazz, jlong connectionPtr,
        jlong statementPtr) {
    SQLiteConnection* connection = reinterpret_cast<SQLiteConnection*>(connectionPtr);
    sqlite3_stmt* statement = reinterpret_cast<sqlite3_stmt*>(statementPtr);

    executeNonQuery(env, connection, statement);
}

static jint nativeExecuteForChangedRowCount(JNIEnv* env, jclass clazz,
        jlong connectionPtr, jlong statementPtr) {
    SQLiteConnection* connection = reinterpret_cast<SQLiteConnection*>(connectionPtr);
    sqlite3_stmt* statement = reinterpret_cast<sqlite3_stmt*>(statementPtr);

    int err = executeNonQuery(env, connection, statement);
    return err == SQLITE_DONE ? sqlite3_changes(connection->db) : -1;
}

static jlong nativeExecuteForLastInsertedRowId(JNIEnv* env, jclass clazz,
        jlong connectionPtr, jlong statementPtr) {
    SQLiteConnection* connection = reinterpret_cast<SQLiteConnection*>(connectionPtr);
    sqlite3_stmt* statement = reinterpret_cast<sqlite3_stmt*>(statementPtr);

    int err = executeNonQuery(env, connection, statement);
    return err == SQLITE_DONE && sqlite3_changes(connection->db) > 0
            ? sqlite3_last_insert_rowid(connection->db) : -1;
}

static int executeOneRowQuery(JNIEnv* env, SQLiteConnection* connection, sqlite3_stmt* statement) {
    int err = sqlite3_step(statement);
    if (err != SQLITE_ROW) {
        throw_sqlite3_exception(env, connection->db);
    }
    return err;
}

static jlong nativeExecuteForLong(JNIEnv* env, jclass clazz,
        jlong connectionPtr, jlong statementPtr) {
    SQLiteConnection* connection = reinterpret_cast<SQLiteConnection*>(connectionPtr);
    sqlite3_stmt* statement = reinterpret_cast<sqlite3_stmt*>(statementPtr);

    int err = executeOneRowQuery(env, connection, statement);
    if (err == SQLITE_ROW && sqlite3_column_count(statement) >= 1) {
        return sqlite3_column_int64(statement, 0);
    }
    return -1;
}

static jstring nativeExecuteForString(JNIEnv* env, jclass clazz,
        jlong connectionPtr, jlong statementPtr) {
    SQLiteConnection* connection = reinterpret_cast<SQLiteConnection*>(connectionPtr);
    sqlite3_stmt* statement = reinterpret_cast<sqlite3_stmt*>(statementPtr);

    int err = executeOneRowQuery(env, connection, statement);
    if (err == SQLITE_ROW && sqlite3_column_count(statement) >= 1) {
        const jchar* text = static_cast<const jchar*>(sqlite3_column_text16(statement, 0));
        if (text) {
            size_t length = sqlite3_column_bytes16(statement, 0) / sizeof(jchar);
            return env->NewString(text, length);
        }
    }
    return NULL;
}

static int createAshmemRegionWithData(JNIEnv* env, const void* data, size_t length) {
#if 0
    int error = 0;
    int fd = ashmem_create_region(NULL, length);
    if (fd < 0) {
        error = errno;
        ALOGE("ashmem_create_region failed: %s", strerror(error));
    } else {
        if (length > 0) {
            void* ptr = mmap(NULL, length, PROT_READ | PROT_WRITE, MAP_SHARED, fd, 0);
            if (ptr == MAP_FAILED) {
                error = errno;
                ALOGE("mmap failed: %s", strerror(error));
            } else {
                memcpy(ptr, data, length);
                munmap(ptr, length);
            }
        }

        if (!error) {
            if (ashmem_set_prot_region(fd, PROT_READ) < 0) {
                error = errno;
                ALOGE("ashmem_set_prot_region failed: %s", strerror(errno));
            } else {
                return fd;
            }
        }

        close(fd);
    }

#endif
    jniThrowIOException(env, -1);
    return -1;
}

static jint nativeExecuteForBlobFileDescriptor(JNIEnv* env, jclass clazz,
        jlong connectionPtr, jlong statementPtr) {
    SQLiteConnection* connection = reinterpret_cast<SQLiteConnection*>(connectionPtr);
    sqlite3_stmt* statement = reinterpret_cast<sqlite3_stmt*>(statementPtr);

    int err = executeOneRowQuery(env, connection, statement);
    if (err == SQLITE_ROW && sqlite3_column_count(statement) >= 1) {
        const void* blob = sqlite3_column_blob(statement, 0);
        if (blob) {
            int length = sqlite3_column_bytes(statement, 0);
            if (length >= 0) {
                return createAshmemRegionWithData(env, blob, length);
            }
        }
    }
    return -1;
}

/*
** Note: The following symbols must be in the same order as the corresponding
** elements in the aMethod[] array in function nativeExecuteForCursorWindow().
*/
enum CWMethodNames {
  CW_CLEAR         = 0,
  CW_SETNUMCOLUMNS = 1,
  CW_ALLOCROW      = 2,
  CW_FREELASTROW   = 3,
  CW_PUTNULL       = 4,
  CW_PUTLONG       = 5,
  CW_PUTDOUBLE     = 6,
  CW_PUTSTRING     = 7,
  CW_PUTBLOB       = 8
};

/*
** An instance of this structure represents a single CursorWindow java method.
*/
struct CWMethod {
  jmethodID id;                   /* Method id */
  const char *zName;              /* Method name */
  const char *zSig;               /* Method JNI signature */
};

/*
** Append the contents of the row that SQL statement pStmt currently points to
** to the CursorWindow object passed as the second argument. The CursorWindow
** currently contains iRow rows. Return true on success or false if an error
** occurs.
*/
static jboolean copyRowToWindow(
  JNIEnv *pEnv,
  jobject win,
  int iRow,
  sqlite3_stmt *pStmt,
  CWMethod *aMethod
){
  int nCol = sqlite3_column_count(pStmt);
  int i;
  jboolean bOk;

  bOk = pEnv->CallBooleanMethod(win, aMethod[CW_ALLOCROW].id);
  for(i=0; bOk && i<nCol; i++){
    switch( sqlite3_column_type(pStmt, i) ){
      case SQLITE_NULL: {
        bOk = pEnv->CallBooleanMethod(win, aMethod[CW_PUTNULL].id, iRow, i);
        break;
      }

      case SQLITE_INTEGER: {
        jlong val = sqlite3_column_int64(pStmt, i);
        bOk = pEnv->CallBooleanMethod(win, aMethod[CW_PUTLONG].id, val, iRow, i);
        break;
      }

      case SQLITE_FLOAT: {
        jdouble val = sqlite3_column_double(pStmt, i);
        bOk = pEnv->CallBooleanMethod(win, aMethod[CW_PUTDOUBLE].id, val, iRow, i);
        break;
      }

      case SQLITE_TEXT: {
        jchar *pStr = (jchar*)sqlite3_column_text16(pStmt, i);
        int nStr = sqlite3_column_bytes16(pStmt, i) / sizeof(jchar);
        jstring val = pEnv->NewString(pStr, nStr);
        bOk = pEnv->CallBooleanMethod(win, aMethod[CW_PUTSTRING].id, val, iRow, i);
        pEnv->DeleteLocalRef(val);
        break;
      }

      default: {
        assert( sqlite3_column_type(pStmt, i)==SQLITE_BLOB );
        const jbyte *p = (const jbyte*)sqlite3_column_blob(pStmt, i);
        int n = sqlite3_column_bytes(pStmt, i);
        jbyteArray val = pEnv->NewByteArray(n);
        pEnv->SetByteArrayRegion(val, 0, n, p);
        bOk = pEnv->CallBooleanMethod(win, aMethod[CW_PUTBLOB].id, val, iRow, i);
        pEnv->DeleteLocalRef(val);
        break;
      }
    }

    if( bOk==0 ){
      pEnv->CallVoidMethod(win, aMethod[CW_FREELASTROW].id);
    }
  }

  return bOk;
}

static jboolean setWindowNumColumns(
  JNIEnv *pEnv,
  jobject win,
  sqlite3_stmt *pStmt,
  CWMethod *aMethod
){
  int nCol;

  pEnv->CallVoidMethod(win, aMethod[CW_CLEAR].id);
  nCol = sqlite3_column_count(pStmt);
  return pEnv->CallBooleanMethod(win, aMethod[CW_SETNUMCOLUMNS].id, (jint)nCol);
}

/*
** This method has been rewritten for org.sqlite.database.*. The original 
** android implementation used the C++ interface to populate a CursorWindow
** object. Since the NDK does not export this interface, we invoke the Java
** interface using standard JNI methods to do the same thing.
**
** This function executes the SQLite statement object passed as the 4th 
** argument and copies one or more returned rows into the CursorWindow
** object passed as the 5th argument. The set of rows copied into the 
** CursorWindow is always contiguous.
**
** The only row that *must* be copied into the CursorWindow is row 
** iRowRequired. Ideally, all rows from iRowStart through to the end
** of the query are copied into the CursorWindow. If this is not possible
** (CursorWindow objects have a finite capacity), some compromise position
** is found (see comments embedded in the code below for details).
**
** The return value is a 64-bit integer calculated as follows:
**
**      (iStart << 32) | nRow
**
** where iStart is the index of the first row copied into the CursorWindow.
** If the countAllRows argument is true, nRow is the total number of rows
** returned by the query. Otherwise, nRow is one greater than the index of 
** the last row copied into the CursorWindow.
*/
static jlong nativeExecuteForCursorWindow(
  JNIEnv *pEnv, 
  jclass clazz,
  jlong connectionPtr,            /* Pointer to SQLiteConnection C++ object */
  jlong statementPtr,             /* Pointer to sqlite3_stmt object */
  jobject win,                    /* The CursorWindow object to populate */
  jint startPos,                  /* First row to add (advisory) */
  jint iRowRequired,              /* Required row */
  jboolean countAllRows
) {
  SQLiteConnection *pConnection = reinterpret_cast<SQLiteConnection*>(connectionPtr);
  sqlite3_stmt *pStmt = reinterpret_cast<sqlite3_stmt*>(statementPtr);

  CWMethod aMethod[] = {
    {0, "clear",         "()V"},
    {0, "setNumColumns", "(I)Z"},
    {0, "allocRow",      "()Z"},
    {0, "freeLastRow",   "()V"},
    {0, "putNull",       "(II)Z"},
    {0, "putLong",       "(JII)Z"},
    {0, "putDouble",     "(DII)Z"},
    {0, "putString",     "(Ljava/lang/String;II)Z"},
    {0, "putBlob",       "([BII)Z"},
  };
  jclass cls;                     /* Class android.database.CursorWindow */
  int i;                          /* Iterator variable */
  int nCol;                       /* Number of columns returned by pStmt */
  int nRow;
  jboolean bOk;
  int iStart;                     /* First row copied to CursorWindow */

  /* Locate all required CursorWindow methods. */
  cls = pEnv->FindClass("android/database/CursorWindow");
  for(i=0; i<(sizeof(aMethod)/sizeof(struct CWMethod)); i++){
    aMethod[i].id = pEnv->GetMethodID(cls, aMethod[i].zName, aMethod[i].zSig);
    if( aMethod[i].id==NULL ){
      jniThrowExceptionFmt(pEnv, "java/lang/Exception", 
          "Failed to find method CursorWindow.%s()", aMethod[i].zName
      );
      return 0;
    }
  }


  /* Set the number of columns in the window */
  bOk = setWindowNumColumns(pEnv, win, pStmt, aMethod);
  if( bOk==0 ) return 0;

  nRow = 0;
  iStart = startPos;
  while( sqlite3_step(pStmt)==SQLITE_ROW ){
    /* Only copy in rows that occur at or after row index iStart. */
    if( nRow>=iStart && bOk ){
      bOk = copyRowToWindow(pEnv, win, (nRow - iStart), pStmt, aMethod);
      if( bOk==0 ){
        /* The CursorWindow object ran out of memory. If row iRowRequired was
        ** not successfully added before this happened, clear the CursorWindow
        ** and try to add the current row again.  */
        if( nRow<=iRowRequired ){
          bOk = setWindowNumColumns(pEnv, win, pStmt, aMethod);
          if( bOk==0 ){
            sqlite3_reset(pStmt);
            return 0;
          }
          iStart = nRow;
          bOk = copyRowToWindow(pEnv, win, (nRow - iStart), pStmt, aMethod);
        }

        /* If the CursorWindow is still full and the countAllRows flag is not
        ** set, break out of the loop here. If countAllRows is set, continue
        ** so as to set variable nRow correctly.  */
        if( bOk==0 && countAllRows==0 ) break;
      }
    }

    nRow++;
  }

  /* Finalize the statement. If this indicates an error occurred, throw an
  ** SQLiteException exception.  */
  int rc = sqlite3_reset(pStmt);
  if( rc!=SQLITE_OK ){
    throw_sqlite3_exception(pEnv, sqlite3_db_handle(pStmt));
    return 0;
  }

  jlong lRet = jlong(iStart) << 32 | jlong(nRow);
  return lRet;
}

static jint nativeGetDbLookaside(JNIEnv* env, jobject clazz, jlong connectionPtr) {
    SQLiteConnection* connection = reinterpret_cast<SQLiteConnection*>(connectionPtr);

    int cur = -1;
    int unused;
    sqlite3_db_status(connection->db, SQLITE_DBSTATUS_LOOKASIDE_USED, &cur, &unused, 0);
    return cur;
}

static void nativeCancel(JNIEnv* env, jobject clazz, jlong connectionPtr) {
    SQLiteConnection* connection = reinterpret_cast<SQLiteConnection*>(connectionPtr);
    connection->canceled = true;
}

static void nativeResetCancel(JNIEnv* env, jobject clazz, jlong connectionPtr,
        jboolean cancelable) {
    SQLiteConnection* connection = reinterpret_cast<SQLiteConnection*>(connectionPtr);
    connection->canceled = false;

    if (cancelable) {
        sqlite3_progress_handler(connection->db, 4, sqliteProgressHandlerCallback,
                connection);
    } else {
        sqlite3_progress_handler(connection->db, 0, NULL, NULL);
    }
}

static jboolean nativeHasCodec(JNIEnv* env, jobject clazz){
#ifdef SQLITE_HAS_CODEC
  return true;
#else
  return false;
#endif
}


static JNINativeMethod sMethods[] =
{
    /* name, signature, funcPtr */
    { "nativeOpen", "(Ljava/lang/String;ILjava/lang/String;ZZ)J",
            (void*)nativeOpen },
    { "nativeClose", "(J)V",
            (void*)nativeClose },
    { "nativeRegisterCustomFunction", "(JLorg/sqlite/database/sqlite/SQLiteCustomFunction;)V",
            (void*)nativeRegisterCustomFunction },
    { "nativeRegisterLocalizedCollators", "(JLjava/lang/String;)V",
            (void*)nativeRegisterLocalizedCollators },
    { "nativePrepareStatement", "(JLjava/lang/String;)J",
            (void*)nativePrepareStatement },
    { "nativeFinalizeStatement", "(JJ)V",
            (void*)nativeFinalizeStatement },
    { "nativeGetParameterCount", "(JJ)I",
            (void*)nativeGetParameterCount },
    { "nativeIsReadOnly", "(JJ)Z",
            (void*)nativeIsReadOnly },
    { "nativeGetColumnCount", "(JJ)I",
            (void*)nativeGetColumnCount },
    { "nativeGetColumnName", "(JJI)Ljava/lang/String;",
            (void*)nativeGetColumnName },
    { "nativeBindNull", "(JJI)V",
            (void*)nativeBindNull },
    { "nativeBindLong", "(JJIJ)V",
            (void*)nativeBindLong },
    { "nativeBindDouble", "(JJID)V",
            (void*)nativeBindDouble },
    { "nativeBindString", "(JJILjava/lang/String;)V",
            (void*)nativeBindString },
    { "nativeBindBlob", "(JJI[B)V",
            (void*)nativeBindBlob },
    { "nativeResetStatementAndClearBindings", "(JJ)V",
            (void*)nativeResetStatementAndClearBindings },
    { "nativeExecute", "(JJ)V",
            (void*)nativeExecute },
    { "nativeExecuteForLong", "(JJ)J",
            (void*)nativeExecuteForLong },
    { "nativeExecuteForString", "(JJ)Ljava/lang/String;",
            (void*)nativeExecuteForString },
    { "nativeExecuteForBlobFileDescriptor", "(JJ)I",
            (void*)nativeExecuteForBlobFileDescriptor },
    { "nativeExecuteForChangedRowCount", "(JJ)I",
            (void*)nativeExecuteForChangedRowCount },
    { "nativeExecuteForLastInsertedRowId", "(JJ)J",
            (void*)nativeExecuteForLastInsertedRowId },
    { "nativeExecuteForCursorWindow", "(JJLandroid/database/CursorWindow;IIZ)J",
            (void*)nativeExecuteForCursorWindow },
    { "nativeGetDbLookaside", "(J)I",
            (void*)nativeGetDbLookaside },
    { "nativeCancel", "(J)V",
            (void*)nativeCancel },
    { "nativeResetCancel", "(JZ)V",
            (void*)nativeResetCancel },

    { "nativeHasCodec", "()Z", (void*)nativeHasCodec },
};

#define FIND_CLASS(var, className) \
        var = env->FindClass(className); \
        LOG_FATAL_IF(! var, "Unable to find class " className);

#define GET_METHOD_ID(var, clazz, methodName, fieldDescriptor) \
        var = env->GetMethodID(clazz, methodName, fieldDescriptor); \
        LOG_FATAL_IF(! var, "Unable to find method" methodName);

#define GET_FIELD_ID(var, clazz, fieldName, fieldDescriptor) \
        var = env->GetFieldID(clazz, fieldName, fieldDescriptor); \
        LOG_FATAL_IF(! var, "Unable to find field " fieldName);

int register_android_database_SQLiteConnection(JNIEnv *env)
{
    jclass clazz;
    FIND_CLASS(clazz, "org/sqlite/database/sqlite/SQLiteCustomFunction");

    GET_FIELD_ID(gSQLiteCustomFunctionClassInfo.name, clazz,
            "name", "Ljava/lang/String;");
    GET_FIELD_ID(gSQLiteCustomFunctionClassInfo.numArgs, clazz,
            "numArgs", "I");
    GET_METHOD_ID(gSQLiteCustomFunctionClassInfo.dispatchCallback,
            clazz, "dispatchCallback", "([Ljava/lang/String;)V");

    FIND_CLASS(clazz, "java/lang/String");
    gStringClassInfo.clazz = jclass(env->NewGlobalRef(clazz));

    return jniRegisterNativeMethods(env, 
        "org/sqlite/database/sqlite/SQLiteConnection",
        sMethods, NELEM(sMethods)
    );
}

extern int register_android_database_SQLiteGlobal(JNIEnv *env);
extern int register_android_database_SQLiteDebug(JNIEnv *env);

} // namespace android

extern "C" JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {
  JNIEnv *env = 0;

  android::gpJavaVM = vm;
  vm->GetEnv((void**)&env, JNI_VERSION_1_4);

  android::register_android_database_SQLiteConnection(env);
  android::register_android_database_SQLiteDebug(env);
  android::register_android_database_SQLiteGlobal(env);

  return JNI_VERSION_1_4;
}



