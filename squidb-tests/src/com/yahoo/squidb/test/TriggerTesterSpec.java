/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.test;

import com.yahoo.squidb.annotations.ColumnName;
import com.yahoo.squidb.annotations.PrimaryKey;
import com.yahoo.squidb.annotations.TableModelSpec;
import com.yahoo.squidb.annotations.defaults.DefaultInt;

@TableModelSpec(className = "TriggerTester", tableName = "trigger_testers")
public class TriggerTesterSpec {

    @PrimaryKey
    @ColumnName("_id")
    long id;

    @DefaultInt(0)
    int value1;

    @DefaultInt(0)
    int value2;

    String str1;

    String str2;
}
