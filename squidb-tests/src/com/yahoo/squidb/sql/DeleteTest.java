/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.sql;

import android.text.format.DateUtils;

import com.yahoo.squidb.test.DatabaseTestCase;
import com.yahoo.squidb.test.TestModel;

public class DeleteTest extends DatabaseTestCase {

    private TestModel sam;
    private TestModel kevin;
    private TestModel jonathan;
    private TestModel scott;

    @Override
    protected void setupDatabase() {
        super.setupDatabase();

        long now = System.currentTimeMillis();
        sam = new TestModel()
                .setFirstName("Sam")
                .setLastName("Bosley")
                .setBirthday(now);
        dao.persist(sam);
        kevin = new TestModel()
                .setFirstName("Kevin")
                .setLastName("Lim")
                .setBirthday(now - DateUtils.WEEK_IN_MILLIS)
                .setLuckyNumber(314);
        dao.persist(kevin);
        jonathan = new TestModel()
                .setFirstName("Jonathan")
                .setLastName("Koren")
                .setBirthday(now + DateUtils.HOUR_IN_MILLIS)
                .setLuckyNumber(3);
        dao.persist(jonathan);
        scott = new TestModel()
                .setFirstName("Scott")
                .setLastName("Serrano")
                .setBirthday(now - DateUtils.DAY_IN_MILLIS * 2)
                .setLuckyNumber(-5);
        dao.persist(scott);
    }

    public void testDeleteWhere() {
        Criterion criterion = TestModel.LUCKY_NUMBER.lte(0);

        // check preconditions
        TestModel shouldBeFound = dao.fetchByCriterion(TestModel.class, criterion, TestModel.PROPERTIES);
        assertNotNull(shouldBeFound);

        // delete from testModels where testModels.luckyNumber <= 0;
        Delete delete = Delete.from(TestModel.TABLE).where(criterion);
        CompiledStatement compiled = delete.compile();
        verifyCompiledSqlArgs(compiled, 1, 0);

        assertEquals(1, dao.delete(delete));

        int numRows = dao.count(TestModel.class, Criterion.all);
        assertEquals(3, numRows);

        TestModel shouldNotBeFound = dao.fetchByCriterion(TestModel.class, criterion, TestModel.PROPERTIES);
        assertNull(shouldNotBeFound);
    }

    public void testDeleteAll() {
        // check preconditions
        int numRows = dao.count(TestModel.class, Criterion.all);
        assertTrue(numRows > 0);

        // delete from testModels
        Delete delete = Delete.from(TestModel.TABLE);
        CompiledStatement compiled = delete.compile();
        verifyCompiledSqlArgs(compiled, 0);

        assertEquals(numRows, dao.delete(delete));

        numRows = dao.count(TestModel.class, Criterion.all);
        assertEquals(0, numRows);
    }
}
