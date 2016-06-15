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
// Adapted from String16.cpp and Unicode.cpp found at:
// https://android.googlesource.com/platform/frameworks/native/+/jb-dev/libs/utils/String16.cpp
// https://android.googlesource.com/platform/frameworks/native/+/jb-dev/libs/utils/Unicode.cpp

#import <stdlib.h>
#import "SquidbUnicode.h"

static inline uint32_t utf8_codepoint_len(uint8_t ch)
{
    return ((0xe5000000 >> ((ch >> 3) & 0x1e)) & 3) + 1;
}

static inline void utf8_shift_and_mask(uint32_t* codePoint, const uint8_t byte)
{
    *codePoint <<= 6;
    *codePoint |= 0x3F & byte;
}

static inline uint32_t utf8_to_utf32_codepoint(const uint8_t *src, size_t length)
{
    uint32_t unicode;
    switch (length)
    {
        case 1:
            return src[0];
        case 2:
            unicode = src[0] & 0x1f;
            utf8_shift_and_mask(&unicode, src[1]);
            return unicode;
        case 3:
            unicode = src[0] & 0x0f;
            utf8_shift_and_mask(&unicode, src[1]);
            utf8_shift_and_mask(&unicode, src[2]);
            return unicode;
        case 4:
            unicode = src[0] & 0x07;
            utf8_shift_and_mask(&unicode, src[1]);
            utf8_shift_and_mask(&unicode, src[2]);
            utf8_shift_and_mask(&unicode, src[3]);
            return unicode;
        default:
            return 0xffff;
    }
}

static uint32_t utf8_to_utf16_length(const uint8_t* u8str, size_t u8len)
{
    const uint8_t* const u8end = u8str + u8len;
    const uint8_t* u8cur = u8str;
    /* Validate that the UTF-8 is the correct len */
    uint32_t u16measuredLen = 0;
    while (u8cur < u8end) {
        u16measuredLen++;
        uint32_t u8charLen = utf8_codepoint_len(*u8cur);
        uint32_t codepoint = utf8_to_utf32_codepoint(u8cur, u8charLen);
        if (codepoint > 0xFFFF) u16measuredLen++; // this will be a surrogate pair in utf16
        u8cur += u8charLen;
    }
    /**
     * Make sure that we ended where we thought we would and the output UTF-16
     * will be exactly how long we were told it would be.
     */
    if (u8cur != u8end) {
        return -1;
    }
    return u16measuredLen;
}

static jchar* utf8_to_utf16_no_null_terminator(const uint8_t* u8str, size_t u8len, jchar* u16str)
{
    const uint8_t* const u8end = u8str + u8len;
    const uint8_t* u8cur = u8str;
    jchar* u16cur = u16str;
    while (u8cur < u8end) {
        size_t u8len = utf8_codepoint_len(*u8cur);
        uint32_t codepoint = utf8_to_utf32_codepoint(u8cur, u8len);
        // Convert the UTF32 codepoint to one or more UTF16 codepoints
        if (codepoint <= 0xFFFF) {
            // Single UTF16 character
            *u16cur++ = (jchar) codepoint;
        } else {
            // Multiple UTF16 characters with surrogates
            codepoint = codepoint - 0x10000;
            *u16cur++ = (jchar) ((codepoint >> 10) + 0xD800);
            *u16cur++ = (jchar) ((codepoint & 0x3FF) + 0xDC00);
        }
        u8cur += u8len;
    }
    return u16cur;
}
static void utf8_to_utf16(const uint8_t* u8str, size_t u8len, jchar* u16str) {
    jchar* end = utf8_to_utf16_no_null_terminator(u8str, u8len, u16str);
    *end = 0;
}

IOSCharArray* allocFromUTF8(const char* u8str, size_t u8len)
{
    const uint8_t* u8cur = (const uint8_t*) u8str;
    const ssize_t u16len = utf8_to_utf16_length(u8cur, u8len);
    jchar* u16str = malloc(sizeof(jchar)*(u16len+1));
    if (u16str) {
        u8cur = (const uint8_t*) u8str;
        utf8_to_utf16(u8cur, u8len, u16str);
        IOSCharArray *chars = [IOSCharArray arrayWithChars:u16str count:u16len];
        free(u16str);
        return chars;
    }
    return nil;
}
