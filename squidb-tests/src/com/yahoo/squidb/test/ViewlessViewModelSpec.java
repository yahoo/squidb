/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.test;

import com.yahoo.squidb.annotations.ViewModelSpec;
import com.yahoo.squidb.sql.Function;
import com.yahoo.squidb.sql.Property.LongProperty;
import com.yahoo.squidb.sql.Property.StringProperty;

@ViewModelSpec(className = "ViewlessViewModel", viewName = "viewlessViewModel")
public class ViewlessViewModelSpec {

    public static final LongProperty TEST_MODEL_ID = TestModel.ID;

    public static final LongProperty EMPLOYEE_MODEL_ID = Employee.ID;

    public static final StringProperty TEST_NAME = TestModel.FIRST_NAME;

    public static final StringProperty EMPLOYEE_NAME = Employee.NAME;

    public static final StringProperty UPPERCASE_NAME = StringProperty
            .fromFunction(Function.upper(EMPLOYEE_NAME), "uppercase_name");
}
