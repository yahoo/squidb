/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.annotations.tables.constraints;

import com.yahoo.squidb.annotations.tables.ConflictAlgorithm;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Used to specify a NOT NULL constraint on a column.
 */
@Target(ElementType.FIELD)
public @interface NotNull {

    /**
     * Specify the {@link ConflictAlgorithm} to use for this column if the not null constraint is violated
     */
    ConflictAlgorithm onConflict() default ConflictAlgorithm.NONE;

}
