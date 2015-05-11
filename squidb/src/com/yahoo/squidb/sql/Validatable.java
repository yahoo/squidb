/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.sql;

import java.util.List;

/**
 * An extension of compilable that lets us pass a boolean flag to signal that extra validation is needed. This
 * is useful when compiling queries that need validation to guard against malicious arguments.
 */
abstract class Validatable extends CompilableWithArguments {

    @Override
    final void appendCompiledStringWithArguments(StringBuilder sql, List<Object> selectionArgsBuilder) {
        appendCompiledStringWithArguments(sql, selectionArgsBuilder, false);
    }

    abstract void appendCompiledStringWithArguments(StringBuilder sql, List<Object> selectionArgsBuilder,
            boolean withValidation);
}
