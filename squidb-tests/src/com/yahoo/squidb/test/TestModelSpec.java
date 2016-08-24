/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.test;

import com.yahoo.squidb.annotations.ColumnSpec;
import com.yahoo.squidb.annotations.Implements;
import com.yahoo.squidb.annotations.ModelMethod;
import com.yahoo.squidb.annotations.TableModelSpec;
import com.yahoo.squidb.data.JSONPojo;
import com.yahoo.squidb.json.annotations.JSONColumn;
import com.yahoo.squidb.sql.Order;
import com.yahoo.squidb.sql.Property;
import com.yahoo.squidb.utility.Logger;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Here's a test javadoc for a model spec. It should be copied to the generated model.
 */
@TableModelSpec(className = "TestModel", tableName = "testModels",
        tableConstraint = "UNIQUE (creationDate) ON CONFLICT REPLACE")
@Implements(interfaceClasses = Runnable.class,
        interfaceDefinitions = @Implements.InterfaceSpec(interfaceClass = Iterable.class,
                interfaceTypeArgs = {String.class}))
public class TestModelSpec {

    public static final int INT_CONST = 0;
    public static final int ANOTHER_INT_CONST = 1;
    public static final String STRING_CONST = "Hello";
    public static final Map<String, Property<?>> CONST_MAP = new HashMap<>();
    public static final Order DEFAULT_ORDER = TestModel.FIRST_NAME.asc();

    static final String PACKAGE_PROTECTED_CONST = "somePackageProtectedConst";

    @Deprecated
    public static final int DEPRECATED_CONST = -1;

    @ColumnSpec(defaultValue = ColumnSpec.DEFAULT_NULL)
    String firstName;

    @ColumnSpec(constraints = "UNIQUE COLLATE NOCASE")
    String lastName;

    /**
     * Here's a super awesome javadoc I made for this field<br/>
     * Oh look, it has multiple lines
     * <br/><br/>
     * It even has lists!
     * <ul>
     * <li>Item 1</li>
     * <li>Item 2</li>
     * <li>Item 3</li>
     * </ul>
     */
    @ColumnSpec(name = "creationDate")
    long birthday;

    @ColumnSpec(defaultValue = "true")
    boolean isHappy;

    @ColumnSpec(defaultValue = "7")
    int luckyNumber;

    @Deprecated
    long someDeprecatedLong;

    double someDouble;

    @ColumnSpec(name = "dollar123abc")
    int $123abc;

    TestEnum someEnum;

    @ColumnSpec(defaultValue = "[]")
    @JSONColumn
    List<String> someList;

    @JSONColumn
    Map<String, Integer> someMap;

    @JSONColumn
    Map<String, Map<String, List<Integer>>> complicatedMap;

    @JSONColumn
    JSONPojo somePojo;

    @ModelMethod
    public static String getDisplayName(TestModel instance) {
        return instance.getFirstName() + " " + instance.getLastName();
    }

    /**
     * Returns the display name of the test model prefixed with the given prefix
     *
     * @param prefix the prefix to use
     */
    @ModelMethod(name = "prefixedName")
    public static String getDisplayNameWithPrefix(TestModel instance, String prefix) {
        return prefix + " " + instance.getDisplayName();
    }

    @ModelMethod
    public static void testVoidMethod(TestModel instance) {
        System.err.println("Hello");
    }

    @ModelMethod
    public static void run(TestModel instance) {
        Logger.e(Logger.LOG_TAG, "TestModel: Interface method");
    }

    @ModelMethod
    public static Iterator<String> iterator(TestModel instance) {
        return null;
    }

    /**
     * It would be pretty great if this static method had a javadoc too
     *
     * @param instance first TestModel instance
     * @param anotherInstance another TestModel instance
     * @return the literal String "Blah"
     */
    public static String someStaticMethod(TestModel instance, TestModel anotherInstance) {
        return "Blah";
    }
}
