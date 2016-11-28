/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.test;

import com.yahoo.squidb.annotations.Alias;
import com.yahoo.squidb.annotations.Ignore;
import com.yahoo.squidb.annotations.ViewModelSpec;
import com.yahoo.squidb.annotations.ViewQuery;
import com.yahoo.squidb.sql.Function;
import com.yahoo.squidb.sql.Join;
import com.yahoo.squidb.sql.Property.EnumProperty;
import com.yahoo.squidb.sql.Property.IntegerProperty;
import com.yahoo.squidb.sql.Property.LongProperty;
import com.yahoo.squidb.sql.Property.StringProperty;
import com.yahoo.squidb.sql.Query;

@ViewModelSpec(className = "TestSubqueryModel", viewName = "subquery", isSubquery = true)
public class TestSubqueryModelSpec {

    @ViewQuery
    public static final Query QUERY = Query.select().from(TestModel.TABLE)
            .join(Join.inner(Employee.TABLE, TestModel.ID.eq(Employee.ID)));

    @Ignore
    public static final LongProperty IGNORED_PROPERTY = Employee.MANAGER_ID;

    public static final LongProperty TEST_MODEL_ID = TestModel.ID;

    public static final LongProperty EMPLOYEE_MODEL_ID = Employee.ID;

    @Alias("blahTestName")
    public static final StringProperty TEST_NAME = TestModel.FIRST_NAME;

    @Alias("blahName")
    public static final StringProperty EMPLOYEE_NAME = Employee.NAME;

    @Alias("   luckyNumber\t")
    public static final IntegerProperty TEST_LUCKY_NUMBER = TestModel.LUCKY_NUMBER;

    @Alias("blahEnum")
    public static final EnumProperty<TestEnum> TEST_ENUM = TestModel.SOME_ENUM;

    @Alias("uppercase_name")
    public static final StringProperty UPPERCASE_NAME = StringProperty
            .fromFunction(Function.upper(EMPLOYEE_NAME), "uppercase_name");

}
