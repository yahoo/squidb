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
import com.yahoo.squidb.test.Thing;
import com.yahoo.squidb.utility.VersionCode;

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
                .setBaz(60L * 60 * 24 * 7)
                .setQux(5.5)
                .setIsAlive(true);
        database.persist(thingOne);
        thingTwo = new Thing()
                .setFoo("Thing2")
                .setBar(456)
                .setBaz(System.currentTimeMillis())
                .setQux(17.4)
                .setIsAlive(false);
        database.persist(thingTwo);

        sam = new TestModel()
                .setFirstName("Sam")
                .setLastName("Bosley")
                .setBirthday(System.currentTimeMillis());
        database.persist(sam);
    }

    public void testInsertWithValues() {
        final String fname = "Jack";
        final String lname = "Sparrow";

        // check preconditions
        // last name is unique
        Criterion lastNameSparrow = TestModel.LAST_NAME.eqCaseInsensitive(lname);
        TestModel shouldBeNull = database.fetchByCriterion(TestModel.class, lastNameSparrow, TestModel.PROPERTIES);
        assertNull(shouldBeNull);

        // insert into testModels (firstName, lastName) values ('Jack', 'Sparrow');
        Insert insert = Insert.into(TestModel.TABLE).columns(TestModel.FIRST_NAME, TestModel.LAST_NAME).values(fname,
                lname);
        CompiledStatement compiled = insert.compile(database.getSqliteVersion());

        verifyCompiledSqlArgs(compiled, 2, fname, lname);

        assertEquals(2, database.insert(insert));

        TestModel shouldNotBeNull = database.fetchByCriterion(TestModel.class, lastNameSparrow, TestModel.PROPERTIES);
        assertNotNull(shouldNotBeNull);
        assertEquals(fname, shouldNotBeNull.getFirstName());
        assertEquals(lname, shouldNotBeNull.getLastName());
    }

    public void testInsertMultipleValues() {
        if (database.getSqliteVersion().isLessThan(VersionCode.V3_7_11)) {
            // see testInsertMultipleValuesPreJellybeanThrowsException
            return;
        }

        final String fname1 = "Alan";
        final String lname1 = "Turing";
        final String fname2 = "Linus";
        final String lname2 = "Torvalds";
        Insert insert = Insert.into(TestModel.TABLE)
                .columns(TestModel.FIRST_NAME, TestModel.LAST_NAME)
                .values(fname1, lname1)
                .values(fname2, lname2);

        CompiledStatement compiled = insert.compile(database.getSqliteVersion());
        verifyCompiledSqlArgs(compiled, 4, fname1, lname1, fname2, lname2);

        assertEquals(3, database.insert(insert));

        Criterion where = TestModel.FIRST_NAME.eq(fname1).and(TestModel.LAST_NAME.eq(lname1));
        assertNotNull(database.fetchByCriterion(TestModel.class, where, TestModel.PROPERTIES));
        where = TestModel.FIRST_NAME.eq(fname2).and(TestModel.LAST_NAME.eq(lname2));
        assertNotNull(database.fetchByCriterion(TestModel.class, where, TestModel.PROPERTIES));
    }

    public void testInsertMultipleValuesPreJellybeanThrowsException() {
        if (database.getSqliteVersion().isAtLeast(VersionCode.V3_7_11)) {
            // see testInsertMultipleValues
            return;
        }
        testThrowsException(new Runnable() {
            @Override
            public void run() {
                Insert insert = Insert.into(TestModel.TABLE)
                        .columns(TestModel.FIRST_NAME, TestModel.LAST_NAME)
                        .values("Alan", "Turing")
                        .values("Linus", "Torvalds");
                database.insert(insert);
            }
        }, UnsupportedOperationException.class);
    }

    public void testInsertWithQuery() {
        double pi = Math.PI;

        Criterion criterion = Thing.QUX.gt(pi);
        int numThingsMatching = database.count(Thing.class, criterion);

        // insert into testModels select foo, bar, isAlive from things where qux > 3.1415...;
        Query query = Query.select(Thing.FOO, Thing.BAR, Thing.IS_ALIVE).from(Thing.TABLE).where(criterion);
        Insert insert = Insert.into(TestModel.TABLE).columns(TestModel.LAST_NAME, TestModel.LUCKY_NUMBER,
                TestModel.IS_HAPPY).select(query);
        CompiledStatement compiled = insert.compile(database.getSqliteVersion());

        verifyCompiledSqlArgs(compiled, 1, pi);

        int testModelsBeforeInsert = database.countAll(TestModel.class);
        assertEquals(3, database.insert(insert));
        int testModelsAfterInsert = database.countAll(TestModel.class);
        assertEquals(testModelsBeforeInsert + numThingsMatching, testModelsAfterInsert);
    }

    public void testInsertWithDefaultValues() {
        // insert into things default values;
        Insert insert = Insert.into(Thing.TABLE).defaultValues();
        CompiledStatement compiled = insert.compile(database.getSqliteVersion());

        verifyCompiledSqlArgs(compiled, 0);

        int rowsBeforeInsert = database.countAll(Thing.class);
        assertEquals(3, database.insert(insert));
        int rowsAfterInsert = database.countAll(Thing.class);

        assertEquals(rowsBeforeInsert + 1, rowsAfterInsert);

        // get the newest
        Thing newThing = null;
        SquidCursor<Thing> cursor = null;
        try {
            cursor = database.query(Thing.class, Query.select(Thing.PROPERTIES).orderBy(Order.desc(Thing.ID)).limit(1));
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
        TestModel shouldNotBeNull = database.fetchByCriterion(TestModel.class, TestModel.LAST_NAME.eq(lname),
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
        CompiledStatement compiled = insert.compile(database.getSqliteVersion());

        verifyCompiledSqlArgs(compiled, 4, fname, lname, isHappy, luckyNumber);

        int rowsBeforeInsert = database.countAll(Thing.class);
        assertEquals(-1, database.insert(insert)); // Expect conflict
        int rowsAfterInsert = database.countAll(Thing.class);

        assertEquals(rowsBeforeInsert, rowsAfterInsert);

        TestModel withSamLastName = database.fetchByCriterion(TestModel.class, TestModel.LAST_NAME.eq(lname),
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
        TestModel shouldNotBeNull = database.fetchByCriterion(TestModel.class, TestModel.LAST_NAME.eq(lname),
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
        CompiledStatement compiled = insert.compile(database.getSqliteVersion());

        verifyCompiledSqlArgs(compiled, 4, fname, lname, isHappy, luckyNumber);

        int rowsBeforeInsert = database.countAll(Thing.class);
        assertEquals(rowsBeforeInsert, database.insert(insert)); // Expect replace
        int rowsAfterInsert = database.countAll(Thing.class);

        assertEquals(rowsBeforeInsert, rowsAfterInsert);

        TestModel modelWithSamLastName = database.fetchByCriterion(TestModel.class, TestModel.LAST_NAME.eq(lname),
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
                insert.compile(database.getSqliteVersion());
            }
        }, IllegalStateException.class);
    }

    public void testValuesSpecifiedButColumnsMissingThrowsIllegalStateException() {
        testThrowsException(new Runnable() {

            @Override
            public void run() {
                Insert insert = Insert.into(TestModel.TABLE).values(Integer.valueOf(0xF00), "bar");
                insert.compile(database.getSqliteVersion());
            }
        }, IllegalStateException.class);
    }

    public void testSetsOfValuesOfUnequalSizeThrowsIllegalStateException() {
        if (database.getSqliteVersion().isLessThan(VersionCode.V3_7_11)) {
            // see testInsertMultipleValuesPreJellybeanThrowsException
            return;
        }
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
                insert.compile(database.getSqliteVersion());
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
                insert.compile(database.getSqliteVersion());
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
                insert.compile(database.getSqliteVersion());
            }
        }, IllegalStateException.class);
    }

    public void testNoValuesOptionSpecifiedThrowsIllegalStateException() {
        testThrowsException(new Runnable() {

            @Override
            public void run() {
                // insert into testModels;
                Insert insert = Insert.into(TestModel.TABLE);
                insert.compile(database.getSqliteVersion());
            }
        }, IllegalStateException.class);
    }
}
