/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.annotations.defaults;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Annotation for specifying a default value on a double column
 */
@Target(ElementType.FIELD)
public @interface DefaultDouble {

    double value();
}
