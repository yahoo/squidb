/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.sql;

import android.text.TextUtils;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class SqlUtils {

    private SqlUtils() {
        /* no instantiation */
    }

    static final Object[] EMPTY_ARGS = new Object[0];

    /**
     * Append a literal value to a SQL string being built. If {@code sqlArgs} is provided, a '?' may be placed in the
     * SQL string and the value added to {@code sqlArgs} instead. This method properly handles {@link Field},
     * {@link Property}, {@link Query}, and other database objects.
     *
     * @param sql The SQL string being built
     * @param sqlArgs Holds arguments that should bind to '?' characters appended to {@code sql}
     * @param value The value to be appended
     */
    static void addToSqlString(StringBuilder sql, List<Object> sqlArgs, Object value) {
        if (value instanceof DBObject<?>) {
            ((DBObject<?>) value).appendQualifiedExpression(sql, sqlArgs);
        } else if (value instanceof Query) {
            sql.append("(");
            ((Query) value).appendCompiledStringWithArguments(sql, sqlArgs);
            sql.append(")");
        } else if (value instanceof CompilableWithArguments) {
            ((CompilableWithArguments) value).appendCompiledStringWithArguments(sql, sqlArgs);
        } else if (sqlArgs == null) {
            sql.append(toSanitizedString(value));
        } else {
            if (value != null) {
                sql.append(SqlStatement.REPLACEABLE_PARAMETER);
                sqlArgs.add(value);
            } else {
                sql.append("NULL");
            }
        }
    }

    static void addToSqlString(StringBuilder sql, Object value) {
        addToSqlString(sql, null, value);
    }

    static void addCollectionArgToSqlString(StringBuilder sql, List<Object> sqlArgs, Collection<?> value) {
        if (value != null) {
            if (sqlArgs == null) {
                addInlineCollectionToSqlString(sql, value);
            } else {
                sql.append(SqlStatement.REPLACEABLE_ARRAY_PARAMETER);
                sqlArgs.add(value);
            }
        }
    }

    static void addInlineCollectionToSqlString(StringBuilder sql, Collection<?> values) {
        if (values != null && !values.isEmpty()) {
            for (Object t : values) {
                addToSqlString(sql, t);
                sql.append(",");
            }
            sql.deleteCharAt(sql.length() - 1);
        }
    }

    /**
     * Convert an arbitrary object to a string. If the argument is a {@link DBObject}, the qualified name is returned
     * instead. If the object is a {@link String}, it will be sanitized.
     */
    static String toSanitizedString(Object value) {
        if (value instanceof DBObject<?>) {
            return ((DBObject<?>) value).getQualifiedExpression();
        } else if (value instanceof String) {
            return sanitizeStringAsLiteral((String) value);
        } else if (value instanceof AtomicReference<?>) {
            return toSanitizedString(((AtomicReference<?>) value).get());
        } else if (value instanceof Query) {
            return "(" + ((Query) value).toRawSql() + ")";
        } else {
            return sanitizeObject(value);
        }
    }

    /**
     * Sanitize a {@link String} for use in a SQL statement
     */
    private static String sanitizeStringAsLiteral(String literal) {
        String sanitizedLiteral = literal.replace("'", "''");
        return "'" + sanitizedLiteral + "'";
    }

    private static String sanitizeObject(Object value) {
        if (value == null) {
            return "NULL";
        } else if (value instanceof Double || value instanceof Float || value instanceof BigDecimal) {
            return Double.toString(((Number) value).doubleValue());
        } else if (value instanceof Number) {
            return Long.toString(((Number) value).longValue());
        } else if (value instanceof Boolean) {
            return ((Boolean) value) ? "1" : "0";
        } else if (value instanceof AtomicBoolean) {
            return ((AtomicBoolean) value).get() ? "1" : "0";
        } else {
            return sanitizeStringAsLiteral(String.valueOf(value));
        }
    }

    static void appendConcatenatedCompilables(List<? extends CompilableWithArguments> compilables, StringBuilder sql,
            List<Object> selectionArgsBuilder, String separator) {
        if (compilables != null && !compilables.isEmpty()) {
            boolean needSeparator = false;
            for (CompilableWithArguments compilable : compilables) {
                if (needSeparator) {
                    sql.append(separator);
                }
                needSeparator = true;
                compilable.appendCompiledStringWithArguments(sql, selectionArgsBuilder);
            }
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
        if (TextUtils.isEmpty(pattern)) {
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
