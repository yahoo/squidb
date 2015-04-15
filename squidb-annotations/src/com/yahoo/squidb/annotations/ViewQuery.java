/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * For specifying a Query object in a {@link com.yahoo.squidb.annotations.ViewModelSpec} class that defines the SQLite
 * query to use for the view
 */
@Target(ElementType.FIELD)
public @interface ViewQuery {

    /**
     * Set to false if you want the query object in the generated model class to be mutable. This is <em>not</em>
     * recommended for most cases.
     */
    boolean freeze() default true;

}
