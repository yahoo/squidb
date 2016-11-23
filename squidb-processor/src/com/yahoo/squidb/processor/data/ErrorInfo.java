/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.data;

import com.yahoo.aptutils.model.DeclaredTypeName;

/**
 * Tuple class to hold logged error info, to be written by the
 * {@link com.yahoo.squidb.processor.plugins.defaults.ErrorLoggingPlugin}.
 */
public final class ErrorInfo {

    /**
     * The class on which to log the error
     */
    public final DeclaredTypeName errorClass;

    /**
     * The specific element on which to log the error (or null/empty to log on the class)
     */
    public final String element;

    /**
     * The error message to log
     */
    public final String message;

    ErrorInfo(DeclaredTypeName errorClass, String element, String message) {
        this.errorClass = errorClass;
        this.element = element;
        this.message = message;
    }
}
