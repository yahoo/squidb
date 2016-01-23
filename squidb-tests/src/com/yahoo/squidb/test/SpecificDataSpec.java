/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.test;

import com.yahoo.squidb.annotations.InheritedModelSpec;
import com.yahoo.squidb.sql.Property.EnumProperty;
import com.yahoo.squidb.sql.Property.StringProperty;

@InheritedModelSpec(className = "SpecificData", inheritsFrom = "com.yahoo.squidb.test.BasicData")
public class SpecificDataSpec {

    public static final StringProperty FIRST_NAME = BasicData.DATA_1;

    public static final StringProperty LAST_NAME = BasicData.DATA_2;

    public static final StringProperty ADDRESS = BasicData.DATA_3;

    public static final EnumProperty<TestEnum> MY_ENUM = BasicData.SOME_ENUM;

}
