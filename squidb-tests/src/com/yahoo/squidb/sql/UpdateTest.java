/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.sql;

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
        database.persist(sam);
        kevin = new TestModel()
                .setFirstName("Kevin")
                .setLastName("Lim")
                .setBirthday(now - 60 * 60 * 24 * 7)
                .setLuckyNumber(314);
        database.persist(kevin);
        jonathan = new TestModel()
                .setFirstName("Jonathan")
                .setLastName("Koren")
                .setBirthday(now + 60 * 60)
                .setLuckyNumber(3);
        database.persist(jonathan);
        scott = new TestModel()
                .setFirstName("Scott")
                .setLastName("Serrano")
                .setBirthday(now - 60 * 60 * 24 * 2)
                .setLuckyNumber(-5);
        database.persist(scott);
    }

    public void testUpdateWithNoColumnsSpecifiedThrowsIllegalStateException() {
        testThrowsException(new Runnable() {

            @Override
            public void run() {
                Update update = Update.table(TestModel.TABLE).where(TestModel.IS_HAPPY.isTrue());
                update.compile(database.getSqliteVersion());
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
        int numRows = database.countAll(TestModel.class);
        assertTrue(numRows > 0);
        int shouldBeZero = database.count(TestModel.class, TestModel.LUCKY_NUMBER.eq(newLuckyNumber));
        assertEquals(0, shouldBeZero);

        // update testModels set luckyNumber = 99
        Update update = Update.table(TestModel.TABLE).set(new Property<?>[]{TestModel.LUCKY_NUMBER},
                new Integer[]{newLuckyNumber});
        CompiledStatement compiled = update.compile(database.getSqliteVersion());

        verifyCompiledSqlArgs(compiled, 1, newLuckyNumber);

        assertEquals(numRows, database.update(update));

        int rowsWithNewLuckyNumber = database.count(TestModel.class, TestModel.LUCKY_NUMBER.eq(newLuckyNumber));
        assertEquals(numRows, rowsWithNewLuckyNumber);
    }

    public void testUpdateWhere() {
        Criterion criterion = TestModel.LUCKY_NUMBER.lte(0);

        // check preconditions
        int rowsBeforeWithLuckyNumberLteZero = database.count(TestModel.class, criterion);
        assertTrue(rowsBeforeWithLuckyNumberLteZero > 0);

        // update testModels set luckyNumber = 777 where luckyNumber <= 0;
        int luckyNumber = 777;
        Update update = Update.table(TestModel.TABLE).set(TestModel.LUCKY_NUMBER, luckyNumber).where(criterion);
        CompiledStatement compiled = update.compile(database.getSqliteVersion());

        verifyCompiledSqlArgs(compiled, 2, luckyNumber, 0);

        assertEquals(rowsBeforeWithLuckyNumberLteZero, database.update(update));
        int rowsAfterWithLuckyNumberLteZero = database.count(TestModel.class, criterion);
        int rowsWithNewLuckyNumber = database.count(TestModel.class, TestModel.LUCKY_NUMBER.eq(777));
        assertEquals(0, rowsAfterWithLuckyNumberLteZero);
        assertTrue(rowsWithNewLuckyNumber >= rowsBeforeWithLuckyNumberLteZero);
    }

    public void testUpdateWithTemplate() {
        Criterion criterion = TestModel.LUCKY_NUMBER.lte(0);

        // check preconditions
        int rowsBeforeWithLuckyNumberLteZero = database.count(TestModel.class, criterion);
        assertTrue(rowsBeforeWithLuckyNumberLteZero > 0);

        // update testModels set luckyNumber = 777 where luckyNumber <= 0;
        TestModel template = new TestModel().setLuckyNumber(777);
        Update update = Update.table(TestModel.TABLE).fromTemplate(template).where(criterion);
        CompiledStatement compiled = update.compile(database.getSqliteVersion());

        verifyCompiledSqlArgs(compiled, 2, template.getLuckyNumber(), 0);

        assertEquals(rowsBeforeWithLuckyNumberLteZero, database.update(update));
        int rowsAfterWithLuckyNumberLteZero = database.count(TestModel.class, criterion);
        int rowsWithNewLuckyNumber = database.count(TestModel.class, TestModel.LUCKY_NUMBER.eq(777));
        assertEquals(0, rowsAfterWithLuckyNumberLteZero);
        assertTrue(rowsWithNewLuckyNumber >= rowsBeforeWithLuckyNumberLteZero);
    }

    public void testUpdateWithConflictIgnore() {
        final String samLastName = sam.getLastName();
        final String kevinFirstName = kevin.getFirstName();

        // check preconditions
        TestModel modelWithSamLastName = database.fetchByCriterion(TestModel.class, TestModel.LAST_NAME.eq(samLastName),
                TestModel.PROPERTIES);
        TestModel modelWithKevinFirstName = database
                .fetchByCriterion(TestModel.class, TestModel.FIRST_NAME.eq(kevinFirstName),
                        TestModel.PROPERTIES);
        assertNotNull(modelWithSamLastName);
        assertNotNull(modelWithKevinFirstName);
        assertFalse(modelWithSamLastName.equals(modelWithKevinFirstName));

        // update or ignore testModels set lastName = 'Bosley' where firstName = 'Kevin'
        Update update = Update.table(TestModel.TABLE).onConflict(ConflictAlgorithm.IGNORE).set(TestModel.LAST_NAME,
                samLastName).where(TestModel.FIRST_NAME.eq(kevinFirstName));
        CompiledStatement compiled = update.compile(database.getSqliteVersion());

        verifyCompiledSqlArgs(compiled, 2, samLastName, kevinFirstName);

        assertEquals(0, database.update(update)); // Expect ignore

        int shouldBeOne = database.count(TestModel.class, TestModel.LAST_NAME.eq(samLastName));
        assertEquals(1, shouldBeOne);

        modelWithSamLastName = database.fetchByCriterion(TestModel.class, TestModel.LAST_NAME.eq(samLastName),
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
        TestModel modelWithSamLastName = database.fetchByCriterion(TestModel.class, TestModel.LAST_NAME.eq(samLastName),
                TestModel.PROPERTIES);
        TestModel modelWithKevinFirstName = database
                .fetchByCriterion(TestModel.class, TestModel.FIRST_NAME.eq(kevinFirstName),
                        TestModel.PROPERTIES);
        assertNotNull(modelWithSamLastName);
        assertNotNull(modelWithKevinFirstName);
        assertFalse(modelWithSamLastName.equals(modelWithKevinFirstName));

        int shouldBeOne = database.count(TestModel.class, TestModel.LAST_NAME.eq(samLastName));
        assertEquals(1, shouldBeOne);
        shouldBeOne = database.count(TestModel.class, TestModel.FIRST_NAME.eq(kevinFirstName));
        assertEquals(1, shouldBeOne);

        // update or replace testModels set lastName = 'Bosley' where firstName = 'Kevin'
        Update update = Update.table(TestModel.TABLE).onConflict(ConflictAlgorithm.REPLACE).set(TestModel.LAST_NAME,
                samLastName).where(TestModel.FIRST_NAME.eq(kevinFirstName));
        CompiledStatement compiled = update.compile(database.getSqliteVersion());

        verifyCompiledSqlArgs(compiled, 2, samLastName, kevinFirstName);

        assertEquals(1, database.update(update));
        modelWithSamLastName = database.fetchByCriterion(TestModel.class, TestModel.LAST_NAME.eq(samLastName),
                TestModel.PROPERTIES);
        modelWithKevinFirstName = database.fetchByCriterion(TestModel.class, TestModel.FIRST_NAME.eq(kevinFirstName),
                TestModel.PROPERTIES);
        assertNotNull(modelWithSamLastName);
        assertNotNull(modelWithKevinFirstName);
        assertEquals(modelWithSamLastName, modelWithKevinFirstName);
    }

    public void testUpdateWithNonLiteralValue() {
        int[] expectedResults;
        SquidCursor<TestModel> cursor;

        // build expected results
        cursor = database.query(TestModel.class, Query.select(TestModel.LUCKY_NUMBER).orderBy(TestModel.ID.asc()));
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
        CompiledStatement compiled = update.compile(database.getSqliteVersion());

        verifyCompiledSqlArgs(compiled, 0);

        assertEquals(4, database.update(update));

        // verify
        cursor = database.query(TestModel.class, Query.select(TestModel.LUCKY_NUMBER).orderBy(TestModel.ID.asc()));
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
