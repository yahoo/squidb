/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.test;

import com.yahoo.squidb.annotations.PrimaryKey;
import com.yahoo.squidb.annotations.TableModelSpec;
import com.yahoo.squidb.annotations.defaults.DefaultBlob;
import com.yahoo.squidb.annotations.defaults.DefaultBool;
import com.yahoo.squidb.annotations.defaults.DefaultDouble;
import com.yahoo.squidb.annotations.defaults.DefaultExpression;
import com.yahoo.squidb.annotations.defaults.DefaultInt;
import com.yahoo.squidb.annotations.defaults.DefaultString;

@TableModelSpec(className = "Thing", tableName = "things")
public class ThingSpec {

    public static final String DEFAULT_FOO = "thing";
    public static final int DEFAULT_BAR = 100;
    public static final boolean DEFAULT_IS_ALIVE = true;

    @PrimaryKey(autoincrement = false)
    long id;

    @DefaultString(DEFAULT_FOO)
    String foo;

    @DefaultInt(100)
    int bar;

    long baz;

    @DefaultDouble(0.0)
    double qux;

    @DefaultBool(true)
    boolean isAlive;

    @DefaultBlob("x'123ABC'")
    byte[] blob;

    @DefaultExpression(DefaultExpression.CURRENT_TIMESTAMP)
    String timestamp;
}
