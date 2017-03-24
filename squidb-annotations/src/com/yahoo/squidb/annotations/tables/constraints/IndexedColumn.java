/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.annotations.tables.constraints;

import com.yahoo.squidb.annotations.tables.IndexOrder;

/**
 * Used to build an indexed column declaration for multi-column {@link UniqueColumns} and {@link PrimaryKeyColumns}
 * constraints. An indexed column is a column name followed by an optional collation sequence and an optional ASC or
 * DESC index order declaration.
 */
public @interface IndexedColumn {

    /**
     * The column name
     */
    String name();

    /**
     * An optional collation sequence. SQLite default collation sequences are defined as constants in the
     * {@link Collate} annotation, e.g. {@link Collate#NOCASE}.
     */
    String collate() default "";

    /**
     * An optional index order for this column
     */
    IndexOrder order() default IndexOrder.UNSPECIFIED;
}
