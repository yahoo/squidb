/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.utility;

import com.yahoo.squidb.sql.Field;
import com.yahoo.squidb.test.SquidTestCase;
import com.yahoo.squidb.test.TestModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ProjectionMapTest extends SquidTestCase {

    public void testInvalidPutArgumentsThrowsException() {
        final ProjectionMap map = new ProjectionMap();

        testThrowsException(new Runnable() {
            @Override
            public void run() {
                map.put((Field<?>) null);
            }
        }, IllegalArgumentException.class);

        testThrowsException(new Runnable() {
            @Override
            public void run() {
                map.put((String) null);
            }
        }, IllegalArgumentException.class);

        testThrowsException(new Runnable() {
            @Override
            public void run() {
                map.put("");
            }
        }, IllegalArgumentException.class);

        testThrowsException(new Runnable() {
            @Override
            public void run() {
                map.put(null, TestModel.FIRST_NAME);
            }
        }, IllegalArgumentException.class);

        testThrowsException(new Runnable() {
            @Override
            public void run() {
                map.put("", TestModel.FIRST_NAME);
            }
        }, IllegalArgumentException.class);

        testThrowsException(new Runnable() {
            @Override
            public void run() {
                map.put("foo", null);
            }
        }, IllegalArgumentException.class);
    }

    public void testPutAliasesColumnIfNameDiffers() {
        final String key = "given_name";
        ProjectionMap map = new ProjectionMap();
        map.put(key, TestModel.FIRST_NAME);
        Field<?> field = map.get(key);
        assertEquals(key, field.getName());
    }

    public void testPutUsesFieldName() {
        ProjectionMap map = new ProjectionMap();
        map.put(TestModel.FIRST_NAME);
        assertEquals(TestModel.FIRST_NAME, map.get(TestModel.FIRST_NAME.getName()));
    }

    public void testDefaultProjection() {
        final String BIRTHDAY = "birthday";
        final String IS_HAPPY = "isHappy";
        final List<Field<?>> columns = new ArrayList<>();
        columns.add(TestModel.FIRST_NAME);
        columns.add(TestModel.LAST_NAME);
        columns.add(TestModel.BIRTHDAY.as(BIRTHDAY));
        columns.add(Field.field(IS_HAPPY));
        final String[] columnNames = {TestModel.FIRST_NAME.getName(), TestModel.LAST_NAME.getName(), BIRTHDAY,
                IS_HAPPY};

        ProjectionMap map = new ProjectionMap();
        map.put(TestModel.FIRST_NAME);
        map.put(TestModel.LAST_NAME);
        map.put(BIRTHDAY, TestModel.BIRTHDAY);
        map.put(IS_HAPPY);

        // test names the same
        String[] names = map.getDefaultProjectionNames();
        assertTrue(Arrays.deepEquals(columnNames, names));

        // test fields the same
        List<Field<?>> fields = map.getDefaultProjection();
        assertTrue(fields.equals(columns));
    }

    public void testConstructFromMap() {
        ProjectionMap base = new ProjectionMap();
        base.put(TestModel.FIRST_NAME);
        base.put(TestModel.LAST_NAME);
        base.put("blah");

        ProjectionMap copy = new ProjectionMap(base);
        copy.put("blah2");

        assertEquals(TestModel.FIRST_NAME, copy.get(TestModel.FIRST_NAME.getName()));
        assertEquals(TestModel.LAST_NAME, copy.get(TestModel.LAST_NAME.getName()));
        assertNotNull(copy.get("blah"));
        assertNotNull(copy.get("blah2"));
        assertNull(base.get("blah2"));
    }
}
