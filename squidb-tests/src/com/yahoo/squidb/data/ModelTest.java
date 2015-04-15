/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.data;

import com.yahoo.squidb.sql.Criterion;
import com.yahoo.squidb.test.DatabaseTestCase;
import com.yahoo.squidb.test.TestModel;

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

        dao.persist(model);
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
        assertEquals(1, dao.count(TestModel.class, Criterion.all));

        // query
        final long id = model.getId();
        TestModel fetched = dao.fetch(TestModel.class, id, TestModel.PROPERTIES);
        assertNotNull(fetched);

        // update
        model.setFirstName("Jack").setLastName("Sparrow").setBirthday(System.currentTimeMillis());
        assertTrue(dao.saveExisting(model));
        assertEquals(1, dao.count(TestModel.class, Criterion.all));

        // delete
        assertTrue(dao.delete(TestModel.class, id));
        assertEquals(0, dao.count(TestModel.class, Criterion.all));
    }
}
