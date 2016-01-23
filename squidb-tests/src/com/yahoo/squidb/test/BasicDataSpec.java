/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.test;

import com.yahoo.squidb.annotations.PrimaryKey;
import com.yahoo.squidb.annotations.TableModelSpec;

@TableModelSpec(className = "BasicData", tableName = "data")
public class BasicDataSpec {

    @PrimaryKey
    long dataId;

    String data1;

    String data2;

    String data3;

    int type;

    TestEnum someEnum;

}
