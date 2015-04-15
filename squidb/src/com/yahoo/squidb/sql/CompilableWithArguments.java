package com.yahoo.squidb.sql;

import java.util.List;

abstract class CompilableWithArguments {

    protected static final int STRING_BUILDER_INITIAL_CAPACITY = 128;

    @Override
    public String toString() {
        return toRawSql();
    }

    public final String toRawSql() {
        return toStringWithSelectionArgs(null);
    }

    protected final String toStringWithSelectionArgs(List<Object> args) {
        StringBuilder sql = new StringBuilder(STRING_BUILDER_INITIAL_CAPACITY);
        appendCompiledStringWithArguments(sql, args);
        return sql.toString();
    }

    abstract void appendCompiledStringWithArguments(StringBuilder sql, List<Object> selectionArgsBuilder);

}
