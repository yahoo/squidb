/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.annotations.defaults;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Annotation for specifying a default value on a String column. Note that if you specify a String value of "null",
 * it is interpreted to mean that you want the default value of the column to be the literal 4-character string "null".
 */
@Target(ElementType.FIELD)
public @interface DefaultString {

    String value();
}
