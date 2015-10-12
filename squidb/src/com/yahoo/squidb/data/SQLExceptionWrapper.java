/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.data;

/**
 * Wrapper class used to wrap SQLExceptions and re-throw them using a common wrapper type
 */
public class SQLExceptionWrapper extends RuntimeException {

    public SQLExceptionWrapper(Throwable cause) {
        super(cause);
    }

}
