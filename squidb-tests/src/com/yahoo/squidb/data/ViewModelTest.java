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
import com.yahoo.squidb.test.TestModel;
import com.yahoo.squidb.test.TestSubqueryModel;
import com.yahoo.squidb.test.TestViewModel;
import com.yahoo.squidb.test.ViewlessViewModel;

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

        t1 = new TestModel().setFirstName("Sam").setLuckyNumber(10);
        t2 = new TestModel().setFirstName("Scott").setLuckyNumber(20);
        t3 = new TestModel().setFirstName("Jon").setLuckyNumber(30);

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
            assertEquals(t1.getId(), model.getTestModelId().longValue());
            assertEquals(e1.getId(), model.getEmployeeModelId().longValue());
            assertEquals(t1.getFirstName(), model.getTestName());
            assertEquals(e1.getName(), model.getEmployeeName());
            assertEquals(e1.getName().toUpperCase(), model.getUppercaseName());

            cursor.moveToNext();
            model.readPropertiesFromCursor(cursor);

            assertEquals(t2.getId(), model.getTestModelId().longValue());
            assertEquals(e2.getId(), model.getEmployeeModelId().longValue());
            assertEquals(t2.getFirstName(), model.getTestName());
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
            assertEquals(t1.getId(), model.getTestModelId().longValue());
            assertEquals(e1.getId(), model.getEmployeeModelId().longValue());
            assertEquals(t1.getFirstName(), model.getTestName());
            assertEquals(e1.getName(), model.getEmployeeName());
            assertEquals(e1.getName().toUpperCase(), model.getUppercaseName());

            cursor.moveToNext();
            model.readPropertiesFromCursor(cursor);

            assertEquals(t2.getId(), model.getTestModelId().longValue());
            assertEquals(e2.getId(), model.getEmployeeModelId().longValue());
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

            assertEquals(t1.getId(), testModel.getId());
            assertEquals(t1.getFirstName(), testModel.getFirstName());
            assertFalse(testModel.containsValue(Employee.NAME));
            assertFalse(testModel.containsValue(TestViewModel.UPPERCASE_NAME));

            Employee employee = new Employee();
            model.mapToModel(employee);

            assertEquals(e1.getId(), employee.getId());
            assertEquals(e1.getName(), employee.getName());
            assertFalse(employee.containsValue(TestModel.FIRST_NAME));
            assertFalse(employee.containsValue(TestViewModel.UPPERCASE_NAME));

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
            assertEquals(t1.getId(), model.getTestModelId().longValue());
            assertEquals(e1.getId(), model.getEmployeeModelId().longValue());
            assertEquals(t1.getFirstName(), model.getTestName());
            assertEquals(e1.getName(), model.getEmployeeName());
            assertEquals(e1.getName().toUpperCase(), model.getUppercaseName());

            cursor.moveToNext();
            model.readPropertiesFromCursor(cursor);
            assertEquals(t2.getId(), model.getTestModelId().longValue());
            assertEquals(e2.getId(), model.getEmployeeModelId().longValue());
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

            assertEquals(t1.getId(), testModel.getId());
            assertEquals(t1.getFirstName(), testModel.getFirstName());
            assertFalse(testModel.containsValue(Employee.NAME));
            assertFalse(testModel.containsValue(TestViewModel.UPPERCASE_NAME));

            Employee employee = new Employee();
            model.mapToModel(employee);

            assertEquals(e1.getId(), employee.getId());
            assertEquals(e1.getName(), employee.getName());
            assertFalse(employee.containsValue(TestModel.FIRST_NAME));
            assertFalse(employee.containsValue(TestViewModel.UPPERCASE_NAME));
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

}
