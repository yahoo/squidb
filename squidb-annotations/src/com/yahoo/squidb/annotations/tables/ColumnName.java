/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.annotations.tables;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Use to explicitly specify a SQLite column name distinct from the field/property name
 */
@Target(ElementType.FIELD)
public @interface ColumnName {

    /**
     * The SQLite column name to be used
     */
    String value();
}
