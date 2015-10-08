/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.data;

import android.content.ContentValues;

import com.yahoo.squidb.sql.Property;
import com.yahoo.squidb.test.DatabaseTestCase;
import com.yahoo.squidb.test.TestModel;
import com.yahoo.squidb.test.Thing;

import java.util.Arrays;

public class ModelTest extends DatabaseTestCase {

    public void testBasicModelFunctions() {
        TestModel model = new TestModel();

        assertFalse(model.isSaved());
        assertFalse(model.isModified());

        model.setFirstName("Sam");
        model.setLastName("Bosley");

        assertEquals("Sam", model.getFirstName());
        assertEquals("Bosley", model.getLastName());
        assertTrue(model.isModified());

        database.persist(model);
        assertTrue(model.isSaved());
        assertFalse(model.isModified());
    }

    public void testAddedModelMethods() {
        TestModel model = new TestModel();

        model.setFirstName("Sam");
        model.setLastName("Bosley");

        assertEquals("Sam Bosley", model.getDisplayName());
        assertEquals("Mr. Sam Bosley", model.prefixedName("Mr."));
    }

    public void testClone() {
        TestModel model = new TestModel();
        model.setFirstName("Sam");
        TestModel clone = model.clone();

        assertTrue(model != clone);
        assertEquals(clone, model);
        assertEquals("Sam", clone.getFirstName());
    }

    public void testCrudMethods() {
        // insert
        TestModel model = insertBasicTestModel("Sam", "Bosley", testDate);
        assertEquals(1, database.countAll(TestModel.class));

        // query
        final long id = model.getId();
        TestModel fetched = database.fetch(TestModel.class, id, TestModel.PROPERTIES);
        assertNotNull(fetched);

        // update
        model.setFirstName("Jack").setLastName("Sparrow").setBirthday(System.currentTimeMillis());
        assertTrue(database.saveExisting(model));
        assertEquals(1, database.countAll(TestModel.class));

        // delete
        assertTrue(database.delete(TestModel.class, id));
        assertEquals(0, database.countAll(TestModel.class));
    }

    public void testCrudMethodsWithNonDefaultPrimaryKey() {
        Thing thing = new Thing();
        database.persist(thing);
        assertEquals(1, thing.getId());

        Thing fetched = database.fetch(Thing.class, thing.getId(), Thing.PROPERTIES);
        assertNotNull(fetched);

        thing.setFoo("new foo");
        database.persist(thing);
        fetched = database.fetch(Thing.class, thing.getId(), Thing.PROPERTIES);
        assertEquals("new foo", fetched.getFoo());
        assertEquals(1, database.countAll(Thing.class));

        // delete
        assertTrue(database.delete(Thing.class, thing.getId()));
        assertEquals(0, database.countAll(Thing.class));
    }

    public void testDeprecatedPropertiesNotIncluded() {
        for (Property<?> property : TestModel.PROPERTIES) {
            if (TestModel.SOME_DEPRECATED_LONG.equals(property)) {
                fail("The PROPERTIES array contained a deprecated property");
            }
        }
    }

    public void testTypesafeReadFromContentValues() {
        testContentValuesTypes(false);
    }

    public void testTypesafeSetFromContentValues() {
        testContentValuesTypes(true);
    }

    private void testContentValuesTypes(final boolean useSetValues) {
        final ContentValues values = new ContentValues();
        values.put(TestModel.FIRST_NAME.getName(), "A");
        values.put(TestModel.LAST_NAME.getName(), "B");
        values.put(TestModel.BIRTHDAY.getName(), 1); // Putting an int where long expected
        values.put(TestModel.IS_HAPPY.getName(), 1); // Putting an int where boolean expected
        values.put(TestModel.SOME_DOUBLE.getName(), 1); // Putting an int where double expected
        values.put(TestModel.$_123_ABC.getName(), "1"); // Putting a String where int expected

        TestModel fromValues;
        if (useSetValues) {
            fromValues = new TestModel();
            fromValues.setPropertiesFromContentValues(values, TestModel.PROPERTIES);
        } else {
            fromValues = new TestModel(values);
        }

        // Check the types stored in the values
        ValuesStorage checkTypesOn = useSetValues ? fromValues.getSetValues() : fromValues.getDatabaseValues();
        assertTrue(checkTypesOn.get(TestModel.FIRST_NAME.getName()) instanceof String);
        assertTrue(checkTypesOn.get(TestModel.LAST_NAME.getName()) instanceof String);
        assertTrue(checkTypesOn.get(TestModel.BIRTHDAY.getName()) instanceof Long);
        assertTrue(checkTypesOn.get(TestModel.IS_HAPPY.getName()) instanceof Boolean);
        assertTrue(checkTypesOn.get(TestModel.SOME_DOUBLE.getName()) instanceof Double);
        assertTrue(checkTypesOn.get(TestModel.$_123_ABC.getName()) instanceof Integer);

        // Check the types using the model getters
        assertEquals("A", fromValues.getFirstName());
        assertEquals("B", fromValues.getLastName());
        assertEquals(1L, fromValues.getBirthday().longValue());
        assertTrue(fromValues.isHappy());
        assertEquals(1.0, fromValues.getSomeDouble());
        assertEquals(1, fromValues.get$123abc().intValue());

        values.clear();
        values.put(TestModel.IS_HAPPY.getName(), "ABC");
        testThrowsException(new Runnable() {
            @Override
            public void run() {
                if (useSetValues) {
                    new TestModel().setPropertiesFromContentValues(values, TestModel.IS_HAPPY);
                } else {
                    new TestModel(values);
                }
            }
        }, ClassCastException.class);
    }

    public void testValueCoercionAppliesToAllValues() {
        // Make sure the model is initialized with values and setValues
        ContentValues values = new ContentValues();
        values.put(TestModel.FIRST_NAME.getName(), "A");
        TestModel model = new TestModel();
        model.readPropertiesFromContentValues(values, TestModel.FIRST_NAME);
        model.setFirstName("B");

        model.getDefaultValues().put(TestModel.IS_HAPPY.getName(), 1);
        assertTrue(model.isHappy()); // Test default values
        model.getDatabaseValues().put(TestModel.IS_HAPPY.getName(), 0);
        assertFalse(model.isHappy()); // Test database values
        model.getSetValues().put(TestModel.IS_HAPPY.getName(), 1);
        assertTrue(model.isHappy()); // Test set values

        model.getDefaultValues().put(TestModel.IS_HAPPY.getName(), true); // Reset the static variable
    }

    public void testFieldIsDirty() {
        TestModel model = new TestModel();
        model.setFirstName("Sam");
        assertTrue(model.fieldIsDirty(TestModel.FIRST_NAME));
        model.markSaved();
        assertFalse(model.fieldIsDirty(TestModel.FIRST_NAME));
    }

    public void testTransitories() {
        String key1 = "transitory1";
        String key2 = "transitory2";

        TestModel model = new TestModel();

        model.putTransitory(key1, "A");
        model.putTransitory(key2, "B");
        assertTrue(model.hasTransitory(key1));
        assertTrue(model.hasTransitory(key2));

        assertEquals("A", model.getTransitory(key1));
        assertEquals("B", model.getTransitory(key2));

        assertTrue(model.getAllTransitoryKeys().containsAll(Arrays.asList(key1, key2)));

        model.clearTransitory(key1);
        assertFalse(model.hasTransitory(key1));
        assertTrue(model.checkAndClearTransitory(key2));
        assertFalse(model.hasTransitory(key2));
    }
}
