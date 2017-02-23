/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.data;

import com.yahoo.squidb.json.JSONProperty;
import com.yahoo.squidb.sql.Property;
import com.yahoo.squidb.test.BasicData;
import com.yahoo.squidb.test.DatabaseTestCase;
import com.yahoo.squidb.test.Employee;
import com.yahoo.squidb.test.SpecificData;
import com.yahoo.squidb.test.TestConstraint;
import com.yahoo.squidb.test.TestModel;
import com.yahoo.squidb.test.TestMultiColumnKey;
import com.yahoo.squidb.test.TestNonIntegerPrimaryKey;
import com.yahoo.squidb.test.TestSubqueryModel;
import com.yahoo.squidb.test.TestViewModel;
import com.yahoo.squidb.test.TestVirtualModel;
import com.yahoo.squidb.test.Thing;
import com.yahoo.squidb.test.ThingJoin;
import com.yahoo.squidb.test.TriggerTester;
import com.yahoo.squidb.test.ViewlessViewModel;

import java.util.Arrays;

public class ModelSchemasTest extends DatabaseTestCase {

    // --- Table schemas

    public void testTestModelSchema() {
        assertTrue(Property.LongProperty.class.equals(TestModel.ID.getClass()));
        assertTrue(Property.StringProperty.class.equals(TestModel.FIRST_NAME.getClass()));
        assertTrue(Property.StringProperty.class.equals(TestModel.LAST_NAME.getClass()));
        assertTrue(Property.LongProperty.class.equals(TestModel.BIRTHDAY.getClass()));
        assertTrue(Property.BooleanProperty.class.equals(TestModel.IS_HAPPY.getClass()));
        assertTrue(Property.IntegerProperty.class.equals(TestModel.LUCKY_NUMBER.getClass()));
        assertTrue(Property.DoubleProperty.class.equals(TestModel.SOME_DOUBLE.getClass()));
        assertTrue(Property.IntegerProperty.class.equals(TestModel.$_123_ABC.getClass()));
        assertTrue(Property.EnumProperty.class.equals(TestModel.SOME_ENUM.getClass()));
        assertTrue(JSONProperty.class.equals(TestModel.SOME_LIST.getClass()));
        assertTrue(JSONProperty.class.equals(TestModel.SOME_MAP.getClass()));
        assertTrue(JSONProperty.class.equals(TestModel.COMPLICATED_MAP.getClass()));
        assertTrue(JSONProperty.class.equals(TestModel.SOME_POJO.getClass()));

        assertTrue(Property.LongProperty.class.equals(TestModel.SOME_DEPRECATED_LONG.getClass()));

        assertTrue(TestModel.TABLE.getRowIdProperty() == TestModel.ID);
        assertEquals("testModels", TestModel.TABLE.getExpression());
        assertEquals("UNIQUE(creationDate) ON CONFLICT REPLACE", TestModel.TABLE.getTableConstraint());

        assertEquals("_id", TestModel.ID.getExpression());
        assertEquals("firstName", TestModel.FIRST_NAME.getExpression());
        assertEquals("lastName", TestModel.LAST_NAME.getExpression());
        assertEquals("creationDate", TestModel.BIRTHDAY.getExpression());
        assertEquals("isHappy", TestModel.IS_HAPPY.getExpression());
        assertEquals("luckyNumber", TestModel.LUCKY_NUMBER.getExpression());
        assertEquals("someDouble", TestModel.SOME_DOUBLE.getExpression());
        assertEquals("dollar123abc", TestModel.$_123_ABC.getExpression());
        assertEquals("someEnum", TestModel.SOME_ENUM.getExpression());
        assertEquals("someList", TestModel.SOME_LIST.getExpression());
        assertEquals("someMap", TestModel.SOME_MAP.getExpression());
        assertEquals("complicatedMap", TestModel.COMPLICATED_MAP.getExpression());
        assertEquals("somePojo", TestModel.SOME_POJO.getExpression());

        assertEquals("someDeprecatedLong", TestModel.SOME_DEPRECATED_LONG.getExpression());

        assertEquals("PRIMARY KEY AUTOINCREMENT", TestModel.ID.getColumnDefinition());
        assertEquals("DEFAULT NULL", TestModel.FIRST_NAME.getColumnDefinition());
        assertEquals("UNIQUE COLLATE NOCASE", TestModel.LAST_NAME.getColumnDefinition());
        assertEquals(null, TestModel.BIRTHDAY.getColumnDefinition());
        assertEquals("NOT NULL DEFAULT 1", TestModel.IS_HAPPY.getColumnDefinition());
        assertEquals("NOT NULL COLLATE BINARY DEFAULT 7", TestModel.LUCKY_NUMBER.getColumnDefinition());
        assertEquals(null, TestModel.SOME_DOUBLE.getColumnDefinition());
        assertEquals(null, TestModel.$_123_ABC.getColumnDefinition());
        assertEquals(null, TestModel.SOME_ENUM.getColumnDefinition());
        assertEquals("DEFAULT '[]'", TestModel.SOME_LIST.getColumnDefinition());
        assertEquals(null, TestModel.SOME_MAP.getColumnDefinition());
        assertEquals(null, TestModel.COMPLICATED_MAP.getColumnDefinition());
        assertEquals(null, TestModel.SOME_POJO.getColumnDefinition());

        assertEquals(null, TestModel.SOME_DEPRECATED_LONG.getColumnDefinition());

        ValuesStorage defaultValues = new TestModel().getDefaultValues();
        assertEquals(null, defaultValues.get(TestModel.FIRST_NAME.getName()));
        assertEquals(true, defaultValues.get(TestModel.IS_HAPPY.getName()));
        assertEquals(7, defaultValues.get(TestModel.LUCKY_NUMBER.getName()));
        assertEquals("[]", defaultValues.get(TestModel.SOME_LIST.getName()));

        assertEquals(Arrays.asList(TestModel.ID, TestModel.FIRST_NAME, TestModel.LAST_NAME, TestModel.BIRTHDAY,
                TestModel.IS_HAPPY, TestModel.LUCKY_NUMBER, TestModel.SOME_DOUBLE, TestModel.$_123_ABC,
                TestModel.SOME_ENUM, TestModel.SOME_LIST, TestModel.SOME_MAP, TestModel.COMPLICATED_MAP,
                TestModel.SOME_POJO), TestModel.PROPERTIES);

        String createTableSql = "CREATE TABLE IF NOT EXISTS testModels(_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "firstName TEXT DEFAULT NULL, lastName TEXT UNIQUE COLLATE NOCASE, creationDate INTEGER, isHappy "
                + "INTEGER NOT NULL DEFAULT 1, luckyNumber INTEGER NOT NULL COLLATE BINARY DEFAULT 7, someDouble REAL, "
                + "dollar123abc INTEGER, someEnum TEXT, someList TEXT DEFAULT '[]', someMap TEXT, complicatedMap TEXT, "
                + "somePojo TEXT, UNIQUE(creationDate) ON CONFLICT REPLACE)";
        assertEquals(createTableSql, TestModel.TABLE.getCreateTableSql(database.getCompileContext()));
    }

    public void testThingSchema() {
        assertTrue(Property.LongProperty.class.equals(Thing.ID.getClass()));
        assertTrue(Property.StringProperty.class.equals(Thing.FOO.getClass()));
        assertTrue(Property.IntegerProperty.class.equals(Thing.BAR.getClass()));
        assertTrue(Property.LongProperty.class.equals(Thing.BAZ.getClass()));
        assertTrue(Property.DoubleProperty.class.equals(Thing.QUX.getClass()));
        assertTrue(Property.BlobProperty.class.equals(Thing.BLOB.getClass()));

        assertTrue(Thing.TABLE.getRowIdProperty() == Thing.ID);
        assertEquals("things", Thing.TABLE.getExpression());
        assertEquals("PRIMARY KEY(id)", Thing.TABLE.getTableConstraint());

        assertEquals("id", Thing.ID.getExpression());
        assertEquals("foo", Thing.FOO.getExpression());
        assertEquals("bar", Thing.BAR.getExpression());
        assertEquals("baz", Thing.BAZ.getExpression());
        assertEquals("qux", Thing.QUX.getExpression());
        assertEquals("isAlive", Thing.IS_ALIVE.getExpression());
        assertEquals("blob", Thing.BLOB.getExpression());

        assertEquals(null, Thing.ID.getColumnDefinition());
        assertEquals("DEFAULT 'thing'", Thing.FOO.getColumnDefinition());
        assertEquals("DEFAULT 100", Thing.BAR.getColumnDefinition());
        assertEquals(null, Thing.BAZ.getColumnDefinition());
        assertEquals("DEFAULT 0.0", Thing.QUX.getColumnDefinition());
        assertEquals("DEFAULT 1", Thing.IS_ALIVE.getColumnDefinition());
        assertEquals("DEFAULT x'123ABC'", Thing.BLOB.getColumnDefinition());

        ValuesStorage defaultValues = new Thing().getDefaultValues();
        assertEquals("thing", defaultValues.get(Thing.FOO.getName()));
        assertEquals(100, defaultValues.get(Thing.BAR.getName()));
        assertEquals(0.0, defaultValues.get(Thing.QUX.getName()));
        assertEquals(true, defaultValues.get(Thing.IS_ALIVE.getName()));

        assertEquals(Arrays.asList(Thing.ID, Thing.FOO, Thing.BAR, Thing.BAZ, Thing.QUX, Thing.IS_ALIVE, Thing.BLOB,
                Thing.TIMESTAMP), Thing.PROPERTIES);

        String createTableSql = "CREATE TABLE IF NOT EXISTS things(id INTEGER, foo TEXT DEFAULT 'thing', "
                + "bar INTEGER DEFAULT 100, baz INTEGER, qux REAL DEFAULT 0.0, isAlive INTEGER DEFAULT 1, "
                + "blob BLOB DEFAULT x'123ABC', timestamp TEXT DEFAULT CURRENT_TIMESTAMP, PRIMARY KEY(id))";
        assertEquals(createTableSql, Thing.TABLE.getCreateTableSql(database.getCompileContext()));
    }

    public void testEmployeeSchema() {
        assertTrue(Property.LongProperty.class.equals(Employee.ID.getClass()));
        assertTrue(Property.StringProperty.class.equals(Employee.NAME.getClass()));
        assertTrue(Property.LongProperty.class.equals(Employee.MANAGER_ID.getClass()));
        assertTrue(Property.BooleanProperty.class.equals(Employee.IS_HAPPY.getClass()));

        assertTrue(Employee.TABLE.getRowIdProperty() == Employee.ID);
        assertEquals("employees", Employee.TABLE.getExpression());
        assertEquals(null, Employee.TABLE.getTableConstraint());

        assertEquals("_id", Employee.ID.getExpression());
        assertEquals("name", Employee.NAME.getExpression());
        assertEquals("managerId", Employee.MANAGER_ID.getExpression());
        assertEquals("isHappy", Employee.IS_HAPPY.getExpression());

        assertEquals("PRIMARY KEY AUTOINCREMENT", Employee.ID.getColumnDefinition());
        assertEquals("NOT NULL", Employee.NAME.getColumnDefinition());
        assertEquals(null, Employee.MANAGER_ID.getColumnDefinition());
        assertEquals("DEFAULT 1", Employee.IS_HAPPY.getColumnDefinition());

        ValuesStorage defaultValues = new Employee().getDefaultValues();
        assertEquals(true, defaultValues.get(Employee.IS_HAPPY.getName()));

        assertEquals(Arrays.asList(Employee.ID, Employee.NAME, Employee.MANAGER_ID, Employee.IS_HAPPY),
                Employee.PROPERTIES);

        String createTableSql = "CREATE TABLE IF NOT EXISTS employees(_id INTEGER PRIMARY KEY AUTOINCREMENT, name "
                + "TEXT NOT NULL, managerId INTEGER, isHappy INTEGER DEFAULT 1)";
        assertEquals(createTableSql, Employee.TABLE.getCreateTableSql(database.getCompileContext()));
    }

    public void testTriggerTesterSchema() {
        assertTrue(Property.LongProperty.class.equals(TriggerTester.ID.getClass()));
        assertTrue(Property.IntegerProperty.class.equals(TriggerTester.VALUE_1.getClass()));
        assertTrue(Property.IntegerProperty.class.equals(TriggerTester.VALUE_2.getClass()));
        assertTrue(Property.StringProperty.class.equals(TriggerTester.STR_1.getClass()));
        assertTrue(Property.StringProperty.class.equals(TriggerTester.STR_2.getClass()));

        assertTrue(TriggerTester.TABLE.getRowIdProperty() == TriggerTester.ID);
        assertEquals("trigger_testers", TriggerTester.TABLE.getExpression());
        assertEquals(null, TriggerTester.TABLE.getTableConstraint());

        assertEquals("_id", TriggerTester.ID.getExpression());
        assertEquals("value1", TriggerTester.VALUE_1.getExpression());
        assertEquals("value2", TriggerTester.VALUE_2.getExpression());
        assertEquals("str1", TriggerTester.STR_1.getExpression());
        assertEquals("str2", TriggerTester.STR_2.getExpression());

        assertEquals("PRIMARY KEY AUTOINCREMENT", TriggerTester.ID.getColumnDefinition());
        assertEquals("DEFAULT 0", TriggerTester.VALUE_1.getColumnDefinition());
        assertEquals("DEFAULT 0", TriggerTester.VALUE_2.getColumnDefinition());
        assertEquals(null, TriggerTester.STR_1.getColumnDefinition());
        assertEquals(null, TriggerTester.STR_2.getColumnDefinition());

        ValuesStorage defaultValues = new TriggerTester().getDefaultValues();
        assertEquals(0, defaultValues.get(TriggerTester.VALUE_1.getName()));
        assertEquals(0, defaultValues.get(TriggerTester.VALUE_2.getName()));

        assertEquals(Arrays.asList(TriggerTester.ID, TriggerTester.VALUE_1, TriggerTester.VALUE_2, TriggerTester.STR_1,
                TriggerTester.STR_2), TriggerTester.PROPERTIES);

        String createTableSql = "CREATE TABLE IF NOT EXISTS trigger_testers(_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "value1 INTEGER DEFAULT 0, value2 INTEGER DEFAULT 0, str1 TEXT, str2 TEXT)";
        assertEquals(createTableSql, TriggerTester.TABLE.getCreateTableSql(database.getCompileContext()));
    }

    public void testBasicDataSchema() {
        assertTrue(Property.LongProperty.class.equals(BasicData.DATA_ID.getClass()));
        assertTrue(Property.StringProperty.class.equals(BasicData.DATA_1.getClass()));
        assertTrue(Property.StringProperty.class.equals(BasicData.DATA_2.getClass()));
        assertTrue(Property.StringProperty.class.equals(BasicData.DATA_3.getClass()));
        assertTrue(Property.IntegerProperty.class.equals(BasicData.TYPE.getClass()));
        assertTrue(Property.EnumProperty.class.equals(BasicData.SOME_ENUM.getClass()));

        assertTrue(BasicData.TABLE.getRowIdProperty() == BasicData.DATA_ID);
        assertEquals("data", BasicData.TABLE.getExpression());
        assertEquals(null, BasicData.TABLE.getTableConstraint());

        assertEquals("dataId", BasicData.DATA_ID.getExpression());
        assertEquals("data1", BasicData.DATA_1.getExpression());
        assertEquals("data2", BasicData.DATA_2.getExpression());
        assertEquals("data3", BasicData.DATA_3.getExpression());
        assertEquals("type", BasicData.TYPE.getExpression());
        assertEquals("someEnum", BasicData.SOME_ENUM.getExpression());

        assertEquals("PRIMARY KEY AUTOINCREMENT", BasicData.DATA_ID.getColumnDefinition());
        assertEquals(null, BasicData.DATA_1.getColumnDefinition());
        assertEquals(null, BasicData.DATA_2.getColumnDefinition());
        assertEquals(null, BasicData.DATA_3.getColumnDefinition());
        assertEquals(null, BasicData.TYPE.getColumnDefinition());
        assertEquals(null, BasicData.SOME_ENUM.getColumnDefinition());

        ValuesStorage defaultValues = new BasicData().getDefaultValues();
        assertEquals(0, defaultValues.size());

        assertEquals(Arrays.asList(BasicData.DATA_ID, BasicData.DATA_1, BasicData.DATA_2, BasicData.DATA_3,
                BasicData.TYPE, BasicData.SOME_ENUM), BasicData.PROPERTIES);

        String createTableSql = "CREATE TABLE IF NOT EXISTS data(dataId INTEGER PRIMARY KEY AUTOINCREMENT, data1 TEXT, "
                + "data2 TEXT, data3 TEXT, type INTEGER, someEnum TEXT)";
        assertEquals(createTableSql, BasicData.TABLE.getCreateTableSql(database.getCompileContext()));
    }

    public void testTestVirtualModelSchema() {
        assertTrue(Property.LongProperty.class.equals(TestVirtualModel.ROWID.getClass()));
        assertTrue(Property.LongProperty.class.equals(TestVirtualModel.TEST_NUMBER.getClass()));
        assertTrue(Property.StringProperty.class.equals(TestVirtualModel.TITLE.getClass()));
        assertTrue(Property.StringProperty.class.equals(TestVirtualModel.BODY.getClass()));

        assertTrue(TestVirtualModel.TABLE.getRowIdProperty() == TestVirtualModel.ROWID);
        assertEquals("virtual_models", TestVirtualModel.TABLE.getExpression());
        assertEquals(null, TestVirtualModel.TABLE.getTableConstraint());
        assertEquals("fts4", TestVirtualModel.TABLE.getModuleName());

        assertEquals("rowid", TestVirtualModel.ROWID.getExpression());
        assertEquals("test_num", TestVirtualModel.TEST_NUMBER.getExpression());
        assertEquals("title", TestVirtualModel.TITLE.getExpression());
        assertEquals("body", TestVirtualModel.BODY.getExpression());

        assertEquals(null, TestVirtualModel.ROWID.getColumnDefinition());
        assertEquals("DEFAULT 7", TestVirtualModel.TEST_NUMBER.getColumnDefinition());
        assertEquals("DEFAULT NULL", TestVirtualModel.TITLE.getColumnDefinition());
        assertEquals("DEFAULT NULL", TestVirtualModel.BODY.getColumnDefinition());

        ValuesStorage defaultValues = new TestVirtualModel().getDefaultValues();
        assertEquals(7L, defaultValues.get(TestVirtualModel.TEST_NUMBER.getName()));
        assertEquals(null, defaultValues.get(TestVirtualModel.TITLE.getName()));
        assertEquals(null, defaultValues.get(TestVirtualModel.BODY.getName()));

        assertEquals(Arrays.asList(TestVirtualModel.ROWID, TestVirtualModel.TEST_NUMBER, TestVirtualModel.TITLE,
                TestVirtualModel.BODY), TestVirtualModel.PROPERTIES);

        String createTableSql = "CREATE VIRTUAL TABLE IF NOT EXISTS virtual_models USING fts4(test_num,title,body)";
        assertEquals(createTableSql, TestVirtualModel.TABLE.getCreateTableSql(database.getCompileContext()));
    }

    public void testTestNonIntegerPrimaryKeySchema() {
        assertTrue(Property.LongProperty.class.equals(TestNonIntegerPrimaryKey.ROWID.getClass()));
        assertTrue(Property.StringProperty.class.equals(TestNonIntegerPrimaryKey.KEY.getClass()));
        assertTrue(Property.StringProperty.class.equals(TestNonIntegerPrimaryKey.VALUE.getClass()));

        assertTrue(TestNonIntegerPrimaryKey.TABLE.getRowIdProperty() == TestNonIntegerPrimaryKey.ROWID);
        assertEquals("testNonIntegerPrimaryKey", TestNonIntegerPrimaryKey.TABLE.getExpression());
        assertEquals(null, TestNonIntegerPrimaryKey.TABLE.getTableConstraint());

        assertEquals("rowid", TestNonIntegerPrimaryKey.ROWID.getExpression());
        assertEquals("keyCol", TestNonIntegerPrimaryKey.KEY.getExpression());
        assertEquals("value", TestNonIntegerPrimaryKey.VALUE.getExpression());

        assertEquals(null, TestNonIntegerPrimaryKey.ROWID.getColumnDefinition());
        assertEquals("PRIMARY KEY NOT NULL", TestNonIntegerPrimaryKey.KEY.getColumnDefinition());
        assertEquals(null, TestNonIntegerPrimaryKey.VALUE.getColumnDefinition());

        ValuesStorage defaultValues = new TestNonIntegerPrimaryKey().getDefaultValues();
        assertEquals(0, defaultValues.size());

        assertEquals(Arrays.asList(TestNonIntegerPrimaryKey.ROWID, TestNonIntegerPrimaryKey.KEY,
                TestNonIntegerPrimaryKey.VALUE), TestNonIntegerPrimaryKey.PROPERTIES);

        String createTableSql = "CREATE TABLE IF NOT EXISTS testNonIntegerPrimaryKey(keyCol TEXT PRIMARY KEY NOT NULL, "
                + "value TEXT)";
        assertEquals(createTableSql, TestNonIntegerPrimaryKey.TABLE.getCreateTableSql(database.getCompileContext()));
    }

    public void testTestMultiColumnKeySchema() {
        assertTrue(Property.LongProperty.class.equals(TestMultiColumnKey.ROWID.getClass()));
        assertTrue(Property.StringProperty.class.equals(TestMultiColumnKey.KEY_COL_1.getClass()));
        assertTrue(Property.StringProperty.class.equals(TestMultiColumnKey.KEY_COL_2.getClass()));
        assertTrue(Property.StringProperty.class.equals(TestMultiColumnKey.KEY_COL_3.getClass()));
        assertTrue(Property.StringProperty.class.equals(TestMultiColumnKey.OTHER_DATA.getClass()));

        assertTrue(TestMultiColumnKey.TABLE.getRowIdProperty() == TestMultiColumnKey.ROWID);
        assertEquals("multiColumnKey", TestMultiColumnKey.TABLE.getExpression());
        assertEquals("PRIMARY KEY(keyCol1, keyCol2, keyCol3)", TestMultiColumnKey.TABLE.getTableConstraint());

        assertEquals("rowid", TestMultiColumnKey.ROWID.getExpression());
        assertEquals("keyCol1", TestMultiColumnKey.KEY_COL_1.getExpression());
        assertEquals("keyCol2", TestMultiColumnKey.KEY_COL_2.getExpression());
        assertEquals("keyCol3", TestMultiColumnKey.KEY_COL_3.getExpression());
        assertEquals("otherData", TestMultiColumnKey.OTHER_DATA.getExpression());

        assertEquals(null, TestMultiColumnKey.ROWID.getColumnDefinition());
        assertEquals("NOT NULL", TestMultiColumnKey.KEY_COL_1.getColumnDefinition());
        assertEquals("NOT NULL", TestMultiColumnKey.KEY_COL_2.getColumnDefinition());
        assertEquals("NOT NULL", TestMultiColumnKey.KEY_COL_3.getColumnDefinition());
        assertEquals(null, TestMultiColumnKey.OTHER_DATA.getColumnDefinition());

        ValuesStorage defaultValues = new TestMultiColumnKey().getDefaultValues();
        assertEquals(0, defaultValues.size());

        assertEquals(Arrays.asList(TestMultiColumnKey.ROWID, TestMultiColumnKey.KEY_COL_1, TestMultiColumnKey.KEY_COL_2,
                TestMultiColumnKey.KEY_COL_3, TestMultiColumnKey.OTHER_DATA),
                TestMultiColumnKey.PROPERTIES);

        String createTableSql = "CREATE TABLE IF NOT EXISTS multiColumnKey(keyCol1 TEXT NOT NULL, keyCol2 TEXT NOT "
                + "NULL, keyCol3 TEXT NOT NULL, otherData TEXT, PRIMARY KEY(keyCol1, keyCol2, keyCol3))";
        assertEquals(createTableSql, TestMultiColumnKey.TABLE.getCreateTableSql(database.getCompileContext()));
    }

    public void testTestConstraintModelSchema() {
        assertTrue(Property.LongProperty.class.equals(TestConstraint.ROWID.getClass()));
        assertTrue(Property.LongProperty.class.equals(TestConstraint.SOME_LONG.getClass()));
        assertTrue(Property.StringProperty.class.equals(TestConstraint.SOME_STRING.getClass()));
        assertTrue(Property.IntegerProperty.class.equals(TestConstraint.SOME_INT.getClass()));
        assertTrue(Property.DoubleProperty.class.equals(TestConstraint.SOME_DOUBLE.getClass()));
        assertTrue(Property.BooleanProperty.class.equals(TestConstraint.SOME_BOOLEAN.getClass()));
        assertTrue(Property.BlobProperty.class.equals(TestConstraint.SOME_BLOB.getClass()));

        assertTrue(TestConstraint.TABLE.getRowIdProperty() == TestConstraint.ROWID);
        assertEquals("testConstraints", TestConstraint.TABLE.getExpression());
        assertEquals("UNIQUE(some_long, some_string), UNIQUE(some_string COLLATE NOCASE DESC), CHECK(some_long > 0), "
                + "CHECK(some_int < 0), UNIQUE(some_long, some_boolean), CHECK(some_double != 1), "
                + "CHECK(some_blob != x''), PRIMARY KEY(some_string), UNIQUE(some_double) ON CONFLICT ROLLBACK, "
                + "CHECK(some_boolean != 2)",
                TestConstraint.TABLE.getTableConstraint());

        assertEquals("rowid", TestConstraint.ROWID.getExpression());
        assertEquals("some_long", TestConstraint.SOME_LONG.getExpression());
        assertEquals("some_string", TestConstraint.SOME_STRING.getExpression());
        assertEquals("some_int", TestConstraint.SOME_INT.getExpression());
        assertEquals("some_double", TestConstraint.SOME_DOUBLE.getExpression());
        assertEquals("some_boolean", TestConstraint.SOME_BOOLEAN.getExpression());
        assertEquals("some_blob", TestConstraint.SOME_BLOB.getExpression());

        assertEquals(null, TestConstraint.ROWID.getColumnDefinition());
        assertEquals("UNIQUE ON CONFLICT ABORT NOT NULL DEFAULT 1", TestConstraint.SOME_LONG.getColumnDefinition());
        assertEquals("NOT NULL COLLATE NOCASE", TestConstraint.SOME_STRING.getColumnDefinition());
        assertEquals("UNIQUE ON CONFLICT FAIL DEFAULT -1", TestConstraint.SOME_INT.getColumnDefinition());
        assertEquals("CHECK(some_double != 0.0) DEFAULT 1.5", TestConstraint.SOME_DOUBLE.getColumnDefinition());
        assertEquals("DEFAULT 1", TestConstraint.SOME_BOOLEAN.getColumnDefinition());
        assertEquals("UNIQUE ON CONFLICT IGNORE NOT NULL DEFAULT X'ABCDEF'", TestConstraint.SOME_BLOB.getColumnDefinition());

        ValuesStorage defaultValues = new TestConstraint().getDefaultValues();
        assertEquals(1L, defaultValues.get(TestConstraint.SOME_LONG.getName()));
        assertEquals(-1, defaultValues.get(TestConstraint.SOME_INT.getName()));
        assertEquals(1.5, defaultValues.get(TestConstraint.SOME_DOUBLE.getName()));
        assertEquals(true, defaultValues.get(TestConstraint.SOME_BOOLEAN.getName()));

        String createTableSql = "CREATE TABLE IF NOT EXISTS testConstraints(some_long INTEGER UNIQUE ON CONFLICT ABORT "
                + "NOT NULL DEFAULT 1, some_string TEXT NOT NULL COLLATE NOCASE, some_int INTEGER UNIQUE ON CONFLICT "
                + "FAIL DEFAULT -1, some_double REAL CHECK(some_double != 0.0) DEFAULT 1.5, some_boolean INTEGER "
                + "DEFAULT 1, some_blob BLOB UNIQUE ON CONFLICT IGNORE NOT NULL DEFAULT X'ABCDEF', "
                + "UNIQUE(some_long, some_string), UNIQUE(some_string COLLATE NOCASE DESC), CHECK(some_long > 0), "
                + "CHECK(some_int < 0), UNIQUE(some_long, some_boolean), CHECK(some_double != 1), "
                + "CHECK(some_blob != x''), PRIMARY KEY(some_string), UNIQUE(some_double) ON CONFLICT ROLLBACK, "
                + "CHECK(some_boolean != 2))";
        assertEquals(createTableSql, TestConstraint.TABLE.getCreateTableSql(database.getCompileContext()));
    }

    // --- view schemas

    public void testTestViewModelSchema() {
        assertTrue(Property.LongProperty.class.equals(TestViewModel.TEST_MODEL_ID.getClass()));
        assertTrue(Property.LongProperty.class.equals(TestViewModel.EMPLOYEE_MODEL_ID.getClass()));
        assertTrue(Property.StringProperty.class.equals(TestViewModel.TEST_NAME.getClass()));
        assertTrue(Property.StringProperty.class.equals(TestViewModel.EMPLOYEE_NAME.getClass()));
        assertTrue(Property.StringProperty.class.equals(TestViewModel.UPPERCASE_NAME.getClass()));
        assertTrue(Property.EnumProperty.class.equals(TestViewModel.TEST_ENUM.getClass()));
        assertTrue(JSONProperty.class.equals(TestViewModel.JSON_PROP.getClass()));
        assertTrue(JSONProperty.class.equals(TestViewModel.CRAZY_MAP.getClass()));

        assertEquals("testView", TestViewModel.VIEW.getName());

        assertEquals("testModelsId", TestViewModel.TEST_MODEL_ID.getName());
        assertEquals("employeesId", TestViewModel.EMPLOYEE_MODEL_ID.getName());
        assertEquals("firstName", TestViewModel.TEST_NAME.getName());
        assertEquals("name", TestViewModel.EMPLOYEE_NAME.getName());
        assertEquals("uppercase_name", TestViewModel.UPPERCASE_NAME.getName());
        assertEquals("someEnum", TestViewModel.TEST_ENUM.getName());
        assertEquals("somePojo", TestViewModel.JSON_PROP.getName());
        assertEquals("complicatedMap", TestViewModel.CRAZY_MAP.getName());

        assertEquals(Arrays.asList(TestViewModel.TEST_MODEL_ID, TestViewModel.EMPLOYEE_MODEL_ID,
                TestViewModel.TEST_NAME, TestViewModel.EMPLOYEE_NAME, TestViewModel.UPPERCASE_NAME,
                TestViewModel.TEST_ENUM, TestViewModel.JSON_PROP, TestViewModel.CRAZY_MAP),
                TestViewModel.PROPERTIES);
        assertEquals(TestViewModel.PROPERTIES, TestViewModel.VIEW.getProperties());
    }

    public void testTestSubqueryModelSchema() {
        assertTrue(Property.LongProperty.class.equals(TestSubqueryModel.TEST_MODEL_ID.getClass()));
        assertTrue(Property.LongProperty.class.equals(TestSubqueryModel.EMPLOYEE_MODEL_ID.getClass()));
        assertTrue(Property.StringProperty.class.equals(TestSubqueryModel.TEST_NAME.getClass()));
        assertTrue(Property.StringProperty.class.equals(TestSubqueryModel.EMPLOYEE_NAME.getClass()));
        assertTrue(Property.IntegerProperty.class.equals(TestSubqueryModel.TEST_LUCKY_NUMBER.getClass()));
        assertTrue(Property.EnumProperty.class.equals(TestViewModel.TEST_ENUM.getClass()));
        assertTrue(Property.StringProperty.class.equals(TestSubqueryModel.UPPERCASE_NAME.getClass()));

        assertEquals("subquery", TestSubqueryModel.SUBQUERY.getName());

        assertEquals("testModelsId", TestSubqueryModel.TEST_MODEL_ID.getName());
        assertEquals("employeesId", TestSubqueryModel.EMPLOYEE_MODEL_ID.getName());
        assertEquals("blahTestName", TestSubqueryModel.TEST_NAME.getName());
        assertEquals("blahName", TestSubqueryModel.EMPLOYEE_NAME.getName());
        assertEquals("luckyNumber", TestSubqueryModel.TEST_LUCKY_NUMBER.getName());
        assertEquals("blahEnum", TestSubqueryModel.TEST_ENUM.getName());
        assertEquals("uppercase_name", TestSubqueryModel.UPPERCASE_NAME.getName());

        assertEquals(Arrays.asList(TestSubqueryModel.TEST_MODEL_ID, TestSubqueryModel.EMPLOYEE_MODEL_ID,
                TestSubqueryModel.TEST_NAME, TestSubqueryModel.EMPLOYEE_NAME, TestSubqueryModel.TEST_LUCKY_NUMBER,
                TestSubqueryModel.TEST_ENUM, TestSubqueryModel.UPPERCASE_NAME),
                TestSubqueryModel.PROPERTIES);
        assertEquals(TestSubqueryModel.PROPERTIES, TestSubqueryModel.SUBQUERY.getProperties());
    }

    public void testThingJoinSchema() {
        assertTrue(Property.LongProperty.class.equals(ThingJoin.THING_1_ID.getClass()));
        assertTrue(Property.LongProperty.class.equals(ThingJoin.THING_2_ID.getClass()));
        assertTrue(Property.LongProperty.class.equals(ThingJoin.THING_3_ID.getClass()));
        assertTrue(Property.StringProperty.class.equals(ThingJoin.THING_1_FOO.getClass()));
        assertTrue(Property.StringProperty.class.equals(ThingJoin.THING_2_FOO.getClass()));
        assertTrue(Property.StringProperty.class.equals(ThingJoin.THING_3_FOO.getClass()));
        assertTrue(Property.IntegerProperty.class.equals(ThingJoin.THING_1_BAR.getClass()));
        assertTrue(Property.IntegerProperty.class.equals(ThingJoin.THING_2_BAR.getClass()));
        assertTrue(Property.IntegerProperty.class.equals(ThingJoin.THING_3_BAR.getClass()));

        assertEquals("thingJoin", ThingJoin.SUBQUERY.getName());

        assertEquals("id_1", ThingJoin.THING_1_ID.getName());
        assertEquals("id_2", ThingJoin.THING_2_ID.getName());
        assertEquals("id_3", ThingJoin.THING_3_ID.getName());
        assertEquals("foo_1", ThingJoin.THING_1_FOO.getName());
        assertEquals("foo_2", ThingJoin.THING_2_FOO.getName());
        assertEquals("foo_3", ThingJoin.THING_3_FOO.getName());
        assertEquals("bar_1", ThingJoin.THING_1_BAR.getName());
        assertEquals("bar_2", ThingJoin.THING_2_BAR.getName());
        assertEquals("bar_3", ThingJoin.THING_3_BAR.getName());

        assertEquals(Arrays.asList(ThingJoin.THING_1_ID, ThingJoin.THING_2_ID, ThingJoin.THING_3_ID,
                ThingJoin.THING_1_FOO, ThingJoin.THING_2_FOO, ThingJoin.THING_3_FOO,
                ThingJoin.THING_1_BAR, ThingJoin.THING_2_BAR, ThingJoin.THING_3_BAR),
                ThingJoin.PROPERTIES);
        assertEquals(ThingJoin.PROPERTIES, ThingJoin.SUBQUERY.getProperties());
    }

    public void testViewlessViewModelSchema() {
        assertTrue(Property.LongProperty.class.equals(ViewlessViewModel.TEST_MODEL_ID.getClass()));
        assertTrue(Property.LongProperty.class.equals(ViewlessViewModel.EMPLOYEE_MODEL_ID.getClass()));
        assertTrue(Property.StringProperty.class.equals(ViewlessViewModel.TEST_NAME.getClass()));
        assertTrue(Property.StringProperty.class.equals(ViewlessViewModel.EMPLOYEE_NAME.getClass()));
        assertTrue(Property.StringProperty.class.equals(ViewlessViewModel.UPPERCASE_NAME.getClass()));

        assertEquals("viewlessViewModel", ViewlessViewModel.VIEW_NAME);

        assertEquals("testModelsId", ViewlessViewModel.TEST_MODEL_ID.getName());
        assertEquals("employeesId", ViewlessViewModel.EMPLOYEE_MODEL_ID.getName());
        assertEquals("firstName", ViewlessViewModel.TEST_NAME.getName());
        assertEquals("name", ViewlessViewModel.EMPLOYEE_NAME.getName());
        assertEquals("uppercase_name", ViewlessViewModel.UPPERCASE_NAME.getName());

        assertEquals(Arrays.asList(ViewlessViewModel.TEST_MODEL_ID, ViewlessViewModel.EMPLOYEE_MODEL_ID,
                ViewlessViewModel.TEST_NAME, ViewlessViewModel.EMPLOYEE_NAME, ViewlessViewModel.UPPERCASE_NAME),
                ViewlessViewModel.PROPERTIES);
    }

    // --- inherited model schemas

    public void testSpecificDataSchema() {
        assertTrue(BasicData.class.isAssignableFrom(SpecificData.class));

        assertTrue(BasicData.DATA_1 == SpecificData.FIRST_NAME);
        assertTrue(BasicData.DATA_2 == SpecificData.LAST_NAME);
        assertTrue(BasicData.DATA_3 == SpecificData.ADDRESS);
        assertTrue(BasicData.SOME_ENUM == SpecificData.MY_ENUM);

        assertEquals(BasicData.PROPERTIES, SpecificData.PROPERTIES);
    }
}
