/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.test;

import com.yahoo.squidb.annotations.ColumnName;
import com.yahoo.squidb.annotations.ConstraintSql;
import com.yahoo.squidb.annotations.PrimaryKey;
import com.yahoo.squidb.annotations.TableModelSpec;
import com.yahoo.squidb.annotations.defaults.DefaultBool;

@TableModelSpec(className = "Employee", tableName = "employees")
public class EmployeeSpec {

    @PrimaryKey
    @ColumnName("_id")
    long id;

    @ConstraintSql("NOT NULL")
    String name;

    long managerId;

    @DefaultBool(true)
    boolean isHappy;
}
