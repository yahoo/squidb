/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.test;

import com.yahoo.squidb.annotations.ColumnSpec;
import com.yahoo.squidb.annotations.PrimaryKey;
import com.yahoo.squidb.annotations.TableModelSpec;

@TableModelSpec(className = "Thing", tableName = "things")
public class ThingSpec {

    public static final String DEFAULT_FOO = "thing";
    public static final int DEFAULT_BAR = 100;
    public static final boolean DEFAULT_IS_ALIVE = true;

    @PrimaryKey(autoincrement = false)
    long id;

    @ColumnSpec(defaultValue = DEFAULT_FOO)
    String foo;

    @ColumnSpec(defaultValue = "100")
    int bar;

    long baz;

    @ColumnSpec(defaultValue = "0.0")
    double qux;

    @ColumnSpec(defaultValue = "true")
    boolean isAlive;

    byte[] blob;
}
