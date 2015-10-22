/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.data;

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
