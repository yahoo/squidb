/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.annotations.tables.constraints;

import com.yahoo.squidb.annotations.TableModelSpec;
import com.yahoo.squidb.annotations.tables.ConflictAlgorithm;
import com.yahoo.squidb.annotations.tables.IndexOrder;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Used to specify that a field in a model spec should be used as the primary key for the table. This annotation
 * is only relevant for table models, i.e. models using {@link TableModelSpec}. Only one primary key per table is
 * permitted by SQLite.
 * <p>
 * Integer-type columns (long, int, etc.) that are annotated with the PrimaryKey annotation are considered
 * "INTEGER PRIMARY KEY" columns. In SQLite, an INTEGER PRIMARY KEY column is an alias for the built-in SQLite rowid
 * column. If present, SquiDB will use such a column for model bookkeeping. If no column is annotated with @PrimaryKey,
 * or if a non-integer column is annotated with @PrimaryKey, SquiDB will generate an explicit reference to the
 * built-in rowid column to use for bookkeeping.
 * <p>
 * This annotation targets fields in a model spec; to declare a primary key constraint on a collection of columns at the
 * table level use {@link PrimaryKeyColumns}.
 */
@Target(ElementType.FIELD)
public @interface PrimaryKey {

    /**
     * Allows specifying if ASC or DESC should be appended to the primary key definition. Most applications will
     * probably not need to use this field. Note that an INTEGER PRIMARY KEY DESC column will NOT act as an alias to
     * the rowid, due to an obscure SQLite corner case.
     */
    IndexOrder order() default IndexOrder.UNSPECIFIED;

    /**
     * Specify the {@link ConflictAlgorithm} to use for this column if the uniqueness constraint is violated
     */
    ConflictAlgorithm onConflict() default ConflictAlgorithm.NONE;

    /**
     * Set to true if AUTOINCREMENT behavior for the primary key should be used, false otherwise. Defaults to true.
     * Applies only to columns of integer type (long, int, etc.)
     */
    boolean autoincrement() default true;

}
