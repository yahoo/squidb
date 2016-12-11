/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.sql;

import java.util.Collection;

public class SqlUtils {

    private SqlUtils() {
        /* no instantiation */
    }

    public static boolean isEmpty(String str) {
        return str == null || str.isEmpty();
    }

    public static boolean equals(String a, String b) {
        if (a == null) {
            return b == null;
        }
        return a.equals(b);
    }

    /**
     * Use an instance of ArgumentResolver instead, e.g. {@link DefaultArgumentResolver}
     */
    @Deprecated
    public static Object resolveArgReferences(Object arg) {
        return new DefaultArgumentResolver().resolveArgument(arg);
    }

    static void addInlineCollectionToSqlString(StringBuilder sql, ArgumentResolver argResolver, Collection<?> values) {
        if (values != null && !values.isEmpty()) {
            for (Object value : values) {
                sql.append(toSanitizedString(value, argResolver));
                sql.append(",");
            }
            sql.deleteCharAt(sql.length() - 1);
        }
    }

    /**
     * Convert an arbitrary object to a string. If the object itself is a {@link String}, it will be sanitized.
     */
    static String toSanitizedString(Object value, ArgumentResolver argResolver) {
        value = argResolver.resolveArgument(value);
        if (value == null) {
            return "NULL";
        } else if (value instanceof Double || value instanceof Float) {
            return Double.toString(((Number) value).doubleValue());
        } else if (value instanceof Number) {
            return Long.toString(((Number) value).longValue());
        } else if (value instanceof Boolean) {
            return ((Boolean) value) ? "1" : "0";
        } else if (value instanceof byte[]) {
            return byteArrayToBlobLiteral((byte[]) value);
        } else {
            return sanitizeStringAsLiteral(String.valueOf(value));
        }
    }

    private static final char[] hexChars =
            {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    static String byteArrayToBlobLiteral(byte[] blob) {
        if (blob.length == 0) {
            return "X''"; // Empty blob
        }
        StringBuilder result = new StringBuilder("X'");
        char[] resultChars = new char[blob.length * 2];
        for (int i = 0; i < blob.length; i++) {
            byte b = blob[i];
            int byteAsInt = b & 0xff;
            int upperBytes = byteAsInt >>> 4;
            int lowerByes = byteAsInt & 0x0f;
            resultChars[i * 2] = hexChars[upperBytes];
            resultChars[i * 2 + 1] = hexChars[lowerByes];
        }
        result.append(new String(resultChars)).append("'");
        return result.toString();
    }

    /**
     * Sanitize a {@link String} for use in a SQL statement
     */
    static String sanitizeStringAsLiteral(String literal) {
        if (literal == null) {
            return "NULL";
        }
        String sanitizedLiteral = literal.replace("'", "''");
        int nullIndex = sanitizedLiteral.indexOf('\0');
        if (nullIndex >= 0) {
            StringBuilder builder = new StringBuilder();
            int start = 0;
            while (nullIndex >= 0) {
                String substr = sanitizedLiteral.substring(start, nullIndex);
                if (substr.length() > 0) { // Append sanitized component before the null
                    builder.append("'").append(substr).append("' || ");
                }
                builder.append("CAST(ZEROBLOB(");
                int blobLength = 1;
                while (nullIndex + 1 < sanitizedLiteral.length() &&
                        sanitizedLiteral.charAt(nullIndex + 1) == '\0') { // If there are many adjacent nulls, combine
                    blobLength++;
                    nullIndex++;
                }
                builder.append(blobLength).append(") AS TEXT)"); // Close the cast
                start = nullIndex + 1;
                if (start < sanitizedLiteral.length()) { // If there's more left, continue concatenating
                    builder.append(" || ");
                }
                nullIndex = sanitizedLiteral.indexOf('\0', start);
            }
            if (start < sanitizedLiteral.length()) { // Append final sanitized component
                String substr = sanitizedLiteral.substring(start);
                if (substr.length() > 0) {
                    builder.append("'").append(substr).append("'");
                }
            }
            return builder.toString();
        } else {
            return "'" + sanitizedLiteral + "'";
        }
    }

    /**
     * Escape a pattern for use in LIKE clauses. LIKE clauses support the meta-characters '_' (matching a single
     * character) and '%' (matching a string of any length, including the empty string). Use this method to escape
     * those meta-characters in the input string so that LIKE will match on them normally. This method throws an
     * exception if the caller passes '_' or '%' as the escape character.
     *
     * @param pattern the LIKE clause pattern
     * @param escape the character to use to escape meta-characters and itself in the pattern
     * @return an escaped pattern for LIKE clauses
     * @throws IllegalArgumentException if the escape character is '_' or '%'
     * @see com.yahoo.squidb.sql.Field#like(Object, char)
     */
    public static String escapeLikePattern(String pattern, char escape) {
        if (escape == '%' || escape == '_') {
            throw new IllegalArgumentException("Invalid escape character: " + escape);
        }
        if (isEmpty(pattern)) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < pattern.length(); i++) {
            char c = pattern.charAt(i);
            if (c == '%' || c == '_' || c == escape) {
                sb.append(escape);
            }
            sb.append(c);
        }
        return sb.toString();
    }
}
