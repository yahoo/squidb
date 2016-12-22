/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.test;

import com.yahoo.squidb.annotations.ColumnSpec;
import com.yahoo.squidb.annotations.TableModelSpec;
import com.yahoo.squidb.annotations.UpsertKey;

@TableModelSpec(className = "TestMultiColumnUpsertable", tableName = "testMultiColumnUpsert",
    tableConstraint = "UNIQUE(key1, key2)")
public class TestMultiColumnUpsertableSpec {

    @UpsertKey
    @ColumnSpec(constraints = "NOT NULL")
    String key1;

    @UpsertKey
    @ColumnSpec(constraints = "NOT NULL")
    String key2;

    String value1;

    String value2;

}
