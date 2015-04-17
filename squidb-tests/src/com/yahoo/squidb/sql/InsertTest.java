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
import com.yahoo.squidb.test.Thing;

public class InsertTest extends DatabaseTestCase {

    Thing thingOne;
    Thing thingTwo;

    TestModel sam;

    @Override
    protected void setupDatabase() {
        super.setupDatabase();

        thingOne = new Thing()
                .setFoo("Thing1")
                .setBar(123)
                .setBaz(DateUtils.WEEK_IN_MILLIS)
                .setQux(5.5)
                .setIsAlive(true);
        dao.persist(thingOne);
        thingTwo = new Thing()
                .setFoo("Thing2")
                .setBar(456)
                .setBaz(System.currentTimeMillis())
                .setQux(17.4)
                .setIsAlive(false);
        dao.persist(thingTwo);

        sam = new TestModel()
                .setFirstName("Sam")
                .setLastName("Bosley")
                .setBirthday(System.currentTimeMillis());
        dao.persist(sam);
    }

    public void testInsertWithValues() {
        final String fname = "Jack";
        final String lname = "Sparrow";

        // check preconditions
        // last name is unique
        Criterion lastNameSparrow = TestModel.LAST_NAME.eqCaseInsensitive(lname);
        TestModel shouldBeNull = dao.fetchByCriterion(TestModel.class, lastNameSparrow, TestModel.PROPERTIES);
        assertNull(shouldBeNull);

        // insert into testModels (firstName, lastName) values ('Jack', 'Sparrow');
        Insert insert = Insert.into(TestModel.TABLE).columns(TestModel.FIRST_NAME, TestModel.LAST_NAME).values(fname,
                lname);
        CompiledStatement compiled = insert.compile();

        verifyCompiledSqlArgs(compiled, 2, fname, lname);

        assertEquals(2, dao.insert(insert));

        TestModel shouldNotBeNull = dao.fetchByCriterion(TestModel.class, lastNameSparrow, TestModel.PROPERTIES);
        assertNotNull(shouldNotBeNull);
        assertEquals(fname, shouldNotBeNull.getFirstName());
        assertEquals(lname, shouldNotBeNull.getLastName());
    }

    public void testInsertWithQuery() {
        double pi = Math.PI;

        Criterion criterion = Thing.QUX.gt(pi);
        int numThingsMatching = dao.count(Thing.class, criterion);

        // insert into testModels select foo, bar, isAlive from things where qux > 3.1415...;
        Query query = Query.select(Thing.FOO, Thing.BAR, Thing.IS_ALIVE).from(Thing.TABLE).where(criterion);
        Insert insert = Insert.into(TestModel.TABLE).columns(TestModel.LAST_NAME, TestModel.LUCKY_NUMBER,
                TestModel.IS_HAPPY).select(query);
        CompiledStatement compiled = insert.compile();

        verifyCompiledSqlArgs(compiled, 1, pi);

        int testModelsBeforeInsert = dao.count(TestModel.class, Criterion.all);
        assertEquals(3, dao.insert(insert));
        int testModelsAfterInsert = dao.count(TestModel.class, Criterion.all);
        assertEquals(testModelsBeforeInsert + numThingsMatching, testModelsAfterInsert);
    }

    public void testInsertWithDefaultValues() {
        // insert into things default values;
        Insert insert = Insert.into(Thing.TABLE).defaultValues();
        CompiledStatement compiled = insert.compile();

        verifyCompiledSqlArgs(compiled, 0);

        int rowsBeforeInsert = dao.count(Thing.class, Criterion.all);
        assertEquals(3, dao.insert(insert));
        int rowsAfterInsert = dao.count(Thing.class, Criterion.all);

        assertEquals(rowsBeforeInsert + 1, rowsAfterInsert);

        // get the newest
        Thing newThing = null;
        SquidCursor<Thing> cursor = null;
        try {
            cursor = dao.query(Thing.class, Query.select(Thing.PROPERTIES).orderBy(Order.desc(Thing.ID)).limit(1));
            if (cursor.moveToFirst()) {
                newThing = new Thing(cursor);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        assertNotNull(newThing);
        assertEquals(Thing.DEFAULT_FOO, newThing.getFoo());
        assertEquals(Thing.DEFAULT_BAR, newThing.getBar().intValue());
        assertEquals(Thing.DEFAULT_IS_ALIVE, newThing.isAlive().booleanValue());
    }

    public void testInsertWithCoflictIgnore() {
        final String lname = sam.getLastName();
        // check preconditions
        // last name is unique
        TestModel shouldNotBeNull = dao.fetchByCriterion(TestModel.class, TestModel.LAST_NAME.eq(lname),
                TestModel.PROPERTIES);
        assertNotNull(shouldNotBeNull);

        // insert or replace into testModels (firstName, lastName, isHappy, luckyNumber) values ("Jack", "Bosley", 0,
        // 777);
        final String fname = "Jack";
        final boolean isHappy = false;
        final int luckyNumber = 777;
        Insert insert = Insert.into(TestModel.TABLE)
                .onConflict(ConflictAlgorithm.IGNORE)
                .columns(TestModel.FIRST_NAME, TestModel.LAST_NAME, TestModel.IS_HAPPY, TestModel.LUCKY_NUMBER)
                .values(fname, lname, isHappy, luckyNumber);
        CompiledStatement compiled = insert.compile();

        verifyCompiledSqlArgs(compiled, 4, fname, lname, isHappy, luckyNumber);

        int rowsBeforeInsert = dao.count(Thing.class, Criterion.all);
        assertEquals(-1, dao.insert(insert)); // Expect conflict
        int rowsAfterInsert = dao.count(Thing.class, Criterion.all);

        assertEquals(rowsBeforeInsert, rowsAfterInsert);

        TestModel withSamLastName = dao.fetchByCriterion(TestModel.class, TestModel.LAST_NAME.eq(lname),
                TestModel.PROPERTIES);
        assertEquals(sam.getFirstName(), withSamLastName.getFirstName());
        assertEquals(sam.getLastName(), withSamLastName.getLastName());
        assertEquals(sam.isHappy(), withSamLastName.isHappy());
        assertEquals(sam.getLuckyNumber(), withSamLastName.getLuckyNumber());
    }

    public void testInsertWithCoflictReplace() {
        final String lname = sam.getLastName();
        // check preconditions
        // last name is unique
        TestModel shouldNotBeNull = dao.fetchByCriterion(TestModel.class, TestModel.LAST_NAME.eq(lname),
                TestModel.PROPERTIES);
        assertNotNull(shouldNotBeNull);

        // insert or replace into testModels (firstName, lastName, isHappy, luckyNumber) values ("Jack", "Bosley", 0,
        // 777);
        final String fname = "Jack";
        final boolean isHappy = false;
        final int luckyNumber = 777;
        Insert insert = Insert.into(TestModel.TABLE)
                .onConflict(ConflictAlgorithm.REPLACE)
                .columns(TestModel.FIRST_NAME, TestModel.LAST_NAME, TestModel.IS_HAPPY, TestModel.LUCKY_NUMBER)
                .values(fname, lname, isHappy, luckyNumber);
        CompiledStatement compiled = insert.compile();

        verifyCompiledSqlArgs(compiled, 4, fname, lname, isHappy, luckyNumber);

        int rowsBeforeInsert = dao.count(Thing.class, Criterion.all);
        assertEquals(rowsBeforeInsert, dao.insert(insert)); // Expect replace
        int rowsAfterInsert = dao.count(Thing.class, Criterion.all);

        assertEquals(rowsBeforeInsert, rowsAfterInsert);

        TestModel modelWithSamLastName = dao.fetchByCriterion(TestModel.class, TestModel.LAST_NAME.eq(lname),
                TestModel.PROPERTIES);
        assertEquals(fname, modelWithSamLastName.getFirstName());
        assertEquals(lname, modelWithSamLastName.getLastName());
        assertEquals(Boolean.valueOf(isHappy), modelWithSamLastName.isHappy());
        assertEquals(Integer.valueOf(luckyNumber), modelWithSamLastName.getLuckyNumber());
    }

    public void testColumnsSpecifiedButValuesMissingThrowsIllegalStateException() {
        testThrowsException(new Runnable() {

            @Override
            public void run() {
                Insert insert = Insert.into(TestModel.TABLE).columns(TestModel.FIRST_NAME, TestModel.LAST_NAME,
                        TestModel.BIRTHDAY);
                insert.compile();
            }
        }, IllegalStateException.class);
    }

    public void testValuesSpecifiedButColumnsMissingThrowsIllegalStateException() {
        testThrowsException(new Runnable() {

            @Override
            public void run() {
                Insert insert = Insert.into(TestModel.TABLE).values(Integer.valueOf(0xF00), "bar");
                insert.compile();
            }
        }, IllegalStateException.class);
    }

    public void testSetsOfValuesOfUnequalSizeThrowsIllegalStateException() {
        testThrowsException(new Runnable() {

            @Override
            public void run() {
                // insert into testModels (firstName, lastName) values ("Jack", "Sparrow"), ("James", "Bond", 007),
                // ("Bugs", "Bunny");
                Object[] values1 = new Object[]{"Jack", "Sparrow"};
                Object[] values2 = new Object[]{"James", "Bond", Integer.valueOf(007)};
                Object[] values3 = new Object[]{"Bugs", "Bunny"};
                Insert insert = Insert.into(TestModel.TABLE).columns(TestModel.FIRST_NAME, TestModel.LAST_NAME)
                        .values(values1).values(values2).values(values3);
                insert.compile();
            }
        }, IllegalStateException.class);
    }

    public void testQuerySpecifiedButColumnsMissingThrowsIllegalStateException() {
        testThrowsException(new Runnable() {

            @Override
            public void run() {
                // insert into testModels select luckyNumber from testModels where luckyNumber = 9;
                Query query = Query.select(TestModel.FIRST_NAME, TestModel.LAST_NAME, TestModel.BIRTHDAY)
                        .from(TestModel.TABLE)
                        .where(TestModel.LUCKY_NUMBER.eq(9));
                Insert insert = Insert.into(TestModel.TABLE).select(query);
                insert.compile();
            }
        }, IllegalStateException.class);
    }

    public void testUnequalQueryPropertiesVsNumberOfColumnsThrowsIllegalStateException() {
        testThrowsException(new Runnable() {

            @Override
            public void run() {
                // insert into testModels (firstName, lastName) select (firstName, lastName, creationDate) from
                // testModels where luckyNumber = 9;
                Query query = Query.select(TestModel.FIRST_NAME, TestModel.LAST_NAME, TestModel.BIRTHDAY)
                        .from(TestModel.TABLE)
                        .where(TestModel.LUCKY_NUMBER.eq(9));
                Insert insert = Insert.into(TestModel.TABLE).columns(TestModel.FIRST_NAME, TestModel.LAST_NAME)
                        .select(query);
                insert.compile();
            }
        }, IllegalStateException.class);
    }

    public void testNoValuesOptionSpecifiedThrowsIllegalStateException() {
        testThrowsException(new Runnable() {

            @Override
            public void run() {
                // insert into testModels;
                Insert insert = Insert.into(TestModel.TABLE);
                insert.compile();
            }
        }, IllegalStateException.class);
    }
}
