/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.sql;

import java.util.ArrayList;
import java.util.List;

class SqlBuilder {

    protected static final int STRING_BUILDER_INITIAL_CAPACITY = 128;

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

}
