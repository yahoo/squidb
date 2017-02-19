/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.annotations.defaults;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Annotation for specifying a default value on a blob (byte[]) column. Blob literals are strings containing
 * hexadecimal data preceded by an "x" or "X" character, e.g. <code>X'123abc'</code>
 * <p>
 * Note that non-null blob defaults will only appear in the table definition, and will not be included in the
 * in-memory model default values.
 */
@Target(ElementType.FIELD)
public @interface DefaultBlob {

    String value();
}
