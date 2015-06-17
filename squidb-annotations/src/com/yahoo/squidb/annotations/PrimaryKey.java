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
 * is only relevant for table models, i.e. models using {@link TableModelSpec}. At this time, only a single primary
 * key of type long is supported. You can use this annotation in combination with {@link ColumnSpec} to customize
 * your primary key column, including giving it a custom column name or specifying whether or not the column
 * should autoincrement (using {@link ColumnSpec#constraints()}.
 * <p>
 * If a PrimaryKey annotation is not present in a TableModelSpec class, a default ID property for the table will
 * automatically be generated.
 */
@Target(ElementType.FIELD)
public @interface PrimaryKey {

    /**
     * @return true if AUTOINCREMENT behavior for the primary key should be used. Defaults to true
     */
    boolean autoincrement() default true;

}
