/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.annotations.defaults;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Annotation for specifying a non-primitive default value derived from a SQL expression. This annotation can also be
 * used to specify the special-case defaults CURRENT_TIME, CURRENT_DATE, or CURRENT_TIMESTAMP. Declaration of these
 * constant expressions are defined in this annotation as a convenience.
 *
 * @see <a href="http://sqlite.org/lang_createtable.html">create table syntax</a> for information about
 * non-primitive defaults
 */
@Target(ElementType.FIELD)
public @interface DefaultExpression {

    String CURRENT_TIME = "CURRENT_TIME";
    String CURRENT_DATE = "CURRENT_DATE";
    String CURRENT_TIMESTAMP = "CURRENT_TIMESTAMP";

    String value();
}
