/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.test;

import com.yahoo.squidb.annotations.TableModelSpec;
import com.yahoo.squidb.annotations.tables.UpsertKey;
import com.yahoo.squidb.annotations.tables.constraints.NotNull;
import com.yahoo.squidb.annotations.tables.constraints.UniqueColumns;

@TableModelSpec(className = "TestMultiColumnUpsertable", tableName = "testMultiColumnUpsert")
@UniqueColumns(columns = {"key1", "key2"})
public class TestMultiColumnUpsertableSpec {

    @UpsertKey
    @NotNull
    String key1;

    @UpsertKey
    @NotNull
    String key2;

    String value1;

    String value2;

}
