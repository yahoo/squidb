/*
 * Copyright (C) 2006-2007 The Android Open Source Project
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
//
//  SQLiteConnectionNative.h
//  squidb-ios
//  This file is a fork/port of AOSP SQLiteConnection.cpp (https://github.com/android/platform_frameworks_base/blob/master/core/jni/android_database_SQLiteConnection.cpp)
//  The core logic/structures defined in the file have been left intact; this is just a translation to use Objective-C
//  syntax instead of C++ to make working with the j2objc tool easier.
//

#import <sqlite3.h>
#import "IOSPrimitiveArray.h"

@interface SQLiteConnectionNative : NSObject

@property sqlite3 *db;
@property NSString *path;
@property NSString *label;
@property int openFlags;

+ (SQLiteConnectionNative *) nativeOpen:(NSString *)pathStr openFlags:(int) openFlags labelStr:(NSString *)labelStr enableTrace:(BOOL)enableTrace enableProfile:(BOOL)enableProfile;

+ (void) nativeClose:(NSObject *)connectionPtr;

+ (NSObject *) nativePrepareStatement:(NSObject *)connectionPtr withSql:(NSString *)sqlString;

+ (void) nativeFinalizeStatement:(NSObject *)connectionPtr statement:(NSObject *)statementPtr;

+ (int) nativeGetParameterCount:(NSObject *)connectionPtr statement:(NSObject *)statementPtr;

+ (BOOL) nativeIsReadOnly:(NSObject *)connectionPtr statement:(NSObject *)statementPtr;

+ (int) nativeGetColumnCount:(NSObject *)connectionPtr statement:(NSObject *)statementPtr;

+ (NSString *) nativeGetColumnName:(NSObject *)connectionPtr statement:(NSObject *)statementPtr index:(int)index;

+ (void) nativeBindNull:(NSObject *)connectionPtr statement:(NSObject *)statementPtr index:(int)index;

+ (void) nativeBindLong:(NSObject *)connectionPtr statement:(NSObject *)statementPtr index:(int)index value:(long)value;

+ (void) nativeBindDouble:(NSObject *)connectionPtr statement:(NSObject *)statementPtr index:(int)index value:(double)value;

+ (void) nativeBindBlob:(NSObject *)connectionPtr statement:(NSObject *)statementPtr index:(int)index value:(IOSByteArray *)value;

+ (void) nativeBindString:(NSObject *)connectionPtr statement:(NSObject *)statementPtr index:(int)index value:(NSString *)value;

+ (void) nativeResetStatementAndClearBindings:(NSObject *)connectionPtr statement:(NSObject *)statementPtr;

+ (void) nativeExecute:(NSObject *)connectionPtr statement:(NSObject *)statementPtr;

+ (long) nativeExecuteForLong:(NSObject *)connectionPtr statement:(NSObject *)statementPtr;

+ (NSString *) nativeExecuteForString:(NSObject *)connectionPtr statement:(NSObject *)statementPtr;

+ (int) nativeExecuteForChangedRowCount:(NSObject *)connectionPtr statement:(NSObject *)statementPtr;

+ (long) nativeExecuteForLastInsertedRowId:(NSObject *)connectionPtr statement:(NSObject *)statementPtr;

+ (long) nativeExecuteForCursorWindow:(NSObject *)connectionPtr statement:(NSObject *)statementPtr
                               window:(NSObject *)windowPtr startPos:(int)startPos requiredPos:(int)requiredPos
                               countAllRows:(BOOL)countAllRows;


+ (int) nativeGetDbLookaside:(NSObject *)connectionPtr;

@end
