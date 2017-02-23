/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.test;

import com.yahoo.squidb.annotations.tables.ColumnName;
import com.yahoo.squidb.annotations.TableModelSpec;
import com.yahoo.squidb.annotations.tables.defaults.DefaultLong;
import com.yahoo.squidb.annotations.tables.defaults.DefaultNull;

@TableModelSpec(className = "TestVirtualModel", tableName = "virtual_models", virtualModule = "fts4")
public class TestVirtualModelSpec {

    @ColumnName("test_num")
    @DefaultLong(7)
    long testNumber;

    @DefaultNull
    String title;

    @DefaultNull
    String body;
}
