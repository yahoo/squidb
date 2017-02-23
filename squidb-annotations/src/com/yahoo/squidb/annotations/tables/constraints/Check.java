/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.annotations.tables.constraints;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Used for specifying CHECK constraints as raw SQL expressions
 */
@Target({ElementType.TYPE, ElementType.FIELD})
public @interface Check {

    /**
     * Specify a SQL expression to check. Note that CHECK expressions may not contain subqueries.
     */
    String value() default "";
}
