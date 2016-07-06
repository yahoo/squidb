/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.data;

import com.yahoo.squidb.sql.Join;
import com.yahoo.squidb.sql.Query;
import com.yahoo.squidb.test.DatabaseTestCase;
import com.yahoo.squidb.test.Employee;
import com.yahoo.squidb.test.TestEnum;
import com.yahoo.squidb.test.TestModel;
import com.yahoo.squidb.test.TestSubqueryModel;
import com.yahoo.squidb.test.TestViewModel;
import com.yahoo.squidb.test.Thing;
import com.yahoo.squidb.test.ThingJoin;
import com.yahoo.squidb.test.ViewlessViewModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ViewModelTest extends DatabaseTestCase {

    private TestModel t1;
    private TestModel t2;
    private TestModel t3;

    private Employee e1;
    private Employee e2;

    @Override
    protected void setupDatabase() {
        super.setupDatabase();

        t1 = new TestModel().setFirstName("Sam").setLuckyNumber(10).setSomeEnum(TestEnum.APPLE);
        t2 = new TestModel().setFirstName("Scott").setLuckyNumber(20).setSomeEnum(TestEnum.BANANA);
        t3 = new TestModel().setFirstName("Jon").setLuckyNumber(30).setSomeEnum(TestEnum.CHERRY);

        e1 = new Employee().setName("Big bird");
        e2 = new Employee().setName("Elmo");

        database.persist(t1);
        database.persist(t2);
        database.persist(t3);
        database.persist(e1);
        database.persist(e2);
    }

    public void testBasicSelectFromView() {
        SquidCursor<TestViewModel> cursor = null;
        try {
            cursor = database.query(TestViewModel.class, Query.select());
            assertEquals(2, cursor.getCount());
            cursor.moveToFirst();
            TestViewModel model = new TestViewModel(cursor);
            assertEquals(t1.getRowId(), model.getTestModelId().longValue());
            assertEquals(e1.getRowId(), model.getEmployeeModelId().longValue());
            assertEquals(t1.getFirstName(), model.getTestName());
            assertEquals(t1.getSomeEnum(), model.getTestEnum());
            assertEquals(e1.getName(), model.getEmployeeName());
            assertEquals(e1.getName().toUpperCase(), model.getUppercaseName());

            cursor.moveToNext();
            model.readPropertiesFromCursor(cursor);

            assertEquals(t2.getRowId(), model.getTestModelId().longValue());
            assertEquals(e2.getRowId(), model.getEmployeeModelId().longValue());
            assertEquals(t2.getFirstName(), model.getTestName());
            assertEquals(t2.getSomeEnum(), model.getTestEnum());
            assertEquals(e2.getName(), model.getEmployeeName());
            assertEquals(e2.getName().toUpperCase(), model.getUppercaseName());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public void testBasicSelectFromSubquery() {
        SquidCursor<TestSubqueryModel> cursor = null;
        try {
            cursor = database.query(TestSubqueryModel.class, Query.select().from(TestSubqueryModel.SUBQUERY));
            assertEquals(2, cursor.getCount());
            cursor.moveToFirst();
            TestSubqueryModel model = new TestSubqueryModel(cursor);
            assertEquals(t1.getRowId(), model.getTestModelId().longValue());
            assertEquals(e1.getRowId(), model.getEmployeeModelId().longValue());
            assertEquals(t1.getFirstName(), model.getTestName());
            assertEquals(t1.getSomeEnum(), model.getTestEnum());
            assertEquals(e1.getName(), model.getEmployeeName());
            assertEquals(e1.getName().toUpperCase(), model.getUppercaseName());

            cursor.moveToNext();
            model.readPropertiesFromCursor(cursor);

            assertEquals(t2.getRowId(), model.getTestModelId().longValue());
            assertEquals(e2.getRowId(), model.getEmployeeModelId().longValue());
            assertEquals(t2.getFirstName(), model.getTestName());
            assertEquals(e2.getName(), model.getEmployeeName());
            assertEquals(e2.getName().toUpperCase(), model.getUppercaseName());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public void testProjectionMapAliasing() {
        assertEquals("blahTestName", TestSubqueryModel.TEST_NAME.getName());
        assertEquals("blahName", TestSubqueryModel.EMPLOYEE_NAME.getName());
        assertEquals("luckyNumber", TestSubqueryModel.TEST_LUCKY_NUMBER.getName());
        assertEquals("blahEnum", TestSubqueryModel.TEST_ENUM.getName());
        assertEquals("uppercase_name", TestSubqueryModel.UPPERCASE_NAME.getName());

        SquidCursor<TestSubqueryModel> cursor = null;
        try {
            cursor = database.query(TestSubqueryModel.class, Query.select().from(TestSubqueryModel.SUBQUERY));
            cursor.moveToFirst();
            TestSubqueryModel model = new TestSubqueryModel(cursor);
            // queried model should have "uppercase_name"
            assertEquals(e1.getName().toUpperCase(), model.getUppercaseName());

            TestModel testModel = model.mapToModel(new TestModel());
            Employee employeeModel = model.mapToModel(new Employee());
            assertEquals(t1.getFirstName(), testModel.getFirstName());
            assertEquals(e1.getName(), employeeModel.getName());
            assertEquals(t1.getLuckyNumber(), testModel.getLuckyNumber());
            assertEquals(t1.getSomeEnum(), testModel.getSomeEnum());
            // neither mapped model should have "uppercase_name"
            assertFalse(t1.containsValue(TestSubqueryModel.UPPERCASE_NAME));
            assertFalse(e1.containsValue(TestSubqueryModel.UPPERCASE_NAME));
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public void testModelMapping() {
        SquidCursor<TestViewModel> cursor = null;
        try {
            cursor = database.query(TestViewModel.class, Query.select());
            cursor.moveToFirst();
            TestViewModel model = new TestViewModel(cursor);

            TestModel testModel = new TestModel();
            model.mapToModel(testModel);

            assertEquals(t1.getRowId(), testModel.getRowId());
            assertEquals(t1.getFirstName(), testModel.getFirstName());
            assertEquals(t1.getSomeEnum(), testModel.getSomeEnum());
            assertFalse(testModel.containsValue(Employee.NAME));
            assertFalse(testModel.containsValue(TestViewModel.UPPERCASE_NAME));

            Employee employee = new Employee();
            model.mapToModel(employee);

            assertEquals(e1.getRowId(), employee.getRowId());
            assertEquals(e1.getName(), employee.getName());
            assertFalse(employee.containsValue(TestModel.FIRST_NAME));
            assertFalse(employee.containsValue(TestViewModel.UPPERCASE_NAME));
            assertFalse(employee.containsValue(TestModel.SOME_ENUM));

            List<AbstractModel> allSources = model.mapToSourceModels();
            assertEquals(2, allSources.size());
            AbstractModel source1 = allSources.get(0);
            AbstractModel source2 = allSources.get(1);
            assertTrue(source1 instanceof TestModel || source2 instanceof TestModel);
            assertTrue(source1 instanceof Employee || source2 instanceof Employee);
            assertFalse(source1.getClass().equals(source2.getClass()));
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public void testViewlessViewModel() {
        SquidCursor<ViewlessViewModel> cursor = null;
        try {
            cursor = database.query(ViewlessViewModel.class, Query.select(ViewlessViewModel.PROPERTIES)
                    .from(TestModel.TABLE)
                    .join(Join.left(Employee.TABLE, TestModel.ID.eq(Employee.ID)))
                    .where(TestModel.FIRST_NAME.gt("S"))
                    .orderBy(TestModel.FIRST_NAME.asc()));

            assertEquals(2, cursor.getCount());
            cursor.moveToFirst();

            ViewlessViewModel model = new ViewlessViewModel(cursor);
            assertEquals(t1.getRowId(), model.getTestModelId().longValue());
            assertEquals(e1.getRowId(), model.getEmployeeModelId().longValue());
            assertEquals(t1.getFirstName(), model.getTestName());
            assertEquals(e1.getName(), model.getEmployeeName());
            assertEquals(e1.getName().toUpperCase(), model.getUppercaseName());

            cursor.moveToNext();
            model.readPropertiesFromCursor(cursor);
            assertEquals(t2.getRowId(), model.getTestModelId().longValue());
            assertEquals(e2.getRowId(), model.getEmployeeModelId().longValue());
            assertEquals(t2.getFirstName(), model.getTestName());
            assertEquals(e2.getName(), model.getEmployeeName());
            assertEquals(e2.getName().toUpperCase(), model.getUppercaseName());

        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public void testViewlessViewModelMapping() {
        SquidCursor<ViewlessViewModel> cursor = null;
        try {
            cursor = database.query(ViewlessViewModel.class, Query.select(ViewlessViewModel.PROPERTIES)
                    .from(TestModel.TABLE)
                    .join(Join.left(Employee.TABLE, TestModel.ID.eq(Employee.ID)))
                    .where(TestModel.FIRST_NAME.gt("S"))
                    .orderBy(TestModel.FIRST_NAME.asc()));
            cursor.moveToFirst();
            ViewlessViewModel model = new ViewlessViewModel(cursor);

            TestModel testModel = new TestModel();
            model.mapToModel(testModel);

            assertEquals(t1.getRowId(), testModel.getRowId());
            assertEquals(t1.getFirstName(), testModel.getFirstName());
            assertFalse(testModel.containsValue(Employee.NAME));
            assertFalse(testModel.containsValue(TestViewModel.UPPERCASE_NAME));

            Employee employee = new Employee();
            model.mapToModel(employee);

            assertEquals(e1.getRowId(), employee.getRowId());
            assertEquals(e1.getName(), employee.getName());
            assertFalse(employee.containsValue(TestModel.FIRST_NAME));
            assertFalse(employee.containsValue(TestViewModel.UPPERCASE_NAME));
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public void testMapToModelWithMultipleAliases() {
        Thing[] things = new Thing[]{
                new Thing().setFoo("Thing 1").setBar(0),
                new Thing().setFoo("Thing 2").setBar(1),
                new Thing().setFoo("Thing 3").setBar(2),
                new Thing().setFoo("Thing 4").setBar(3),
                new Thing().setFoo("Thing 5").setBar(4),
        };
        database.beginTransaction();
        try {
            for (Thing t : things) {
                database.persist(t);
            }
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }

        SquidCursor<ThingJoin> cursor = database.query(ThingJoin.class, Query.select(ThingJoin.PROPERTIES)
                .from(ThingJoin.SUBQUERY).orderBy(ThingJoin.THING_1_ID.asc()));
        try {
            assertEquals(3, cursor.getCount());
            ThingJoin thingJoin = new ThingJoin();
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                int position = cursor.getPosition();
                thingJoin.readPropertiesFromCursor(cursor);

                Thing readThing1 = thingJoin.mapToModel(new Thing(), Thing.TABLE);
                Thing readThing2 = thingJoin.mapToModel(new Thing(), ThingJoin.THING_2);
                Thing readThing3 = thingJoin.mapToModel(new Thing(), ThingJoin.THING_3);

                List<AbstractModel> allReadModels = thingJoin.mapToSourceModels();
                assertEquals(3, allReadModels.size());

                List<Thing> allReadThings = new ArrayList<>(3);
                for (AbstractModel model : allReadModels) {
                    allReadThings.add((Thing) model);
                }
                // Sort by id
                Collections.sort(allReadThings, new Comparator<Thing>() {
                    @Override
                    public int compare(Thing lhs, Thing rhs) {
                        if (lhs.getRowId() == rhs.getRowId()) {
                            return 0;
                        } else if (lhs.getRowId() < rhs.getRowId()) {
                            return -1;
                        } else {
                            return 1;
                        }
                    }
                });

                assertEquals(things[position].getRowId(), readThing1.getRowId());
                assertEquals(things[position].getFoo(), readThing1.getFoo());
                assertEquals(things[position].getBar(), readThing1.getBar());
                assertEquals(allReadThings.get(0).getRowId(), readThing1.getRowId());
                assertEquals(allReadThings.get(0).getFoo(), readThing1.getFoo());
                assertEquals(allReadThings.get(0).getBar(), readThing1.getBar());
                assertEquals(things[position], readThing1);
                assertEquals(allReadThings.get(0), readThing1);

                assertEquals(things[position + 1].getRowId(), readThing2.getRowId());
                assertEquals(things[position + 1].getFoo(), readThing2.getFoo());
                assertEquals(things[position + 1].getBar(), readThing2.getBar());
                assertEquals(allReadThings.get(1).getRowId(), readThing2.getRowId());
                assertEquals(allReadThings.get(1).getFoo(), readThing2.getFoo());
                assertEquals(allReadThings.get(1).getBar(), readThing2.getBar());
                assertEquals(things[position + 1], readThing2);
                assertEquals(allReadThings.get(1), readThing2);

                assertEquals(things[position + 2].getRowId(), readThing3.getRowId());
                assertEquals(things[position + 2].getFoo(), readThing3.getFoo());
                assertEquals(things[position + 2].getBar(), readThing3.getBar());
                assertEquals(allReadThings.get(2).getRowId(), readThing3.getRowId());
                assertEquals(allReadThings.get(2).getFoo(), readThing3.getFoo());
                assertEquals(allReadThings.get(2).getBar(), readThing3.getBar());
                assertEquals(things[position + 2], readThing3);
                assertEquals(allReadThings.get(2), readThing3);

            }
        } finally {
            cursor.close();
        }
    }

}
