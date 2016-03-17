// Adapted from String16.h and Unicode.h found at:
// https://android.googlesource.com/platform/frameworks/native/+/jb-dev/include/utils/String16.h
// https://android.googlesource.com/platform/frameworks/native/+/jb-dev/include/utils/Unicode.h

#import <stdint.h>
#import "J2ObjC_types.h"

jchar* allocFromUTF8(const char* u8str, size_t u8len);

uint32_t utf8_to_utf16_length(const uint8_t* u8str, size_t u8len);
