/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class StringUtils {

    private static final String IDENTIFIER_PART_REGEX = "([A-Z][a-z]*)|([a-z]+)|([0-9]+)|([^A-Za-z0-9]+)";
    private static final Pattern IDENTIFIER_PART_PATTERN = Pattern.compile(IDENTIFIER_PART_REGEX);

    private StringUtils() {
    }

    public static String toCamelCase(String s) {
        String[] parts = s.split("_");
        if (parts.length > 1) {
            return convertComponentsToCamelCaseName(parts);
        } else if (isAllUppercase(s)) {
            s = s.toLowerCase();
        }
        return s;
    }

    private static boolean isAllUppercase(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (!Character.isUpperCase(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static String convertComponentsToCamelCaseName(String[] parts) {
        StringBuilder result = new StringBuilder();
        for (String s : parts) {
            s = s.toLowerCase();
            if (s.length() != 0) {
                if (result.length() == 0) {
                    result.append(s);
                } else {
                    result.append(capitalize(s));
                }
            }
        }
        return result.toString();
    }

    public static String capitalize(String s) {
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    public static String toUpperUnderscore(String s) {
        StringBuilder builder = new StringBuilder();
        Matcher matcher = IDENTIFIER_PART_PATTERN.matcher(s);
        while (matcher.find()) {
            String part = matcher.group().toUpperCase();
            if (builder.length() > 0 && !part.startsWith("_")) {
                builder.append("_");
            }
            builder.append(part);
        }
        return builder.toString();
    }
}
