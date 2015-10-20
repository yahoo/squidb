//
//  CursorWindow.h
//  j2objc-squidb-experiments
//
//  Created by Sam Bosley on 10/16/15.
//  Copyright © 2015 Sam Bosley. All rights reserved.
//

#import <Foundation/Foundation.h>
#import "IOSPrimitiveArray.h"
#import "Errors.h"

static const uint32_t ROW_SLOT_CHUNK_NUM_ROWS = 100;

@interface CursorWindowNative : NSObject

enum {
    FIELD_TYPE_NULL = 0,
    FIELD_TYPE_INTEGER = 1,
    FIELD_TYPE_FLOAT = 2,
    FIELD_TYPE_STRING = 3,
    FIELD_TYPE_BLOB = 4,
};

struct Header {
    // Offset of the lowest unused byte in the window.
    uint32_t freeOffset;
    
    // Offset of the first row slot chunk.
    uint32_t firstChunkOffset;
    
    uint32_t numRows;
    uint32_t numColumns;
};

struct RowSlot {
    uint32_t offset;
};

struct RowSlotChunk {
    struct RowSlot slots[ROW_SLOT_CHUNK_NUM_ROWS];
    uint32_t nextChunkOffset;
};

struct FieldSlot {
    int32_t type;
    union {
        double d;
        int64_t l;
        struct {
            uint32_t offset;
            uint32_t size;
        } buffer;
    } data;
};

+ (NSObject *)nativeCreate:(NSString *)name cursorWindowSize:(int)cursorWindowSize;

+ (void) nativeDispose:(NSObject *)windowPtr;

+ (void) nativeClear:(NSObject *)windowPtr;

+ (int) nativeGetNumRows:(NSObject *)windowPtr;

+ (BOOL) nativeSetNumColumns:(NSObject *)windowPtr columnNum:(int)columnNum;

+ (BOOL) nativeAllocRow:(NSObject *)windowPtr;

+ (void) nativeFreeLastRow:(NSObject *)windowPtr;

+ (int) nativeGetType:(NSObject *)windowPtr row:(int) row column:(int)column;

+ (IOSByteArray *) nativeGetBlob:(NSObject *)windowPtr row:(int)row column:(int)column;

+ (NSString *) nativeGetString:(NSObject *)windowPtr row:(int) row column:(int) column;

+ (long) nativeGetLong:(NSObject *)windowPtr row:(int) row column:(int) column;

+ (double) nativeGetDouble:(NSObject *)windowPtr row:(int) row column:(int) column;

//private static native void nativeCopyStringToBuffer(Object windowPtr, int row, int column, CharArrayBuffer buffer);

+ (BOOL) nativePutBlob:(NSObject *)windowPtr value:(IOSByteArray *)value row:(int)row column:(int)column;

+ (BOOL) nativePutString:(NSObject *)windowPtr value:(NSString *)value row:(int)row column:(int)column;

+ (BOOL) nativePutLong:(NSObject *)windowPtr value:(long)value row:(int)row column:(int)column;

+ (BOOL) nativePutDouble:(NSObject *)windowPtr value:(double)value row:(int)row column:(int)column;

+ (BOOL) nativePutNull:(NSObject *)windowPtr row:(int)row column:(int)column;

@property NSString *mName;
@property uint32_t mSize;
@property BOOL mIsReadOnly;

- (id) initWithName:(NSString *)name size:(uint32_t)size isReadOnly:(BOOL)readOnly;

- (status_t) clear;

- (status_t) setNumColumns:(uint32_t)numColumns;

- (status_t) allocRow;

- (status_t) freeLastRow;

- (int) getNumRows;

- (int) getNumColumns;

- (struct RowSlot *) getRowSlot:(uint32_t)row;

- (struct RowSlot *)allocRowSlot;

- (struct FieldSlot *) getFieldSlot:(uint32_t)row column:(uint32_t)column;

- (status_t) putBlobInRow:(uint32_t)row column:(uint32_t)column value:(const void *)value size:(uint32_t)size;

- (status_t) putStringInRow:(uint32_t)row column:(uint32_t)column value:(const char *)value
                       size:(uint32_t)sizeIncludingNull;

- (status_t) putBlobOrStringInRow:(uint32_t)row column:(uint32_t) column
                            value:(const void *)value size:(uint32_t)size type:(int32_t)type;

- (status_t) putLongInRow:(uint32_t)row column:(uint32_t)column value:(int64_t)value;

- (status_t) putDoubleInRow:(uint32_t)row column:(uint32_t)column value:(double)value;

- (status_t) putNullInRow:(uint32_t)row column:(uint32_t)column;

@end
