/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.utility;

import android.annotation.SuppressLint;
import android.provider.BaseColumns;

import com.yahoo.squidb.data.ICursor;
import com.yahoo.squidb.data.SquidCursor;
import com.yahoo.squidb.sql.CompiledStatement;
import com.yahoo.squidb.sql.Field;
import com.yahoo.squidb.sql.Query;
import com.yahoo.squidb.test.DatabaseTestCase;
import com.yahoo.squidb.test.Employee;
import com.yahoo.squidb.test.TestModel;
import com.yahoo.squidb.test.TestSubqueryModel;

import java.util.Arrays;
import java.util.List;

public class ContentProviderQueryBuilderTest extends DatabaseTestCase {

    private static final String COL_GIVEN_NAME = "given_name";
    private static final String COL_SURNAME = "surname";
    private static final String COL_LUCKY_NUMBER = TestModel.LUCKY_NUMBER.getName();
    private static final String COL_IS_HAPPY = TestModel.IS_HAPPY.getName();

    private TestModel model1;
    private TestModel model2;
    private TestModel model3;

    @Override
    protected void setupDatabase() {
        super.setupDatabase();

        model1 = new TestModel().setFirstName("Sam").setLastName("Bosley").setLuckyNumber(21);
        model2 = new TestModel().setFirstName("Jonathan").setLastName("Koren").setLuckyNumber(99);
        model3 = new TestModel().setFirstName("Jack").setLastName("Sparrow").setLuckyNumber(0);
        database.persist(model1);
        database.persist(model2);
        database.persist(model3);
    }

    private ContentProviderQueryBuilder getBuilder() {
        ProjectionMap map = new ProjectionMap();
        map.put(TestModel.ID);
        map.put(COL_GIVEN_NAME, TestModel.FIRST_NAME);
        map.put(COL_SURNAME, TestModel.LAST_NAME);
        map.put(TestModel.LUCKY_NUMBER); // == COL_LUCKY_NUMBER
        map.put(TestModel.IS_HAPPY); // == COL_IS_HAPPY
        return new ContentProviderQueryBuilder().setProjectionMap(map);
    }

    public void testEmptyProjectionWithMapUsesDefault() {
        final Field<?>[] expectedProjection = new Field<?>[]{
                TestModel.ID,
                TestModel.FIRST_NAME.as(COL_GIVEN_NAME),
                TestModel.LAST_NAME.as(COL_SURNAME),
                TestModel.LUCKY_NUMBER,
                TestModel.IS_HAPPY
        };

        ContentProviderQueryBuilder builder = getBuilder();
        Query query = builder.setDataSource(TestModel.TABLE).build(null, null, null, null);
        assertEquals(Arrays.asList(expectedProjection), query.getFields());
    }

    public void testNonEmptyProjectionWithoutMapCreatesFields() {
        final Field<?>[] expectedProjection = new Field<?>[]{Field.field("foo"), Field.field("bar")};
        ContentProviderQueryBuilder builder = new ContentProviderQueryBuilder();
        Query query = builder.setDataSource(TestModel.TABLE).build(new String[]{"foo", "bar"}, null, null, null);
        assertEquals(Arrays.asList(expectedProjection), query.getFields());
    }

    public void testInvalidProjectionIgnored() {
        ContentProviderQueryBuilder builder = getBuilder();
        final String IGNORE = "foo";
        String[] projection = {IGNORE, COL_GIVEN_NAME, COL_SURNAME, COL_LUCKY_NUMBER};
        Query query = builder.setDataSource(TestModel.TABLE).build(projection, null, null, null);
        List<Field<?>> fields = query.getFields();
        assertEquals(3, fields.size());
        for (int i = 0; i < fields.size(); i++) {
            if (IGNORE.equals(fields.get(i).getName())) {
                fail("Invalid projection not ignored!");
            }
        }
    }

    public void testInvalidProjectionInStrictModeThrowsException() {
        final ContentProviderQueryBuilder builder = getBuilder();
        builder.setStrict(true);
        testThrowsException(new Runnable() {
            @Override
            public void run() {
                // try with a malicious projection
                String[] projection = {"* from sqlite_master;--"};
                builder.setDataSource(TestModel.TABLE).build(projection, null, null, null);
            }
        }, IllegalArgumentException.class);
    }

    public void testBuiltQueryNeedsValidation() {
        String selection = COL_LUCKY_NUMBER + " > 50";
        testBuiltQueryNeedsValidation(false, null, false);
        testBuiltQueryNeedsValidation(false, selection, false);
        testBuiltQueryNeedsValidation(true, null, false);
        testBuiltQueryNeedsValidation(true, selection, true);
    }

    private void testBuiltQueryNeedsValidation(boolean strict, String selection, boolean needsValidation) {
        ContentProviderQueryBuilder builder = getBuilder();
        builder.setStrict(strict);
        Query query = builder.setDataSource(TestModel.TABLE).build(null, selection, null, null);
        assertEquals(needsValidation, query.needsValidation());
    }

    public void testRawSelection() {
        String selection = COL_LUCKY_NUMBER + " > ? AND " + COL_IS_HAPPY + " != ?";
        String[] selectionArgs = new String[]{"50", "0"};
        ContentProviderQueryBuilder builder = getBuilder();
        Query query = builder.setDataSource(TestModel.TABLE).build(null, selection, selectionArgs, null);
        CompiledStatement compiled = query.compile(database.getSqliteVersion());
        verifyCompiledSqlArgs(compiled, 2, "50", "0");

        SquidCursor<TestModel> cursor = null;
        try {
            cursor = database.query(TestModel.class, query);
            assertEquals(1, cursor.getCount());
            cursor.moveToFirst();
            assertEquals(model2, buildModelFromCursor(cursor));
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public void testRawOrderBy() {
        String sortOrder = COL_GIVEN_NAME + " ASC";
        ContentProviderQueryBuilder builder = getBuilder();
        Query query = builder.setDataSource(TestModel.TABLE).build(null, null, null, sortOrder);
        CompiledStatement compiled = query.compile(database.getSqliteVersion());
        verifyCompiledSqlArgs(compiled, 0);

        SquidCursor<TestModel> cursor = null;
        try {
            cursor = database.query(TestModel.class, query);
            assertEquals(3, cursor.getCount());
            cursor.moveToFirst();
            assertEquals(model3, buildModelFromCursor(cursor));
            cursor.moveToNext();
            assertEquals(model2, buildModelFromCursor(cursor));
            cursor.moveToNext();
            assertEquals(model1, buildModelFromCursor(cursor));
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public void testDefaultOrderBy() {
        ContentProviderQueryBuilder builder = getBuilder();
        builder.setDefaultOrder(TestModel.LUCKY_NUMBER.desc());
        Query query = builder.setDataSource(TestModel.TABLE).build(null, null, null, null);
        CompiledStatement compiled = query.compile(database.getSqliteVersion());
        verifyCompiledSqlArgs(compiled, 0);

        SquidCursor<TestModel> cursor = null;
        try {
            cursor = database.query(TestModel.class, query);
            assertEquals(3, cursor.getCount());
            cursor.moveToFirst();
            assertEquals(model2, buildModelFromCursor(cursor));
            cursor.moveToNext();
            assertEquals(model1, buildModelFromCursor(cursor));
            cursor.moveToNext();
            assertEquals(model3, buildModelFromCursor(cursor));
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private TestModel buildModelFromCursor(ICursor cursor) {
        TestModel model = new TestModel();
        model.setId(cursor.getLong(cursor.getColumnIndex(BaseColumns._ID)));
        model.setFirstName(cursor.getString(cursor.getColumnIndex(COL_GIVEN_NAME)));
        model.setLastName(cursor.getString(cursor.getColumnIndex(COL_SURNAME)));
        model.setLuckyNumber(cursor.getInt(cursor.getColumnIndex(COL_LUCKY_NUMBER)));
        model.setIsHappy(cursor.getInt(cursor.getColumnIndex(COL_IS_HAPPY)) != 0);
        return model;
    }

    public void testMaliciousSelectionThrowsException() {
        // select ... from testModels where (0) union select * from sqlite_master where (1)
        String selection = "0) union select * from sqlite_master where (1";
        testQueryFailsWithException(selection, null);

        // Clever attacker might recognize that his selection was wrapped by parentheses and try to compensate. Make
        // sure that doesn't work either.
        // select ... from testModels where ((0)) union select * from sqlite_master where ((1))
        selection = "0)) union select * from sqlite_master where ((1";
        testQueryFailsWithException(selection, null);
    }

    public void testMaliciousSelectionAndOrderThrowsException() {
        // select ... from testModels where (0)/* order by */union select * from sqlite_master
        String selection = "0)/*";
        String order = "*/union select * from sqlite_master";
        testQueryFailsWithException(selection, order);

        // Clever attacker might recognize that his selection was wrapped by parentheses and try to compensate. Make
        // sure that doesn't work either.
        // select ... from testModels where ((0))/* order by */union select * from sqlite_master
        selection = "0))/*";
        testQueryFailsWithException(selection, order);
    }

    private void testQueryFailsWithException(String selection, String order) {
        ContentProviderQueryBuilder builder = getBuilder();
        builder.setStrict(true);
        final Query query = builder.setDataSource(TestModel.TABLE).build(null, selection, null, order);

        testThrowsRuntimeException(new Runnable() {
            @Override
            public void run() {
                database.query(TestModel.class, query);
            }
        });
    }

    public void testBuilderFromModel() {
        ContentProviderQueryBuilder builder = new ContentProviderQueryBuilder(TestSubqueryModel.PROPERTIES,
                TestSubqueryModel.SUBQUERY);
        Query query = builder.build(null, null, null, null);
        assertEquals(Arrays.asList(TestSubqueryModel.PROPERTIES), query.getFields());
        assertEquals(TestSubqueryModel.SUBQUERY, query.getTable());
    }

    @SuppressLint("DefaultLocale")
    public void testQueryUsingSubqueryModel() {
        Employee employee1 = new Employee().setName("Big bird");
        Employee employee2 = new Employee().setName("Elmo");
        database.persist(employee1);
        database.persist(employee2);

        ContentProviderQueryBuilder builder = new ContentProviderQueryBuilder(TestSubqueryModel.PROPERTIES,
                TestSubqueryModel.SUBQUERY);
        Query query = builder.build(null, null, null, null);

        SquidCursor<TestSubqueryModel> cursor = null;
        try {
            cursor = database.query(TestSubqueryModel.class, query);
            assertEquals(2, cursor.getCount());

            cursor.moveToFirst();
            TestSubqueryModel model = new TestSubqueryModel(cursor);
            assertEquals(model1.getId(), model.getTestModelId().longValue());
            assertEquals(employee1.getId(), model.getEmployeeModelId().longValue());
            assertEquals(model1.getFirstName(), model.getTestName());
            assertEquals(employee1.getName(), model.getEmployeeName());
            assertEquals(employee1.getName().toUpperCase(), model.getUppercaseName());

            cursor.moveToNext();
            model.readPropertiesFromCursor(cursor);
            assertEquals(model2.getId(), model.getTestModelId().longValue());
            assertEquals(employee2.getId(), model.getEmployeeModelId().longValue());
            assertEquals(model2.getFirstName(), model.getTestName());
            assertEquals(employee2.getName(), model.getEmployeeName());
            assertEquals(employee2.getName().toUpperCase(), model.getUppercaseName());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
}
