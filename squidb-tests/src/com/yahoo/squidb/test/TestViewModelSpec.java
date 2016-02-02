/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.test;

import com.yahoo.squidb.annotations.Constants;
import com.yahoo.squidb.annotations.ViewModelSpec;
import com.yahoo.squidb.annotations.ViewQuery;
import com.yahoo.squidb.data.JSONPojo;
import com.yahoo.squidb.json.JSONProperty;
import com.yahoo.squidb.sql.Function;
import com.yahoo.squidb.sql.Order;
import com.yahoo.squidb.sql.Property.EnumProperty;
import com.yahoo.squidb.sql.Property.LongProperty;
import com.yahoo.squidb.sql.Property.StringProperty;
import com.yahoo.squidb.sql.Query;

import java.util.List;
import java.util.Map;

@ViewModelSpec(className = "TestViewModel", viewName = "testView")
public class TestViewModelSpec {

    @ViewQuery
    public static final Query QUERY = Query.select().from(TestModel.TABLE)
            .innerJoin(Employee.TABLE, TestModel.ID.eq(Employee.ID));

    public static final Query CONST_QUERY = Query.select().from(TestModel.TABLE)
            .leftJoin(Employee.TABLE, TestModel.ID.eq(Employee.ID));

    public static final LongProperty TEST_MODEL_ID = TestModel.ID;

    public static final LongProperty EMPLOYEE_MODEL_ID = Employee.ID;

    public static final StringProperty TEST_NAME = TestModel.FIRST_NAME;

    public static final StringProperty EMPLOYEE_NAME = Employee.NAME;

    public static final StringProperty UPPERCASE_NAME = StringProperty
            .fromFunction(Function.upper(EMPLOYEE_NAME), "uppercase_name");

    public static final EnumProperty<TestEnum> TEST_ENUM = TestModel.SOME_ENUM;

    public static final JSONProperty<JSONPojo> JSON_PROP = TestModel.SOME_POJO;

    public static final JSONProperty<Map<String, Map<String, List<Integer>>>> CRAZY_MAP
            = TestModel.COMPLICATED_MAP;

    @Constants
    public static class Const {

        public static final Order DEFAULT_ORDER = TestViewModel.EMPLOYEE_MODEL_ID.asc();
    }

}
