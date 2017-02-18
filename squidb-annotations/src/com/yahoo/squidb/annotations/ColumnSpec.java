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

    /**
     * Specify column constraints here as raw SQL, e.g. "NOT NULL" or "UNIQUE COLLATE NOCASE"
     */
    String constraints() default "";
}
