/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.data;

import com.yahoo.aptutils.model.DeclaredTypeName;

public final class ErrorInfo {

    public final DeclaredTypeName errorClass; // The class on which to log the error
    public final String element; // The specific element on which to log the error (or null/empty to log on the class)
    public final String message; // The error message to log

    public ErrorInfo(DeclaredTypeName errorClass, String element, String message) {
        this.errorClass = errorClass;
        this.element = element;
        this.message = message;
    }
}
