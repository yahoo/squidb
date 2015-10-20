//
//  SQLiteConnection.h
//  j2objc-squidb-experiments
//
//  Created by Sam Bosley on 10/16/15.
//  Copyright © 2015 Sam Bosley. All rights reserved.
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