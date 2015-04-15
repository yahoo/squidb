/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.test;

import com.yahoo.squidb.annotations.ColumnSpec;
import com.yahoo.squidb.annotations.TableModelSpec;

@TableModelSpec(className = "Employee", tableName = "employees")
public class EmployeeSpec {

    @ColumnSpec(constraints = "NOT NULL")
    String name;

    long managerId;

    @ColumnSpec(defaultValue = "true")
    boolean isHappy;
}
