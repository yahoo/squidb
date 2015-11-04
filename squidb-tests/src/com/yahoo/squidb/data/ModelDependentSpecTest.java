/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.data;

import com.yahoo.squidb.test.DatabaseTestCase;
import com.yahoo.squidb.test.TestModel;
import com.yahoo.squidb.test.TestViewModel;

public class ModelDependentSpecTest extends DatabaseTestCase {

    public void testModelDependentConstantIsAccessible() {
        try {
            TestModel.DEFAULT_ORDER.getClass();
        } catch (ExceptionInInitializerError e) {
            fail("The generated model must define constants that depend on other constants after " +
                    "their dependencies. Fields marked `static final` are initialised in the order " +
                    "they appear in the source.");
        }
    }

    public void testModelDependentConstantFromInnerClassIsAccessible() {
        try {
            TestViewModel.DEFAULT_ORDER.getClass();
        } catch (ExceptionInInitializerError e) {
            fail("The generated model must define constants that depend on other constants after " +
                    "their dependencies. Fields marked `static final` are initialised in the order " +
                    "they appear in the source.");
        }
    }
}
