/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.annotations.tables.defaults;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Annotation for explicitly specifying that a column has a default value of null. This also guarantees that null
 * will appear in the model default values.
 */
@Target(ElementType.FIELD)
public @interface DefaultNull {

}
