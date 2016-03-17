// Adapted from String16.h and Unicode.h found at:
// https://android.googlesource.com/platform/frameworks/native/+/jb-dev/include/utils/String16.h
// https://android.googlesource.com/platform/frameworks/native/+/jb-dev/include/utils/Unicode.h

#import <stdint.h>
#import "IOSPrimitiveArray.h"

IOSCharArray* allocFromUTF8(const char* u8str, size_t u8len);
