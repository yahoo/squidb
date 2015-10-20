//
//  Errors.h
//  j2objc-squidb-experiments
//
//  Created by Sam Bosley on 10/19/15.
//  Copyright © 2015 Sam Bosley. All rights reserved.
//

#import <sqlite3.h>

typedef int32_t     status_t;

enum {
    OK                = 0,    // Everything's swell.
    NO_ERROR          = 0,    // No errors.
    
    UNKNOWN_ERROR       = 0x80000000,
    
    NO_MEMORY           = -ENOMEM,
    INVALID_OPERATION   = -ENOSYS,
    BAD_VALUE           = -EINVAL,
    BAD_TYPE            = 0x80000001,
    NAME_NOT_FOUND      = -ENOENT,
    PERMISSION_DENIED   = -EPERM,
    NO_INIT             = -ENODEV,
    ALREADY_EXISTS      = -EEXIST,
    DEAD_OBJECT         = -EPIPE,
    FAILED_TRANSACTION  = 0x80000002,
    JPARKS_BROKE_IT     = -EPIPE,
#if !defined(HAVE_MS_C_RUNTIME)
    BAD_INDEX           = -EOVERFLOW,
    NOT_ENOUGH_DATA     = -ENODATA,
    WOULD_BLOCK         = -EWOULDBLOCK,
    TIMED_OUT           = -ETIMEDOUT,
    UNKNOWN_TRANSACTION = -EBADMSG,
#else
    BAD_INDEX           = -E2BIG,
    NOT_ENOUGH_DATA     = 0x80000003,
    WOULD_BLOCK         = 0x80000004,
    TIMED_OUT           = 0x80000005,
    UNKNOWN_TRANSACTION = 0x80000006,
#endif
};

/* throw a SQLiteException with a message appropriate for the error in handle */
void throw_sqlite3_exception_handle(sqlite3* handle);

/* throw a SQLiteException with a message appropriate for the error in handle
 concatenated with the given message
 */
void throw_sqlite3_exception_message(sqlite3* handle, const char* message);

/* throw a SQLiteException for a given error code */
void throw_sqlite3_exception_errcode(int errcode, const char* message);

void throw_sqlite3_exception(int errcode, const char* sqlite3Message, const char* message);
