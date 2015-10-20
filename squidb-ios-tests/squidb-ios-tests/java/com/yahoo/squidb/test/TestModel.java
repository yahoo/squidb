package com.yahoo.squidb.test;

import com.yahoo.squidb.data.SquidCursor;
import com.yahoo.squidb.data.TableModel;
import com.yahoo.squidb.data.ValuesStorage;
import com.yahoo.squidb.ios.IOSTableModel;
import com.yahoo.squidb.ios.MapValuesStorage;
import com.yahoo.squidb.sql.Property;
import com.yahoo.squidb.sql.Property.BooleanProperty;
import com.yahoo.squidb.sql.Property.DoubleProperty;
import com.yahoo.squidb.sql.Property.IntegerProperty;
import com.yahoo.squidb.sql.Property.LongProperty;
import com.yahoo.squidb.sql.Property.StringProperty;
import com.yahoo.squidb.sql.Table;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

// Generated code -- do not modify!
// This class was generated from the model spec at com.yahoo.squidb.test.TestModelSpec
public class TestModel extends IOSTableModel implements Runnable, Iterable<String> {
    
    // --- constants
    public static final int INT_CONST = TestModelSpec.INT_CONST;
    public static final int ANOTHER_INT_CONST = TestModelSpec.ANOTHER_INT_CONST;
    public static final String STRING_CONST = TestModelSpec.STRING_CONST;
    public static final Map<String, Property<?>> CONST_MAP = TestModelSpec.CONST_MAP;
    
    // --- allocate properties array
    public static final Property<?>[] PROPERTIES = new Property<?>[10];
    
    // --- table declaration
    public static final Table TABLE = new Table(TestModel.class, PROPERTIES, "testModels", null, "UNIQUE (creationDate) ON CONFLICT REPLACE");
    
    // --- property declarations
    public static final LongProperty ID = new LongProperty(TABLE, TableModel.DEFAULT_ID_COLUMN, "PRIMARY KEY AUTOINCREMENT");
    static {
        TABLE.setIdProperty(ID);
    };
    
    public static final StringProperty FIRST_NAME = new StringProperty(TABLE, "firstName", "DEFAULT NULL");
    
    public static final StringProperty LAST_NAME = new StringProperty(TABLE, "lastName", "UNIQUE COLLATE NOCASE");
    
    public static final LongProperty BIRTHDAY = new LongProperty(TABLE, "creationDate");
    
    public static final BooleanProperty IS_HAPPY = new BooleanProperty(TABLE, "isHappy", "DEFAULT 1");
    
    public static final StringProperty SOME_LIST = new StringProperty(TABLE, "someList", "DEFAULT '[]'");
    
    public static final IntegerProperty LUCKY_NUMBER = new IntegerProperty(TABLE, "luckyNumber", "DEFAULT 7");
    
    public static final DoubleProperty SOME_DOUBLE = new DoubleProperty(TABLE, "someDouble");
    
    public static final IntegerProperty $_123_ABC = new IntegerProperty(TABLE, "dollar123abc");
    
    public static final StringProperty SOME_MAP = new StringProperty(TABLE, "someMap");
    
    @Deprecated
    public static final LongProperty SOME_DEPRECATED_LONG = new LongProperty(TABLE, "someDeprecatedLong");
    
    @Override
    public LongProperty getIdProperty() {
        return ID;
    }
    
    static {
        PROPERTIES[0] = ID;
        PROPERTIES[1] = FIRST_NAME;
        PROPERTIES[2] = LAST_NAME;
        PROPERTIES[3] = BIRTHDAY;
        PROPERTIES[4] = IS_HAPPY;
        PROPERTIES[5] = SOME_LIST;
        PROPERTIES[6] = LUCKY_NUMBER;
        PROPERTIES[7] = SOME_DOUBLE;
        PROPERTIES[8] = $_123_ABC;
        PROPERTIES[9] = SOME_MAP;
    }
    
    // --- default values
    protected static final ValuesStorage defaultValues = new MapValuesStorage();
    static {
        // --- put property defaults
        defaultValues.putNull(FIRST_NAME.getName());
        defaultValues.put(IS_HAPPY.getName(), true);
        defaultValues.put(SOME_LIST.getName(), "[]");
        defaultValues.put(LUCKY_NUMBER.getName(), 7);
    }
    
    @Override
    public ValuesStorage getDefaultValues() {
        return defaultValues;
    }
    
    // --- default constructors
    public TestModel() {
        super();
    }
    
    public TestModel(SquidCursor<TestModel> cursor) {
        this();
        readPropertiesFromCursor(cursor);
    }
    
    public TestModel(Map<String, Object> values) {
        this(values, PROPERTIES);
    }
    
    public TestModel(Map<String, Object> values, Property<?>... withProperties) {
        this();
        readPropertiesFromMap(values, withProperties);
    }
    
    @Override
    public TestModel clone() {
        return (TestModel) super.clone();
    }
    
    // --- getters and setters
    public String getFirstName() {
        return get(FIRST_NAME);
    }
    
    public TestModel setFirstName(String firstName) {
        set(FIRST_NAME, firstName);
        return this;
    }
    
    public String getLastName() {
        return get(LAST_NAME);
    }
    
    public TestModel setLastName(String lastName) {
        set(LAST_NAME, lastName);
        return this;
    }
    
    public Long getBirthday() {
        return get(BIRTHDAY);
    }
    
    public TestModel setBirthday(Long birthday) {
        set(BIRTHDAY, birthday);
        return this;
    }
    
    public Boolean isHappy() {
        return get(IS_HAPPY);
    }
    
    public TestModel setIsHappy(Boolean isHappy) {
        set(IS_HAPPY, isHappy);
        return this;
    }
    
    public Integer getLuckyNumber() {
        return get(LUCKY_NUMBER);
    }
    
    public TestModel setLuckyNumber(Integer luckyNumber) {
        set(LUCKY_NUMBER, luckyNumber);
        return this;
    }
    
    public Double getSomeDouble() {
        return get(SOME_DOUBLE);
    }
    
    public TestModel setSomeDouble(Double someDouble) {
        set(SOME_DOUBLE, someDouble);
        return this;
    }
    
    public Integer get$123abc() {
        return get($_123_ABC);
    }
    
    public TestModel set$123abc(Integer $123abc) {
        set($_123_ABC, $123abc);
        return this;
    }
    
    @Override
    public TestModel setId(long id) {
        super.setId(id);
        return this;
    }
    
    public String getDisplayName() {
        return TestModelSpec.getDisplayName(this);
    }
    
    public String prefixedName(String prefix) {
        return TestModelSpec.getDisplayNameWithPrefix(this, prefix);
    }
    
    public void testVoidMethod() {
        TestModelSpec.testVoidMethod(this);
    }
    
    public void run() {
        TestModelSpec.run(this);
    }
    
    public Iterator<String> iterator() {
        return TestModelSpec.iterator(this);
    }
    
    public static String someStaticMethod(TestModel instance, TestModel anotherInstance) {
        return TestModelSpec.someStaticMethod(instance, anotherInstance);
    }
    
}
