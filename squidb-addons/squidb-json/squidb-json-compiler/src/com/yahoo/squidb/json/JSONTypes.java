/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.json;

import com.squareup.javapoet.ClassName;

public class JSONTypes {
    private static final String JSON_PACKAGE = "com.yahoo.squidb.json";
    public static final ClassName PARAMETERIZED_TYPE_BUILDER = ClassName.get(JSON_PACKAGE, "ParameterizedTypeBuilder");
    public static final ClassName JSON_PROPERTY_SUPPORT = ClassName.get(JSON_PACKAGE, "JSONPropertySupport");
    public static final ClassName JSON_PROPERTY = ClassName.get(JSON_PACKAGE, "JSONProperty");
}
