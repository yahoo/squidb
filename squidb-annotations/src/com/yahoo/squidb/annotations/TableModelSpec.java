/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * For declaring model classes that are backed by a SQLite table
 */
@Target(ElementType.TYPE)
public @interface TableModelSpec {

    /**
     * The name of the class to be generated
     */
    String className();

    /**
     * The name of the SQLite table for this model
     */
    String tableName();

    /**
     * Optional string for specifying table constraints when creating the table (raw SQL), e.g.
     * "FOREIGN KEY(key_col) REFERENCES other_table(_id) ON DELETE CASCADE" or "UNIQUE (col1, col2) ON CONFLICT
     * IGNORE". This value is ignored for virtual tables.
     */
    String tableConstraint() default "";

    /**
     * Optional string specifying the module used for creating a virtual table. If set, the generated model
     * will be a virtual table model.
     */
    String virtualModule() default "";

    /**
     * Optional flag to specify that the table model should not generate an explicit INTEGER PRIMARY KEY as an alias
     * to the table's rowid. This option should be enabled if a multi-column primary key is specified in
     * {@link #tableConstraint()}, or if the user does not wish to declare an explicit alias to the rowid column.
     */
    boolean noRowIdAlias() default false;

}
