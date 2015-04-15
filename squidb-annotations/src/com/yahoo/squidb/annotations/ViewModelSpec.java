/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * For declaring model classes that are backed by a SQLite view
 */
@Target(ElementType.TYPE)
public @interface ViewModelSpec {

    /**
     * The name of the class to be generated
     */
    String className();

    /**
     * The name of the SQLite view for this model. If {@link #isSubquery()} is set to true, it will be used as the
     * subquery alias instead.
     */
    String viewName();

    /**
     * True if the generated model should be based on an inlined subquery rather than a view
     */
    boolean isSubquery() default false;

}
