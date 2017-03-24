/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.test;

import com.google.j2objc.annotations.ObjectiveCName;

import com.yahoo.squidb.annotations.Implements;
import com.yahoo.squidb.annotations.ModelMethod;
import com.yahoo.squidb.annotations.TableModelSpec;
import com.yahoo.squidb.annotations.tables.ColumnName;
import com.yahoo.squidb.annotations.tables.ConflictAlgorithm;
import com.yahoo.squidb.annotations.tables.constraints.Collate;
import com.yahoo.squidb.annotations.tables.constraints.NotNull;
import com.yahoo.squidb.annotations.tables.constraints.PrimaryKey;
import com.yahoo.squidb.annotations.tables.constraints.Unique;
import com.yahoo.squidb.annotations.tables.constraints.UniqueColumns;
import com.yahoo.squidb.annotations.tables.defaults.DefaultBoolean;
import com.yahoo.squidb.annotations.tables.defaults.DefaultInt;
import com.yahoo.squidb.annotations.tables.defaults.DefaultNull;
import com.yahoo.squidb.annotations.tables.defaults.DefaultString;
import com.yahoo.squidb.data.AbstractModel;
import com.yahoo.squidb.data.JSONPojo;
import com.yahoo.squidb.data.TableModel;
import com.yahoo.squidb.json.annotations.JSONColumn;
import com.yahoo.squidb.sql.Order;
import com.yahoo.squidb.sql.Property;
import com.yahoo.squidb.utility.SquidbLog;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Here's a test javadoc for a model spec. It should be copied to the generated model.
 */
@TableModelSpec(className = "TestModel", tableName = "testModels")
@UniqueColumns(columns = "creationDate", onConflict = ConflictAlgorithm.REPLACE)
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

    @PrimaryKey
    @ColumnName("_id")
    long id;

    @DefaultNull
    String firstName;

    @Unique @Collate(Collate.NOCASE)
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
    @ColumnName("creationDate")
    long birthday;

    @NotNull @DefaultBoolean(true)
    boolean isHappy;

    @NotNull @Collate(Collate.BINARY) @DefaultInt(7)
    int luckyNumber;

    @Deprecated
    long someDeprecatedLong;

    double someDouble;

    @ColumnName("dollar123abc")
    int $123abc;

    TestEnum someEnum;

    @DefaultString("[]")
    @JSONColumn
    List<String> someList;

    @JSONColumn
    Map<String, Integer> someMap;

    @JSONColumn
    Map<String, Map<String, List<Integer>>> complicatedMap;

    @JSONColumn
    JSONPojo somePojo;

    @ModelMethod
    @ObjectiveCName("displayNameWithModel:")
    @Nonnull
    public static String getDisplayName(TestModel instance) {
        return instance.getFirstName() + " " + instance.getLastName();
    }

    /**
     * Returns the display name of the test model prefixed with the given prefix
     *
     * @param prefix the prefix to use
     */
    @ModelMethod(name = "prefixedName")
    @ObjectiveCName("displayNameWithModel:withPrefix:")
    @Nonnull
    public static String getDisplayNameWithPrefix(TestModel instance, @Nonnull String prefix) {
        return prefix + " " + instance.getDisplayName();
    }

    @ModelMethod
    public static void testVoidMethod(AbstractModel instance) {
        System.err.println("Hello");
    }

    @ModelMethod
    public static void run(TableModel instance) {
        SquidbLog.e(SquidbLog.LOG_TAG, "TestModel: Interface method");
    }

    @ModelMethod
    @Nullable
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
    @ObjectiveCName("staticMethodWithModel1:withModel2:")
    @Nonnull
    public static String someStaticMethod(@Nullable TestModel instance, @Nullable TestModel anotherInstance) {
        return "Blah";
    }
}
