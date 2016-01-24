/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.json;

import com.yahoo.aptutils.model.DeclaredTypeName;

public class JSONTypes {
    private static final String JSON_PACKAGE = "com.yahoo.squidb.json";
    public static final DeclaredTypeName PARAMETERIZED_TYPE_BUILDER = new DeclaredTypeName(JSON_PACKAGE,
            "ParameterizedTypeBuilder");
    public static final DeclaredTypeName JSON_PROPERTY_SUPPORT = new DeclaredTypeName(JSON_PACKAGE,
            "JSONPropertySupport");
    public static final DeclaredTypeName JSON_PROPERTY = new DeclaredTypeName(JSON_PACKAGE, "JSONProperty");
}
