/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Used for specifying extra metadata about generated column definitions.
 */
@Target(ElementType.FIELD)
public @interface ColumnSpec {

    String DEFAULT_NONE = "!NONE!";
    String DEFAULT_NULL = "!NULL!";

    /**
     * Specify a column name here if you want your property to have a different column name in the SQL table than its
     * declared name
     */
    String name() default "";

    /**
     * Specify column constraints here as raw SQL, e.g. "NOT NULL" or "UNIQUE COLLATE NOCASE"
     */
    String constraints() default DEFAULT_NONE;

    /**
     * Default value for the SQL column in table definition
     */
    String defaultValue() default DEFAULT_NONE;
}
