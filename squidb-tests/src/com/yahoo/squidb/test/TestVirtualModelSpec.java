/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.test;

import com.yahoo.squidb.annotations.ColumnSpec;
import com.yahoo.squidb.annotations.TableModelSpec;

@TableModelSpec(className = "TestVirtualModel", tableName = "virtual_models", virtualModule = "fts4")
public class TestVirtualModelSpec {

    @ColumnSpec(name = "test_num", defaultValue = "7")
    long testNumber;

    @ColumnSpec(defaultValue = ColumnSpec.DEFAULT_NULL)
    String title;

    @ColumnSpec(defaultValue = ColumnSpec.DEFAULT_NULL)
    String body;
}
