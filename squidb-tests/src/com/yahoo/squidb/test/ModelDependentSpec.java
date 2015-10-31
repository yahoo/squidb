/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.test;

import com.yahoo.squidb.annotations.TableModelSpec;
import com.yahoo.squidb.sql.Order;

@TableModelSpec(className = "ModelDependent", tableName = "modelDependents")
public class ModelDependentSpec {

    /**
     * Constant that depends on constants defined in the model.
     */
    public static final Order DEFAULT_ORDER = ModelDependent.NAME.asc();

    String name;

}
