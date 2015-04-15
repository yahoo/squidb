/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.data;

import android.database.sqlite.SQLiteConstraintException;

import com.yahoo.squidb.sql.Criterion;
import com.yahoo.squidb.sql.Query;
import com.yahoo.squidb.sql.TableStatement.ConflictAlgorithm;
import com.yahoo.squidb.test.DatabaseTestCase;
import com.yahoo.squidb.test.TestModel;

import java.util.Arrays;
import java.util.List;

public class DatabaseDaoTest extends DatabaseTestCase {

    public void testBasicInsertAndFetch() {
        TestModel model = insertBasicTestModel();

        TestModel fetch = dao.fetch(TestModel.class, model.getId(), TestModel.PROPERTIES);
        assertEquals("Sam", fetch.getFirstName());
        assertEquals("Bosley", fetch.getLastName());
        assertEquals(testDate, fetch.getBirthday().longValue());
    }

    public void testPropertiesAreNullable() {
        TestModel model = insertBasicTestModel();
        model.setFirstName(null);
        model.setLastName(null);

        assertNull(model.getFirstName());
        assertNull(model.getLastName());
        dao.persist(model);

        TestModel fetch = dao.fetch(TestModel.class, model.getId(), TestModel.PROPERTIES);
        assertNull(fetch.getFirstName());
        assertNull(fetch.getLastName());
    }

    public void testBooleanProperties() {
        TestModel model = insertBasicTestModel();
        assertTrue(model.isHappy());

        model.setIsHappy(false);
        assertFalse(model.isHappy());
        dao.persist(model);
        TestModel fetch = dao.fetch(TestModel.class, model.getId(), TestModel.PROPERTIES);
        assertFalse(fetch.isHappy());
    }

    public void testQueriesWithBooleanPropertiesWork() {
        insertBasicTestModel();

        SquidCursor<TestModel> result = dao
                .query(TestModel.class, Query.select(TestModel.PROPERTIES).where(TestModel.IS_HAPPY.isTrue()));
        assertEquals(1, result.getCount());
        result.moveToFirst();
        TestModel model = new TestModel(result);
        assertTrue(model.isHappy());

        model.setIsHappy(false);
        dao.persist(model);
        result.close();

        result = dao.query(TestModel.class, Query.select(TestModel.PROPERTIES).where(TestModel.IS_HAPPY.isFalse()));
        assertEquals(1, result.getCount());
        result.moveToFirst();
        model = new TestModel(result);
        assertFalse(model.isHappy());
    }

    public void testConflict() {
        insertBasicTestModel();

        TestModel conflict = new TestModel();
        conflict.setFirstName("Dave");
        conflict.setLastName("Bosley");

        boolean result = dao.persistWithOnConflict(conflict, ConflictAlgorithm.IGNORE);
        assertFalse(result);
        TestModel shouldntExist = dao.fetchByCriterion(TestModel.class,
                TestModel.FIRST_NAME.eq("Dave").and(TestModel.LAST_NAME.eq("Bosley")),
                TestModel.PROPERTIES);
        assertNull(shouldntExist);
        SQLiteConstraintException expected = null;
        try {
            conflict.clearValue(TestModel.ID);
            dao.persistWithOnConflict(conflict, ConflictAlgorithm.FAIL);
        } catch (SQLiteConstraintException e) {
            expected = e;
        }
        assertNotNull(expected);
    }

    public void testListProperty() {
        TestModel model = insertBasicTestModel();
        List<String> numbers = Arrays.asList("0", "1", "2", "3");
        model.setSomeList(numbers);

        dao.persist(model);

        model = dao.fetch(TestModel.class, model.getId(), TestModel.PROPERTIES);
        List<String> readNumbers = model.getSomeList();
        assertEquals(numbers.size(), readNumbers.size());
        for (int i = 0; i < numbers.size(); i++) {
            assertEquals(numbers.get(i), readNumbers.get(i));
        }
    }

    public void testFetchByQueryResetsLimitAndTable() {
        TestModel model1 = new TestModel().setFirstName("Sam1").setLastName("Bosley1");
        TestModel model2 = new TestModel().setFirstName("Sam2").setLastName("Bosley2");
        TestModel model3 = new TestModel().setFirstName("Sam3").setLastName("Bosley3");
        dao.persist(model1);
        dao.persist(model2);
        dao.persist(model3);

        Query query = Query.select().limit(2, 1);
        TestModel fetched = dao.fetchByQuery(TestModel.class, query);
        assertEquals(model2.getId(), fetched.getId());
        assertEquals(2, query.getLimit());
        assertEquals(1, query.getOffset());
        assertEquals(null, query.getTable());
    }

    public void testEqCaseInsensitive() {
        insertBasicTestModel();

        TestModel fetch = dao.fetchByCriterion(TestModel.class, TestModel.LAST_NAME.eqCaseInsensitive("BOSLEY"),
                TestModel.PROPERTIES);
        assertNotNull(fetch);
    }

    public void testInsertRow() {
        TestModel model = insertBasicTestModel();
        assertNotNull(dao.fetch(TestModel.class, model.getId()));

        dao.delete(TestModel.class, model.getId());
        assertNull(dao.fetch(TestModel.class, model.getId()));

        long modelId = model.getId();
        dao.insertRow(model); // Should reinsert the row with the same id

        assertEquals(modelId, model.getId());
        assertNotNull(dao.fetch(TestModel.class, model.getId()));
        assertEquals(1, dao.count(TestModel.class, Criterion.all));
    }
}
