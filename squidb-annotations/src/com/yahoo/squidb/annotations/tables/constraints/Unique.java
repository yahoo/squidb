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
 * Used to specify a uniqueness constraint on a column. This annotation targets fields in a model spec; to declare
 * a uniqueness constraint on a collection of columns at the table level use {@link UniqueColumns}.
 */
@Target(ElementType.FIELD)
public @interface Unique {

    /**
     * Specify the {@link ConflictAlgorithm} to use for this column if the uniqueness constraint is violated
     */
    ConflictAlgorithm onConflict() default ConflictAlgorithm.NONE;

}
