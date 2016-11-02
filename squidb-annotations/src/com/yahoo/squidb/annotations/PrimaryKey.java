/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Used to specify that a field in a model spec should be used as the primary key for the table. This annotation
 * is only relevant for table models, i.e. models using {@link TableModelSpec}. Only one primary key per table is
 * permitted by SQLite.
 * <p>
 * Integer-type columns (long, int, etc.) that are annotated with the PrimaryKey annotation are considered
 * "INTEGER PRIMARY KEY" columns. In SQLite, an INTEGER PRIMARY KEY column is an alias for the built-in SQLite rowid
 * column. If present, SquiDB will use such a column for model bookkeeping. If a non-integer column is annotated with
 * PrimaryKey, SquiDB will generate an explicit reference to the built-in rowid column to use for bookkeeping.
 * <p>
 * If a PrimaryKey annotation is not present in a TableModelSpec class, a default INTEGER PRIMARY KEY ID property for
 * the table will automatically be generated. However, this behavior will be removed in a future version, in favor
 * of using the built-in rowid column directly when no other primary key is declared.
 */
@Target(ElementType.FIELD)
public @interface PrimaryKey {

    /**
     * @return true if AUTOINCREMENT behavior for the primary key should be used. Defaults to true. Applies only to
     * columns of integer type (long, int, etc.)
     */
    boolean autoincrement() default true;

}
