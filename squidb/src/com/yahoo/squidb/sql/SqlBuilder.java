/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.sql;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

class SqlBuilder {

    private static final int STRING_BUILDER_INITIAL_CAPACITY = 128;

    final StringBuilder sql = new StringBuilder(STRING_BUILDER_INITIAL_CAPACITY);
    final List<Object> args;
    private boolean needsValidation = false;

    SqlBuilder(boolean withBoundArguments) {
        args = withBoundArguments ? new ArrayList<Object>() : null;
    }

    public String getSqlString() {
        return sql.toString();
    }

    public List<Object> getBoundArguments() {
        return args;
    }

    public boolean needsValidation() {
        return needsValidation;
    }

    void setNeedsValidation() {
        needsValidation = true;
    }

    /**
     * Append a value to a SQL string being built. If the SqlBuilder has bound arguments enabled, a '?' may be placed
     * in the SQL string and the value added to {@link #args} instead. This method properly handles {@link Field},
     * {@link Property}, {@link Query}, and other database objects.
     *
     * @param value The value to be appended
     * @param forSqlValidation forSqlValidation true if this statement is being compiled to validate against malicious SQL
     */
    void addValueToSql(Object value, boolean forSqlValidation) {
        if (value instanceof DBObject<?>) {
            ((DBObject<?>) value).appendQualifiedExpression(this, forSqlValidation);
        } else if (value instanceof Query) {
            Query query = (Query) value;
            sql.append("(");
            query.appendToSqlBuilder(this, forSqlValidation);
            sql.append(")");
        } else if (value instanceof CompilableWithArguments) {
            ((CompilableWithArguments) value).appendToSqlBuilder(this, forSqlValidation);
        } else if (args == null) {
            sql.append(SqlUtils.toSanitizedString(value));
        } else {
            if (value != null) {
                sql.append(SqlStatement.REPLACEABLE_PARAMETER);
                args.add(value);
            } else {
                sql.append("NULL");
            }
        }
    }

    void addCollectionArg(Collection<?> value) {
        if (value != null) {
            if (args == null) {
                SqlUtils.addInlineCollectionToSqlString(sql, value);
            } else {
                sql.append(SqlStatement.REPLACEABLE_ARRAY_PARAMETER);
                args.add(value);
            }
        }
    }

    void appendConcatenatedCompilables(List<? extends CompilableWithArguments> compilables, String separator,
            boolean forSqlValidation) {
        if (compilables != null && !compilables.isEmpty()) {
            boolean needSeparator = false;
            for (CompilableWithArguments compilable : compilables) {
                if (needSeparator) {
                    sql.append(separator);
                }
                needSeparator = true;
                compilable.appendToSqlBuilder(this, forSqlValidation);
            }
        }
    }

}
