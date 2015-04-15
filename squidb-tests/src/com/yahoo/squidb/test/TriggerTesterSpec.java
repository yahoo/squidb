/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.test;

import com.yahoo.squidb.annotations.ColumnSpec;
import com.yahoo.squidb.annotations.TableModelSpec;

@TableModelSpec(className = "TriggerTester", tableName = "trigger_testers")
public class TriggerTesterSpec {

    @ColumnSpec(defaultValue = "0")
    int value1;

    @ColumnSpec(defaultValue = "0")
    int value2;
}
