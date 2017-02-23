/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.annotations.tables.constraints;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Used to specify a collation constraint for a table column. SQLite supports the collation sequences BINARY, NOCASE,
 * and RTRIM by default; constants for these sequences are defined in this annotation as a convenience. This annotation
 * is optional; SQLite defaults to BINARY when no collation sequence is specified.
 *
 * @see <a href="https://www.sqlite.org/datatype3.html#collation">https://www.sqlite.org/datatype3.html#collation</a>
 */
@Target(ElementType.FIELD)
public @interface Collate {

    String BINARY = "BINARY";
    String NOCASE = "NOCASE";
    String RTRIM = "RTRIM";

    /**
     * The collation sequence to use
     */
    String value();
}
