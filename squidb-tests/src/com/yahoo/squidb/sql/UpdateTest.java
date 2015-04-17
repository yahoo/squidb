/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.sql;

import android.text.format.DateUtils;

import com.yahoo.squidb.data.SquidCursor;
import com.yahoo.squidb.sql.TableStatement.ConflictAlgorithm;
import com.yahoo.squidb.test.DatabaseTestCase;
import com.yahoo.squidb.test.TestModel;

public class UpdateTest extends DatabaseTestCase {

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

    public void testUpdateWithNoColumnsSpecifiedThrowsIllegalStateException() {
        testThrowsException(new Runnable() {

            @Override
            public void run() {
                Update update = Update.table(TestModel.TABLE).where(TestModel.IS_HAPPY.isTrue());
                update.compile();
            }
        }, IllegalStateException.class);
    }

    public void testUnequalTermsThrowsIllegalArgumentException() {
        testThrowsException(new Runnable() {

            @Override
            public void run() {
                Property<?>[] fields = new Property<?>[]{TestModel.FIRST_NAME, TestModel.LAST_NAME};
                Object[] values = new String[]{"Batman"};
                Update.table(TestModel.TABLE).set(fields, values);
            }
        }, IllegalArgumentException.class);
    }

    public void testUpdateAll() {
        final int newLuckyNumber = 99;

        // check preconditions
        int numRows = dao.count(TestModel.class, Criterion.all);
        assertTrue(numRows > 0);
        int shouldBeZero = dao.count(TestModel.class, TestModel.LUCKY_NUMBER.eq(newLuckyNumber));
        assertEquals(0, shouldBeZero);

        // update testModels set luckyNumber = 99
        Update update = Update.table(TestModel.TABLE).set(new Property<?>[]{TestModel.LUCKY_NUMBER},
                new Integer[]{newLuckyNumber});
        CompiledStatement compiled = update.compile();

        verifyCompiledSqlArgs(compiled, 1, newLuckyNumber);

        assertEquals(numRows, dao.update(update));

        int rowsWithNewLuckyNumber = dao.count(TestModel.class, TestModel.LUCKY_NUMBER.eq(newLuckyNumber));
        assertEquals(numRows, rowsWithNewLuckyNumber);
    }

    public void testUpdateWhere() {
        Criterion criterion = TestModel.LUCKY_NUMBER.lte(0);

        // check preconditions
        int rowsBeforeWithLuckyNumberLteZero = dao.count(TestModel.class, criterion);
        assertTrue(rowsBeforeWithLuckyNumberLteZero > 0);

        // update testModels set luckyNumber = 777 where luckyNumber <= 0;
        int luckyNumber = 777;
        Update update = Update.table(TestModel.TABLE).set(TestModel.LUCKY_NUMBER, luckyNumber).where(criterion);
        CompiledStatement compiled = update.compile();

        verifyCompiledSqlArgs(compiled, 2, luckyNumber, 0);

        assertEquals(rowsBeforeWithLuckyNumberLteZero, dao.update(update));
        int rowsAfterWithLuckyNumberLteZero = dao.count(TestModel.class, criterion);
        int rowsWithNewLuckyNumber = dao.count(TestModel.class, TestModel.LUCKY_NUMBER.eq(777));
        assertEquals(0, rowsAfterWithLuckyNumberLteZero);
        assertTrue(rowsWithNewLuckyNumber >= rowsBeforeWithLuckyNumberLteZero);
    }

    public void testUpdateWithTemplate() {
        Criterion criterion = TestModel.LUCKY_NUMBER.lte(0);

        // check preconditions
        int rowsBeforeWithLuckyNumberLteZero = dao.count(TestModel.class, criterion);
        assertTrue(rowsBeforeWithLuckyNumberLteZero > 0);

        // update testModels set luckyNumber = 777 where luckyNumber <= 0;
        TestModel template = new TestModel().setLuckyNumber(777);
        Update update = Update.table(TestModel.TABLE).fromTemplate(template).where(criterion);
        CompiledStatement compiled = update.compile();

        verifyCompiledSqlArgs(compiled, 2, template.getLuckyNumber(), 0);

        assertEquals(rowsBeforeWithLuckyNumberLteZero, dao.update(update));
        int rowsAfterWithLuckyNumberLteZero = dao.count(TestModel.class, criterion);
        int rowsWithNewLuckyNumber = dao.count(TestModel.class, TestModel.LUCKY_NUMBER.eq(777));
        assertEquals(0, rowsAfterWithLuckyNumberLteZero);
        assertTrue(rowsWithNewLuckyNumber >= rowsBeforeWithLuckyNumberLteZero);
    }

    public void testUpdateWithConflictIgnore() {
        final String samLastName = sam.getLastName();
        final String kevinFirstName = kevin.getFirstName();

        // check preconditions
        TestModel modelWithSamLastName = dao.fetchByCriterion(TestModel.class, TestModel.LAST_NAME.eq(samLastName),
                TestModel.PROPERTIES);
        TestModel modelWithKevinFirstName = dao
                .fetchByCriterion(TestModel.class, TestModel.FIRST_NAME.eq(kevinFirstName),
                        TestModel.PROPERTIES);
        assertNotNull(modelWithSamLastName);
        assertNotNull(modelWithKevinFirstName);
        assertFalse(modelWithSamLastName.equals(modelWithKevinFirstName));

        // update or ignore testModels set lastName = 'Bosley' where firstName = 'Kevin'
        Update update = Update.table(TestModel.TABLE).onConflict(ConflictAlgorithm.IGNORE).set(TestModel.LAST_NAME,
                samLastName).where(TestModel.FIRST_NAME.eq(kevinFirstName));
        CompiledStatement compiled = update.compile();

        verifyCompiledSqlArgs(compiled, 2, samLastName, kevinFirstName);

        assertEquals(0, dao.update(update)); // Expect ignore

        int shouldBeOne = dao.count(TestModel.class, TestModel.LAST_NAME.eq(samLastName));
        assertEquals(1, shouldBeOne);

        modelWithSamLastName = dao.fetchByCriterion(TestModel.class, TestModel.LAST_NAME.eq(samLastName),
                TestModel.PROPERTIES);
        assertNotNull(modelWithSamLastName);

        assertEquals(sam.getFirstName(), modelWithSamLastName.getFirstName());
        assertEquals(samLastName, modelWithSamLastName.getLastName());
        assertEquals(sam.getBirthday(), modelWithSamLastName.getBirthday());
        assertEquals(sam.getLuckyNumber(), modelWithSamLastName.getLuckyNumber());
    }

    public void testUpdateWithConflictReplace() {
        final String samLastName = sam.getLastName();
        final String kevinFirstName = kevin.getFirstName();

        // check preconditions
        TestModel modelWithSamLastName = dao.fetchByCriterion(TestModel.class, TestModel.LAST_NAME.eq(samLastName),
                TestModel.PROPERTIES);
        TestModel modelWithKevinFirstName = dao
                .fetchByCriterion(TestModel.class, TestModel.FIRST_NAME.eq(kevinFirstName),
                        TestModel.PROPERTIES);
        assertNotNull(modelWithSamLastName);
        assertNotNull(modelWithKevinFirstName);
        assertFalse(modelWithSamLastName.equals(modelWithKevinFirstName));

        int shouldBeOne = dao.count(TestModel.class, TestModel.LAST_NAME.eq(samLastName));
        assertEquals(1, shouldBeOne);
        shouldBeOne = dao.count(TestModel.class, TestModel.FIRST_NAME.eq(kevinFirstName));
        assertEquals(1, shouldBeOne);

        // update or replace testModels set lastName = 'Bosley' where firstName = 'Kevin'
        Update update = Update.table(TestModel.TABLE).onConflict(ConflictAlgorithm.REPLACE).set(TestModel.LAST_NAME,
                samLastName).where(TestModel.FIRST_NAME.eq(kevinFirstName));
        CompiledStatement compiled = update.compile();

        verifyCompiledSqlArgs(compiled, 2, samLastName, kevinFirstName);

        assertEquals(1, dao.update(update));
        modelWithSamLastName = dao.fetchByCriterion(TestModel.class, TestModel.LAST_NAME.eq(samLastName),
                TestModel.PROPERTIES);
        modelWithKevinFirstName = dao.fetchByCriterion(TestModel.class, TestModel.FIRST_NAME.eq(kevinFirstName),
                TestModel.PROPERTIES);
        assertNotNull(modelWithSamLastName);
        assertNotNull(modelWithKevinFirstName);
        assertEquals(modelWithSamLastName, modelWithKevinFirstName);
    }

    public void testUpdateWithNonLiteralValue() {
        int[] expectedResults;
        SquidCursor<TestModel> cursor;

        // build expected results
        cursor = dao.query(TestModel.class, Query.select(TestModel.LUCKY_NUMBER).orderBy(TestModel.ID.asc()));
        int numRows = cursor.getCount();
        assertTrue(numRows > 0);
        expectedResults = new int[numRows];
        try {
            int index = 0;
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                int newValue = cursor.get(TestModel.LUCKY_NUMBER) + 1;
                expectedResults[index++] = newValue;
            }
        } finally {
            cursor.close();
        }

        Field<Integer> luckyPlusPlus = Field.field(TestModel.LUCKY_NUMBER.getExpression() + " + 1");
        Update update = Update.table(TestModel.TABLE).set(TestModel.LUCKY_NUMBER, luckyPlusPlus);
        CompiledStatement compiled = update.compile();

        verifyCompiledSqlArgs(compiled, 0);

        assertEquals(4, dao.update(update));

        // verify
        cursor = dao.query(TestModel.class, Query.select(TestModel.LUCKY_NUMBER).orderBy(TestModel.ID.asc()));
        try {
            int index = 0;
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                assertEquals(expectedResults[index++], cursor.get(TestModel.LUCKY_NUMBER).intValue());
            }
        } finally {
            cursor.close();
        }
    }
}
