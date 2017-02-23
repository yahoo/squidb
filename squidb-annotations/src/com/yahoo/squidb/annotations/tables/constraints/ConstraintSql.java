/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.annotations.tables.constraints;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Used for specifying constraints as raw SQL. This may annotate either a column field in a table model spec
 * (for column constraints) or the table model spec itself (for table constraints). Note that dedicated annotations
 * for many different types of constraints exist (e.g. {@link PrimaryKey} or {@link Collate}). These dedicated
 * annotations are preferable to ConstraintSql, as they can be subject to additional validation at compile time.
 * Where possible, prefer to use the dedicated constraint annotations instead of this annotation.
 */
@Target({ElementType.TYPE, ElementType.FIELD})
public @interface ConstraintSql {

    /**
     * Specify constraints here as raw SQL, e.g. "NOT NULL" or "UNIQUE COLLATE NOCASE"
     */
    String value() default "";
}
