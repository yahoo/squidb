/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.sql;

import com.yahoo.squidb.data.SquidCursor;
import com.yahoo.squidb.sql.Property.IntegerProperty;
import com.yahoo.squidb.test.Constants;
import com.yahoo.squidb.test.DatabaseTestCase;
import com.yahoo.squidb.test.Employee;
import com.yahoo.squidb.test.TestModel;
import com.yahoo.squidb.test.TestViewModel;
import com.yahoo.squidb.test.Thing;
import com.yahoo.squidb.test.TriggerTester;

import java.util.ArrayList;
import java.util.List;

public class TriggerTest extends DatabaseTestCase {

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
                .setBirthday(now - Constants.WEEK_IN_MILLIS)
                .setLuckyNumber(314);
        database.persist(kevin);
        jonathan = new TestModel()
                .setFirstName("Jonathan")
                .setLastName("Koren")
                .setBirthday(now + Constants.HOUR_IN_MILLIS)
                .setLuckyNumber(3);
        database.persist(jonathan);
        scott = new TestModel()
                .setFirstName("Scott")
                .setLastName("Serrano")
                .setBirthday(now - Constants.DAY_IN_MILLIS * 2)
                .setLuckyNumber(-5);
        database.persist(scott);

        // So that testViewModel will have rows in it -- sqlite triggers are "for each row" only
        database.persist(new Employee().setName("Sam").setIsHappy(true));
    }

    public void testMissingTriggerEventThrowsIllegalStateException() {
        Delete delete = Delete.from(TestModel.TABLE).where(TestModel.IS_HAPPY.isFalse());
        final Trigger trigger = Trigger.after("trigger").when(TestModel.IS_HAPPY.isFalse())
                .perform(delete);

        testThrowsException(new Runnable() {
            public void run() {
                trigger.compile(database.getSqliteVersion());
            }
        }, IllegalStateException.class);
    }

    public void testMissingStatementsThrowsIllegalStateException() {
        final Trigger trigger = Trigger.after("trigger").insertOn(TestModel.TABLE)
                .when(TestModel.IS_HAPPY.isFalse());

        testThrowsException(new Runnable() {
            public void run() {
                trigger.compile(database.getSqliteVersion());
            }
        }, IllegalStateException.class);
    }

    public void testSettingTriggerEventMoreThanOnceThrowsIllegalStateException() {
        testThrowsException(new Runnable() {
            public void run() {
                Trigger.after("trigger").insertOn(TestModel.TABLE).deleteOn(TestModel.TABLE);
            }
        }, IllegalStateException.class);
    }

    public void testTriggerBefore() {
        final int initialValue = 5;
        final int terminalValue = 10;
        TriggerTester test1 = new TriggerTester().setValue1(initialValue);
        TriggerTester test2 = new TriggerTester().setValue1(initialValue);
        database.persist(test1);
        database.persist(test2);
        final long idTest1 = test1.getId();
        final long idTest2 = test2.getId();

        // create trigger set_value2_before before update of value1 on trigger_testers begin
        //      update trigger_testers set value2 = value1 where _id = NEW._id;
        // end;
        Trigger trigger = Trigger.before("set_value2_before")
                .updateOn(TriggerTester.TABLE, TriggerTester.VALUE_1)
                .perform(Update.table(TriggerTester.TABLE).set(TriggerTester.VALUE_2, TriggerTester.VALUE_1)
                        .where(TriggerTester.ID.eq(Trigger.newValueOf(TriggerTester.ID))));
        CompiledStatement compiledTrigger = trigger.compile(database.getSqliteVersion());

        verifyCompiledSqlArgs(compiledTrigger, 0);

        // create trigger
        database.tryExecSql(compiledTrigger.sql, compiledTrigger.sqlArgs);

        // update test1 with db
        test1.setValue1(terminalValue);
        assertTrue(database.persist(test1));

        // update test2 with compiled statement
        Update update = Update.table(TriggerTester.TABLE).set(TriggerTester.VALUE_1, terminalValue)
                .where(TriggerTester.ID.eq(idTest2));
        CompiledStatement compiledUpdate = update.compile(database.getSqliteVersion());
        database.tryExecSql(compiledUpdate.sql, compiledUpdate.sqlArgs);

        test1 = database.fetch(TriggerTester.class, idTest1, TriggerTester.PROPERTIES);
        assertEquals(terminalValue, test1.getValue1().intValue());
        assertEquals(initialValue, test1.getValue2().intValue());

        test2 = database.fetch(TriggerTester.class, idTest2, TriggerTester.PROPERTIES);
        assertEquals(terminalValue, test2.getValue1().intValue());
        assertEquals(initialValue, test2.getValue2().intValue());
    }

    public void testTriggerAfter() {
        final int initialValue = 5;
        final int terminalValue = 10;
        TriggerTester test1 = new TriggerTester().setValue1(initialValue);
        TriggerTester test2 = new TriggerTester().setValue1(initialValue);
        database.persist(test1);
        database.persist(test2);
        final long idTest1 = test1.getId();
        final long idTest2 = test2.getId();

        // create trigger set_value2_after after update of value1 on trigger_testers begin
        //      update trigger_testers set value2 = value1 where _id = NEW._id;
        // end;
        Trigger trigger = Trigger.after("set_value2_after")
                .updateOn(TriggerTester.TABLE, TriggerTester.VALUE_1)
                .perform(Update.table(TriggerTester.TABLE).set(TriggerTester.VALUE_2, TriggerTester.VALUE_1)
                        .where(TriggerTester.ID.eq(Trigger.newValueOf(TriggerTester.ID))));
        CompiledStatement compiledTrigger = trigger.compile(database.getSqliteVersion());

        verifyCompiledSqlArgs(compiledTrigger, 0);

        // create trigger
        database.tryExecSql(compiledTrigger.sql, compiledTrigger.sqlArgs);

        // update test1 with db
        test1.setValue1(terminalValue);
        assertTrue(database.persist(test1));

        // update test2 with compiled statement
        Update update = Update.table(TriggerTester.TABLE).set(TriggerTester.VALUE_1, terminalValue)
                .where(TriggerTester.ID.eq(idTest2));
        CompiledStatement compiledUpdate = update.compile(database.getSqliteVersion());
        database.tryExecSql(compiledUpdate.sql, compiledUpdate.sqlArgs);

        test1 = database.fetch(TriggerTester.class, idTest1, TriggerTester.PROPERTIES);
        assertEquals(terminalValue, test1.getValue1().intValue());
        assertEquals(terminalValue, test1.getValue2().intValue());

        test2 = database.fetch(TriggerTester.class, idTest2, TriggerTester.PROPERTIES);
        assertEquals(terminalValue, test2.getValue1().intValue());
        assertEquals(terminalValue, test2.getValue2().intValue());
    }

    public void testTriggerInsteadOf() {
        String tname1 = "tname1";
        String ename1 = "ename1";

        String tname2 = "tname2";
        String ename2 = "ename2";

        Insert insert = Insert.into(TriggerTester.TABLE)
                .columns(TriggerTester.STR_1, TriggerTester.STR_2)
                .values(Trigger.newValueOf(TestViewModel.TEST_NAME), Trigger.newValueOf(TestViewModel.EMPLOYEE_NAME));
        Trigger insertInsteadOf = Trigger.insteadOf("view_insert")
                .insertOn(TestViewModel.VIEW)
                .perform(insert);

        Update update = Update.table(TriggerTester.TABLE)
                .set(TriggerTester.STR_1, Trigger.newValueOf(TestViewModel.TEST_NAME))
                .set(TriggerTester.STR_2, Trigger.newValueOf(TestViewModel.EMPLOYEE_NAME))
                .where(TriggerTester.ID.eq(Trigger.oldValueOf(TestViewModel.TEST_MODEL_ID)));
        Trigger updateInsteadOf = Trigger.insteadOf("view_update")
                .updateOn(TestViewModel.VIEW, TestViewModel.TEST_NAME, TestViewModel.EMPLOYEE_NAME)
                .perform(update);

        Delete delete = Delete.from(TriggerTester.TABLE)
                .where(TriggerTester.ID.eq(Trigger.oldValueOf(TestViewModel.TEST_MODEL_ID)));
        Trigger deleteInsteadOf = Trigger.insteadOf("view_delete").deleteOn(TestViewModel.VIEW)
                .perform(delete);

        assertTrue(database.tryExecStatement(insertInsteadOf));
        assertTrue(database.tryExecStatement(updateInsteadOf));
        assertTrue(database.tryExecStatement(deleteInsteadOf));

        database.insert(Insert.into(TestViewModel.VIEW)
                .columns(TestViewModel.TEST_NAME, TestViewModel.EMPLOYEE_NAME).values(tname1, ename1));

        assertEquals(1, database.countAll(TriggerTester.class));
        TriggerTester triggerValue = database.fetch(TriggerTester.class, 1);
        assertNotNull(triggerValue);
        assertEquals(tname1, triggerValue.getStr1());
        assertEquals(ename1, triggerValue.getStr2());

        database.update(Update.table(TestViewModel.VIEW)
                .set(TestViewModel.TEST_NAME, tname2)
                .set(TestViewModel.EMPLOYEE_NAME, ename2));

        assertEquals(1, database.countAll(TriggerTester.class));
        triggerValue = database.fetch(TriggerTester.class, 1);
        assertNotNull(triggerValue);
        assertEquals(tname2, triggerValue.getStr1());
        assertEquals(ename2, triggerValue.getStr2());

        database.delete(Delete.from(TestViewModel.VIEW));
        assertEquals(0, database.countAll(TriggerTester.class));
    }

    public void testTriggerWithCriterion() {
        final int threshold = 9000;

        // create trigger record_bar_gt_9k after insert on things when NEW.bar > 9000 begin
        //      insert into trigger_testers (value1, value2) values (NEW._id, NEW.bar);
        // end;
        Insert insert = Insert.into(TriggerTester.TABLE).columns(TriggerTester.VALUE_1, TriggerTester.VALUE_2)
                .values(Trigger.newValueOf(Thing.ID), Trigger.newValueOf(Thing.BAR));
        Trigger trigger = Trigger.after("record_bar_gt_9k")
                .insertOn(Thing.TABLE)
                .when(Trigger.newValueOf(Thing.BAR).gt(threshold))
                .perform(insert);
        CompiledStatement compiledTrigger = trigger.compile(database.getSqliteVersion());

        verifyCompiledSqlArgs(compiledTrigger, 0);

        database.tryExecSql(compiledTrigger.sql, compiledTrigger.sqlArgs);

        // persist new model instances
        Thing thing1 = new Thing().setFoo("small thing").setBar(5);
        assertTrue(database.persist(thing1)); // should not trigger
        Thing thing2 = new Thing().setFoo("big thing").setBar(9001);
        assertTrue(database.persist(thing2)); // should trigger
        Thing thing3 = new Thing().setFoo("bigger thing").setBar(20000);
        assertTrue(database.persist(thing3)); // should trigger

        SquidCursor<TriggerTester> cursor = database.query(TriggerTester.class, Query.select());
        assertTrue(cursor.getCount() > 0);
        try {
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                assertTrue(cursor.get(TriggerTester.VALUE_2) > threshold);
            }
        } finally {
            cursor.close();
        }
    }

    public void testReferencesToOldAndNewColumnValues() {
        IntegerProperty oldLuckyNumber = Trigger.oldValueOf(TestModel.LUCKY_NUMBER);
        IntegerProperty newLuckyNumber = Trigger.newValueOf(TestModel.LUCKY_NUMBER);

        // create trigger log_insert_lucky after insert on testModels begin
        //      insert into trigger_testers (value1, value2) values (0, NEW.luckyNumber);
        // end;
        Trigger logInsertLucky = Trigger.after("log_insert_lucky").insertOn(TestModel.TABLE)
                .perform(Insert.into(TriggerTester.TABLE).columns(TriggerTester.VALUE_1, TriggerTester.VALUE_2)
                        .values(0, newLuckyNumber));

        // create trigger log_delete_lucky after delete on testModels begin
        //      insert into trigger_testers (value1, value2) values (OLD.luckyNumber, 0);
        // end;
        Trigger logDeleteLucky = Trigger.after("log_delete_lucky").deleteOn(TestModel.TABLE)
                .perform(Insert.into(TriggerTester.TABLE).columns(TriggerTester.VALUE_1, TriggerTester.VALUE_2)
                        .values(oldLuckyNumber, 0));

        // create trigger log_update_lucky after update on testModels when OLD.luckyNumber != NEW.luckyNumber begin
        //      insert into trigger_testers (value1, value2) values (OLD.luckyNumber, NEW.luckyNumber);
        // end;
        Trigger logUpdateLucky = Trigger.after("log_update_lucky").updateOn(TestModel.TABLE)
                .when(oldLuckyNumber.neq(newLuckyNumber))
                .perform(Insert.into(TriggerTester.TABLE).columns(TriggerTester.VALUE_1, TriggerTester.VALUE_2)
                        .values(oldLuckyNumber, newLuckyNumber));

        CompiledStatement compiledLogInsert = logInsertLucky.compile(database.getSqliteVersion());
        verifyCompiledSqlArgs(compiledLogInsert, 0);

        CompiledStatement compiledLogDelete = logDeleteLucky.compile(database.getSqliteVersion());
        verifyCompiledSqlArgs(compiledLogDelete, 0);

        CompiledStatement compiledLogUpdate = logUpdateLucky.compile(database.getSqliteVersion());
        verifyCompiledSqlArgs(compiledLogUpdate, 0);

        database.tryExecSql(compiledLogInsert.sql, compiledLogInsert.sqlArgs);
        database.tryExecSql(compiledLogDelete.sql, compiledLogDelete.sqlArgs);
        database.tryExecSql(compiledLogUpdate.sql, compiledLogUpdate.sqlArgs);

        List<Integer> expectedBefore = new ArrayList<Integer>(10);
        List<Integer> expectedAfter = new ArrayList<Integer>(10);

        // insert
        final int randomLuckyNumber = (int) (Math.random() * 1000);
        expectedBefore.add(0);
        expectedAfter.add(randomLuckyNumber);

        TestModel chesterCheetah = new TestModel()
                .setFirstName("Chester")
                .setLastName("Cheetah")
                .setLuckyNumber(randomLuckyNumber);
        assertTrue(database.persist(chesterCheetah)); // +1 trigger

        // delete
        expectedBefore.add(randomLuckyNumber);
        expectedAfter.add(0);
        assertTrue(database.delete(TestModel.class, chesterCheetah.getId())); // +1 trigger

        // update
        SquidCursor<TestModel> modelCursor = database.query(TestModel.class, Query.select(TestModel.LUCKY_NUMBER)
                .orderBy(TestModel.ID.asc()));
        int numTestModels = modelCursor.getCount();
        try {
            for (modelCursor.moveToFirst(); !modelCursor.isAfterLast(); modelCursor.moveToNext()) {
                int luckyNumber = modelCursor.get(TestModel.LUCKY_NUMBER);
                expectedBefore.add(luckyNumber);
                expectedAfter.add(luckyNumber + 1);
            }
        } finally {
            modelCursor.close();
        }

        // update testModels set luckyNumber = (luckyNumber + 1);
        Field<Integer> luckyPlusPlus = Field.field("(" + TestModel.LUCKY_NUMBER.getExpression() + " + 1)");
        Update update = Update.table(TestModel.TABLE).set(TestModel.LUCKY_NUMBER, luckyPlusPlus);
        CompiledStatement compiledUpdate = update.compile(database.getSqliteVersion());

        database.tryExecSql(compiledUpdate.sql, compiledUpdate.sqlArgs); // +numTestModels triggers
        int expectedTriggers = numTestModels + 2;

        // verify
        SquidCursor<TriggerTester> triggerCursor = database.query(TriggerTester.class, Query.select().orderBy(
                TriggerTester.ID.asc()));
        try {
            assertEquals(expectedTriggers, triggerCursor.getCount());

            for (triggerCursor.moveToFirst(); !triggerCursor.isAfterLast(); triggerCursor.moveToNext()) {
                int before = triggerCursor.get(TriggerTester.VALUE_1);
                int after = triggerCursor.get(TriggerTester.VALUE_2);
                assertEquals(expectedBefore.get(triggerCursor.getPosition()).intValue(), before);
                assertEquals(expectedAfter.get(triggerCursor.getPosition()).intValue(), after);
            }
        } finally {
            triggerCursor.close();
        }
    }
}
