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
 * Used to specify a uniqueness constraint on a collection of columns at the table level. This annotation targets
 * a table model spec class and puts the SQLite constraint at the table level. For single column uniqueness constraints,
 * users can also target model spec fields with {@link Unique}.
 */
@Target(ElementType.TYPE)
public @interface UniqueColumns {

    /**
     * Specify the collection of columns to be used in this uniqueness constraint using only column names. If
     * collation sequences or ASC/DESC are needed on some columns in the constraint, use {@link #indexedColumns()}
     * instead.
     */
    String[] columns() default {};

    /**
     * Specify the collection of columns to be used in this uniqueness constraint as a list of {@link IndexedColumn}s.
     */
    IndexedColumn[] indexedColumns() default {};

    /**
     * Specify the {@link ConflictAlgorithm} to use if the uniqueness constraint is violated
     */
    ConflictAlgorithm onConflict() default ConflictAlgorithm.NONE;
}
