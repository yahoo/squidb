/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.data;

import com.yahoo.squidb.sql.Property;
import com.yahoo.squidb.sql.Query;
import com.yahoo.squidb.test.DatabaseTestCase;
import com.yahoo.squidb.test.TestEnum;
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

    public void testContainsNonNullValueMethod() {
        TestModel model = new TestModel();
        model.setFirstName("Test");
        model.markSaved(); // Move values from set values into database values
        assertEquals("Test", model.getFirstName());
        assertFalse(model.fieldIsDirty(TestModel.FIRST_NAME));
        assertNull(model.getSetValues());
        assertTrue(model.containsNonNullValue(TestModel.FIRST_NAME));

        model.setFirstName(null);
        // Assert that despite the presence of a non-null value in the database values, containsNonNullValue
        // defers to the "active" setValue.
        assertNotNull(model.getDatabaseValues().get(TestModel.FIRST_NAME.getName()));
        assertNull(model.getFirstName());
        assertFalse(model.containsNonNullValue(TestModel.FIRST_NAME));
    }

    public void testAddedModelMethods() {
        TestModel model = new TestModel();

        model.setFirstName("Sam");
        model.setLastName("Bosley");

        assertEquals("Sam Bosley", model.getDisplayName());
        assertEquals("Mr. Sam Bosley", model.prefixedName("Mr."));
    }

    public void testClone() {
        // Init model with set values, regular values, and transitory values
        TestModel model = new TestModel();
        model.setFirstName("Sam");
        model.markSaved();
        model.setLastName("B");
        model.putTransitory("a", "A");
        TestModel clone = model.clone();
        clone.putTransitory("b", "B");

        assertTrue(model != clone);
        assertTrue(model.getSetValues() != clone.getSetValues());
        assertTrue(model.getDatabaseValues() != clone.getDefaultValues());

        assertEquals(clone, model);
        assertEquals(model.getSetValues(), clone.getSetValues());
        assertEquals(model.getDatabaseValues(), clone.getDatabaseValues());

        assertEquals("Sam", clone.getFirstName());
        assertEquals("B", clone.getLastName());
        assertEquals("A", clone.getTransitory("a"));
        assertEquals("B", clone.getTransitory("b"));
        assertFalse(model.hasTransitory("b"));

        // Test cloning an empty model
        TestModel empty = new TestModel();
        TestModel emptyClone = empty.clone();

        assertNull(emptyClone.getSetValues());
        assertNull(emptyClone.getDatabaseValues());
        assertNull(emptyClone.getAllTransitoryKeys());
    }

    public void testCrudMethods() {
        // insert
        TestModel model = insertBasicTestModel("Sam", "Bosley", testDate);
        assertEquals(1, database.countAll(TestModel.class));

        // query
        final long id = model.getRowId();
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
        assertEquals(1, thing.getRowId());

        Thing fetched = database.fetch(Thing.class, thing.getRowId(), Thing.PROPERTIES);
        assertNotNull(fetched);

        thing.setFoo("new foo");
        database.persist(thing);
        fetched = database.fetch(Thing.class, thing.getRowId(), Thing.PROPERTIES);
        assertEquals("new foo", fetched.getFoo());
        assertEquals(1, database.countAll(Thing.class));

        // delete
        assertTrue(database.delete(Thing.class, thing.getRowId()));
        assertEquals(0, database.countAll(Thing.class));
    }

    public void testDeprecatedPropertiesNotIncluded() {
        for (Property<?> property : TestModel.PROPERTIES) {
            if (TestModel.SOME_DEPRECATED_LONG.equals(property)) {
                fail("The PROPERTIES array contained a deprecated property");
            }
        }
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

        model.putTransitory(key1, "A");
        model.putTransitory(key2, "B");
        model.clearAllTransitory();
        assertFalse(model.hasTransitory(key1));
        assertFalse(model.hasTransitory(key2));

        model.putTransitory(key1, "A");
        model.putTransitory(key2, "B");
        model.clear();
        assertFalse(model.hasTransitory(key1));
        assertFalse(model.hasTransitory(key2));

        // Test null transitory keys and values
        model.putTransitory(key1, null);
        assertTrue(model.hasTransitory(key1));
        assertTrue(model.checkAndClearTransitory(key1));
        assertFalse(model.hasTransitory(key1));

        model.putTransitory(null, "A");
        assertTrue(model.hasTransitory(null));
        assertTrue(model.checkAndClearTransitory(null));
        assertFalse(model.hasTransitory(null));
    }

    public void testEnumProperties() {
        final TestEnum enumValue = TestEnum.APPLE;
        final String enumAsString = enumValue.name();
        TestModel model = new TestModel()
                .setFirstName("A")
                .setLastName("Z")
                .setBirthday(System.currentTimeMillis())
                .setSomeEnum(enumValue);

        ValuesStorage setValues = model.getSetValues();
        assertEquals(enumAsString, setValues.get(TestModel.SOME_ENUM.getName()));

        database.persist(model);

        SquidCursor<TestModel> cursor = database.query(TestModel.class, Query.select()
                .where(TestModel.SOME_ENUM.eq(TestEnum.APPLE)));
        assertEquals(1, cursor.getCount());
        assertTrue(cursor.moveToFirst());
        assertEquals(enumAsString, cursor.get(TestModel.SOME_ENUM));

        TestModel fromDatabase = new TestModel(cursor);
        assertEquals(enumValue, fromDatabase.getSomeEnum());
    }

    public void testNonPublicConstantCopying() {
        assertEquals("somePackageProtectedConst", TestModel.PACKAGE_PROTECTED_CONST);
    }
}
