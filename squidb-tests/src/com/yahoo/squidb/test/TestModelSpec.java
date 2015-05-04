/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.test;

import android.net.Uri;

import com.yahoo.squidb.annotations.ColumnSpec;
import com.yahoo.squidb.annotations.ModelMethod;
import com.yahoo.squidb.annotations.TableModelSpec;
import com.yahoo.squidb.jackson.JacksonProperty;
import com.yahoo.squidb.sql.Property;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@TableModelSpec(className = "TestModel", tableName = "testModels",
        tableConstraint = "UNIQUE (creationDate) ON CONFLICT REPLACE")
public class TestModelSpec {

    public static final int INT_CONST = 0;
    public static final int ANOTHER_INT_CONST = 1;
    public static final String STRING_CONST = "Hello";
    public static final Map<String, Property<?>> CONST_MAP = new HashMap<String, Property<?>>();

    @Deprecated
    public static final int DEPRECATED_CONST = -1;

    public static final Uri CONTENT_URI = Uri.parse("content://com.yahoo.squidb/testModels");

    @ColumnSpec(defaultValue = ColumnSpec.DEFAULT_NULL)
    String firstName;

    @ColumnSpec(constraints = "UNIQUE COLLATE NOCASE")
    String lastName;

    @ColumnSpec(name = "creationDate")
    long birthday;

    @ColumnSpec(defaultValue = "true")
    boolean isHappy;

    @ColumnSpec(defaultValue = "[]")
    @JacksonProperty
    List<String> someList;

    @ColumnSpec(defaultValue = "7")
    int luckyNumber;

    @Deprecated
    long someDeprecatedLong;

    double someDouble;

    @ColumnSpec(name = "dollar123abc")
    int $123abc;

    @JacksonProperty
    Map<String, Integer> someMap;


    @ModelMethod
    public static String getDisplayName(TestModel instance) {
        return instance.getFirstName() + " " + instance.getLastName();
    }

    @ModelMethod(name = "prefixedName")
    public static String getDisplayNameWithPrefix(TestModel instance, String prefix) {
        return prefix + " " + instance.getDisplayName();
    }

    @ModelMethod
    public static void testVoidMethod(TestModel instance) {
        System.err.println("Hello");
    }

    public static String someStaticMethod(TestModel instance, TestModel anotherInstance) {
        return "Blah";
    }
}
