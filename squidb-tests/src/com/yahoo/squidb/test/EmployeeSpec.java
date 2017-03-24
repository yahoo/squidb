/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.test;

import com.yahoo.squidb.annotations.TableModelSpec;
import com.yahoo.squidb.annotations.tables.ColumnName;
import com.yahoo.squidb.annotations.tables.constraints.NotNull;
import com.yahoo.squidb.annotations.tables.constraints.PrimaryKey;
import com.yahoo.squidb.annotations.tables.defaults.DefaultBoolean;

@TableModelSpec(className = "Employee", tableName = "employees")
public class EmployeeSpec {

    @PrimaryKey
    @ColumnName("_id")
    long id;

    @NotNull
    String name;

    long managerId;

    @DefaultBoolean(true)
    boolean isHappy;
}
