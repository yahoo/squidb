/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.test;

import com.yahoo.squidb.annotations.TableModelSpec;
import com.yahoo.squidb.annotations.tables.UpsertKey;
import com.yahoo.squidb.annotations.tables.constraints.NotNull;
import com.yahoo.squidb.annotations.tables.constraints.Unique;

@TableModelSpec(className = "TestSingleColumnUpsertable", tableName = "testSingleColumnUpsert")
public class TestSingleColumnUpsertableSpec {

    @UpsertKey
    @Unique @NotNull
    String guid;

    String value1;

    String value2;

}
