/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.sql;

import com.yahoo.squidb.data.SquidCursor;
import com.yahoo.squidb.sql.Property.IntegerProperty;
import com.yahoo.squidb.sql.Property.LongProperty;
import com.yahoo.squidb.sql.Property.StringProperty;
import com.yahoo.squidb.test.DatabaseTestCase;
import com.yahoo.squidb.test.Employee;
import com.yahoo.squidb.test.TestModel;
import com.yahoo.squidb.test.TestViewModel;
import com.yahoo.squidb.test.Thing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class QueryTest extends DatabaseTestCase {

    Employee bigBird;
    Employee cookieMonster;
    Employee elmo;
    Employee oscar;
    Employee bert;
    Employee ernie;

    @Override
    protected void setupDatabase() {
        super.setupDatabase();

        bigBird = new Employee();
        bigBird.setName("bigBird").setManagerId(0L);
        dao.persist(bigBird);

        cookieMonster = new Employee();
        cookieMonster.setName("cookieMonster").setManagerId(bigBird.getId());
        dao.persist(cookieMonster);

        elmo = new Employee();
        elmo.setName("elmo").setManagerId(bigBird.getId());
        dao.persist(elmo);

        oscar = new Employee();
        oscar.setName("oscar").setManagerId(bigBird.getId()).setIsHappy(false);
        dao.persist(oscar);

        bert = new Employee();
        bert.setName("bert").setManagerId(cookieMonster.getId());
        dao.persist(bert);

        ernie = new Employee();
        ernie.setName("ernie").setManagerId(bert.getId());
        dao.persist(ernie);
    }

    public void testSelectionArgsGeneration() {
        Query query = Query.select(TestModel.PROPERTIES)
                .where(TestModel.FIRST_NAME.eq("Sam")
                        .and(TestModel.BIRTHDAY.gt(17))
                        .and(TestModel.LAST_NAME.neq("Smith")));

        CompiledStatement compiledQuery = query.compile();
        verifyCompiledSqlArgs(compiledQuery, 3, "Sam", 17, "Smith");
    }

    public void testOrderByField() {
        TestModel one = new TestModel().setFirstName("Sam").setLastName("Bosley");
        TestModel two = new TestModel().setFirstName("Kevin").setLastName("Lim");
        TestModel three = new TestModel().setFirstName("Jonathan").setLastName("Koren");

        dao.persist(one);
        dao.persist(two);
        dao.persist(three);

        String[] nameOrder = new String[]{"Kevin", "Sam", "Jonathan"};
        SquidCursor<TestModel> nameOrderCursor = dao.query(TestModel.class, Query.select(TestModel.PROPERTIES)
                .orderBy(Order.byArray(TestModel.FIRST_NAME, nameOrder)));
        try {
            assertEquals(3, nameOrderCursor.getCount());
            for (nameOrderCursor.moveToFirst(); !nameOrderCursor.isAfterLast(); nameOrderCursor.moveToNext()) {
                assertEquals(nameOrder[nameOrderCursor.getPosition()], nameOrderCursor.get(TestModel.FIRST_NAME));
            }
        } finally {
            nameOrderCursor.close();
        }

        Long[] idOrder = new Long[]{3L, 1L, 2L};
        SquidCursor<TestModel> idOrderCursor = dao.query(TestModel.class, Query.select(TestModel.PROPERTIES)
                .orderBy(Order.byArray(TestModel.ID, idOrder)));
        try {
            assertEquals(3, idOrderCursor.getCount());
            for (idOrderCursor.moveToFirst(); !idOrderCursor.isAfterLast(); idOrderCursor.moveToNext()) {
                assertEquals(idOrder[idOrderCursor.getPosition()], idOrderCursor.get(TestModel.ID));
            }
        } finally {
            idOrderCursor.close();
        }
    }

    public void testLikeWithNoEscape() {
        insertBasicTestModel();
        assertEquals(1, dao.count(TestModel.class, TestModel.LAST_NAME.like("Bo_le%")));
        assertEquals(0, dao.count(TestModel.class, TestModel.LAST_NAME.like("%leyx")));
    }

    public void testLikeWithEscape() {
        TestModel model = insertBasicTestModel();
        model.setFirstName("S_a%m");
        dao.persist(model);

        assertEquals(1, dao.count(TestModel.class, TestModel.FIRST_NAME.like("%\\_a\\%%", '\\')));
    }

    public void testLikeSubquery() {
        insertBasicTestModel("Sam 1", "A", System.currentTimeMillis() - 5);
        insertBasicTestModel("Sam 2", "B", System.currentTimeMillis() - 4);
        insertBasicTestModel("Sam 3", "C", System.currentTimeMillis() - 3);
        insertBasicTestModel("Bla 1", "D", System.currentTimeMillis() - 2);
        insertBasicTestModel("Bla 2", "E", System.currentTimeMillis() - 1);

        Function<String> substr = Function.substr(TestModel.FIRST_NAME, 1, 3);
        Function<String> strConcat = Function.strConcat(substr, "%");
        Query likeFirstName = Query.select().where(TestModel.FIRST_NAME.like(
                Query.select(strConcat).from(TestModel.TABLE).where(TestModel.ID.eq(1)))).orderBy(TestModel.ID.asc());

        SquidCursor<TestModel> cursor = dao.query(TestModel.class, likeFirstName);
        try {
            assertEquals(3, cursor.getCount());
            int index = 1;
            while (cursor.moveToNext()) {
                assertEquals("Sam " + index, cursor.get(TestModel.FIRST_NAME));
                index++;
            }
        } finally {
            cursor.close();
        }
    }

    public void testAggregateCount() {
        TestModel model1 = insertBasicTestModel();
        TestModel model2 = new TestModel().setFirstName(model1.getFirstName()).setLastName("Smith");
        dao.persist(model2);

        IntegerProperty groupCount = IntegerProperty.countProperty(TestModel.FIRST_NAME, false);
        Query query = Query.select(TestModel.PROPERTIES).selectMore(groupCount).groupBy(TestModel.FIRST_NAME);
        SquidCursor<TestModel> groupedCursor = dao.query(TestModel.class, query);
        try {
            groupedCursor.moveToFirst();
            assertEquals(1, groupedCursor.getCount());
            assertEquals(2, groupedCursor.get(groupCount).intValue());
        } finally {
            groupedCursor.close();
        }
    }

    public void testJoinOnSameTableUsingAlias() {
        // check precondition
        int rowsWithManager = dao.count(Employee.class, Employee.MANAGER_ID.gt(0));
        assertEquals(5, rowsWithManager);

        List<String> resultEmployees = new ArrayList<String>(5);
        List<String> resultManagers = new ArrayList<String>(5);
        resultEmployees.add(cookieMonster.getName());
        resultManagers.add(bigBird.getName());
        resultEmployees.add(elmo.getName());
        resultManagers.add(bigBird.getName());
        resultEmployees.add(oscar.getName());
        resultManagers.add(bigBird.getName());
        resultEmployees.add(bert.getName());
        resultManagers.add(cookieMonster.getName());
        resultEmployees.add(ernie.getName());
        resultManagers.add(bert.getName());

        // select employees.name as employeeName, managers.name as managerName from employees inner join employees as
        // managers on (employees.managerId = managers._id) order by managers._id ASC;
        Table managerTable = Employee.TABLE.as("managers");
        StringProperty employeeName = Employee.NAME.as("employeeName");
        StringProperty managerName = Employee.NAME.as(managerTable, "managerName");
        LongProperty managerId = managerTable.qualifyField(Employee.ID);
        Join join = Join.inner(managerTable, Employee.MANAGER_ID.eq(managerId));
        Query query = Query.select(employeeName, managerName).from(Employee.TABLE).join(join).orderBy(managerId.asc());

        SquidCursor<Employee> cursor = dao.query(Employee.class, query);
        try {
            assertEquals(rowsWithManager, cursor.getCount());
            int index = 0;
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext(), index++) {
                String eName = cursor.get(employeeName);
                String mName = cursor.get(managerName);
                assertEquals(resultEmployees.get(index), eName);
                assertEquals(resultManagers.get(index), mName);
            }
        } finally {
            cursor.close();
        }
    }

    public void testSelectAll() {
        insertBasicTestModel();
        TestModel model = dao.fetchByQuery(TestModel.class, Query.select());
        assertTrue(model.containsNonNullValue(TestModel.FIRST_NAME));
        assertTrue(model.containsNonNullValue(TestModel.LAST_NAME));
        assertTrue(model.containsNonNullValue(TestModel.IS_HAPPY));
        assertTrue(model.containsNonNullValue(TestModel.BIRTHDAY));
    }

    public void testWithNonLiteralCriterion() {
        TestModel model = new TestModel().setFirstName("Sam").setLastName("Sam");
        dao.persist(model);

        TestModel fetch = dao
                .fetchByQuery(TestModel.class, Query.select().where(TestModel.FIRST_NAME.eq(TestModel.LAST_NAME)));
        assertNotNull(fetch);
        assertEquals(fetch.getFirstName(), fetch.getLastName());
    }

    public void testLimitAndOffset() {
        // no limit, no offset
        testLimitAndOffsetInternal(Query.NO_LIMIT, Query.NO_OFFSET);

        // limit without offset
        testLimitAndOffsetInternal(-3, Query.NO_OFFSET);
        testLimitAndOffsetInternal(0, Query.NO_OFFSET);
        testLimitAndOffsetInternal(2, Query.NO_OFFSET);
        testLimitAndOffsetInternal(5, Query.NO_OFFSET);

        // offset without limit
        testLimitAndOffsetInternal(Query.NO_LIMIT, -2);
        testLimitAndOffsetInternal(Query.NO_LIMIT, 0);
        testLimitAndOffsetInternal(Query.NO_LIMIT, 3);
        testLimitAndOffsetInternal(Query.NO_LIMIT, 6);

        // limit and offset
        testLimitAndOffsetInternal(3, 2);
        testLimitAndOffsetInternal(5, 3);
    }

    private void testLimitAndOffsetInternal(int limit, int offset) {
        // We'll check against IDs, so choose an order that shuffles the IDs somewhat
        Query query = Query.select().orderBy(Employee.NAME.desc());
        SquidCursor<Employee> cursor = dao.query(Employee.class, query);
        int numRows = cursor.getCount();

        int expectedCount = numRows;
        if (offset > Query.NO_OFFSET) {
            expectedCount = Math.max(expectedCount - offset, 0);
        }
        if (limit > Query.NO_LIMIT) {
            expectedCount = Math.min(expectedCount, limit);
        }

        long[] expectedIds = new long[expectedCount];
        try {
            int index = 0;
            int start = offset > 0 ? offset : 0;
            for (cursor.moveToPosition(start); !cursor.isAfterLast(); cursor.moveToNext()) {
                if (index == expectedIds.length) {
                    break;
                }
                expectedIds[index++] = cursor.get(Employee.ID);
            }
        } finally {
            cursor.close();
        }

        cursor = dao.query(Employee.class, query.limit(limit, offset));
        assertEquals(expectedCount, cursor.getCount());
        try {
            int index = 0;
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                assertEquals(expectedIds[index++], cursor.get(Employee.ID).intValue());
            }
        } finally {
            cursor.close();
        }
    }

    public void testInCriterion() {
        Query query = Query.select().where(Employee.NAME.in("bigBird", "cookieMonster")).orderBy(Employee.NAME.asc());
        testInQuery(query);

        List<String> list = Arrays.asList("bigBird", "cookieMonster");
        query = Query.select().where(Employee.NAME.in(list)).orderBy(Employee.NAME.asc());
        testInQuery(query);

        // Test off-by-one error that used to occur when the in criterion wasn't the last criterion in the list
        query = Query.select().where(Employee.NAME.in(list).or(Field.field("1").neq(1))).orderBy(Employee.NAME.asc());
        testInQuery(query);
    }

    public void testIsEmptyCriterion() {
        TestModel model = new TestModel().setFirstName("").setLastName(null);
        dao.persist(model);

        TestModel fetched = dao.fetchByCriterion(TestModel.class,
                TestModel.FIRST_NAME.isEmpty().and(TestModel.LAST_NAME.isEmpty()), TestModel.ID);
        assertNotNull(fetched);
        assertEquals(model.getId(), fetched.getId());
    }

    public void testIsNotEmptyCriterion() {
        TestModel model = new TestModel().setFirstName("Sam").setLastName(null);
        dao.persist(model);

        TestModel fetched = dao.fetchByCriterion(TestModel.class,
                TestModel.FIRST_NAME.isNotEmpty().and(TestModel.LAST_NAME.isEmpty()), TestModel.ID);
        assertNotNull(fetched);
        assertEquals(model.getId(), fetched.getId());
    }

    private void testInQuery(Query query) {
        SquidCursor<Employee> cursor = dao.query(Employee.class, query);
        try {
            assertEquals(2, cursor.getCount());
            cursor.moveToFirst();
            assertEquals("bigBird", cursor.get(Employee.NAME));
            cursor.moveToNext();
            assertEquals("cookieMonster", cursor.get(Employee.NAME));
        } finally {
            cursor.close();
        }
    }

    public void testReusableQuery() {
        AtomicReference<String> name = new AtomicReference<String>();
        Query query = Query.select().where(Employee.NAME.eq(name));
        testReusableQueryInternal(name, "bigBird", query);
        testReusableQueryInternal(name, "cookieMonster", query);
        testReusableQueryInternal(name, "elmo", query);
    }

    private void testReusableQueryInternal(AtomicReference<String> ref, String name, Query query) {
        ref.set(name);
        SquidCursor<Employee> cursor = dao.query(Employee.class, query);
        try {
            cursor.moveToFirst();
            assertEquals(1, cursor.getCount());
            assertEquals(name, cursor.get(Employee.NAME));
        } finally {
            cursor.close();
        }
    }

    public void testAtomicIntegers() {
        AtomicInteger id = new AtomicInteger(1);
        Query query = Query.select(Employee.ID).where(Employee.ID.eq(id));

        SquidCursor<Employee> cursor = dao.query(Employee.class, query);
        try {
            assertEquals(1, cursor.getCount());
            cursor.moveToFirst();
            assertEquals(1, cursor.get(Employee.ID).longValue());
        } finally {
            cursor.close();
        }
        id.set(2);
        cursor = dao.query(Employee.class, query);
        try {
            assertEquals(1, cursor.getCount());
            cursor.moveToFirst();
            assertEquals(2, cursor.get(Employee.ID).longValue());
        } finally {
            cursor.close();
        }
    }

    public void testAtomicBoolean() {
        AtomicBoolean isHappy = new AtomicBoolean(false);

        Query query = Query.select().where(Employee.IS_HAPPY.eq(isHappy));
        SquidCursor<Employee> unhappyEmployee = dao.query(Employee.class, query);
        try {
            assertEquals(1, unhappyEmployee.getCount());
            unhappyEmployee.moveToFirst();
            assertEquals(oscar.getId(), unhappyEmployee.get(Employee.ID).longValue());
        } finally {
            unhappyEmployee.close();
        }

        isHappy.set(true);
        SquidCursor<Employee> happyEmployees = dao.query(Employee.class, query);
        try {
            assertEquals(5, happyEmployees.getCount());
        } finally {
            happyEmployees.close();
        }
    }

    public void testSimpleSubquerySelect() {
        Query query = Query.fromSubquery(Query.select(Employee.NAME).from(Employee.TABLE), "subquery");
        StringProperty name = query.getTable().qualifyField(Employee.NAME);
        query.where(name.eq("bigBird"));
        SquidCursor<Employee> cursor = dao.query(Employee.class, query);
        try {
            assertEquals(1, cursor.getCount());
            cursor.moveToFirst();
            Employee employee = new Employee(cursor);
            assertEquals("bigBird", employee.getName());
        } finally {
            cursor.close();
        }
    }

    public void testReusableQueryWithInCriterion() {
        Set<String> collection = new HashSet<String>();
        Query query = Query.select().where(Employee.NAME.in(collection));
        testReusableQueryWithInCriterionInternal(collection, query, "bigBird", "cookieMonster", "elmo");
        testReusableQueryWithInCriterionInternal(collection, query, "bigBird", "cookieMonster");
        testReusableQueryWithInCriterionInternal(collection, query, "oscar");
        testReusableQueryWithInCriterionInternal(collection, query);
    }

    private void testReusableQueryWithInCriterionInternal(Collection<String> collection, Query query, String... list) {
        collection.clear();
        collection.addAll(Arrays.asList(list));
        SquidCursor<Employee> cursor = dao.query(Employee.class, query);
        try {
            assertEquals(collection.size(), cursor.getCount());
            while (cursor.moveToNext()) {
                String name = cursor.get(Employee.NAME);
                assertTrue(collection.contains(name));
            }
        } finally {
            cursor.close();
        }
    }

    public void testQueryWithMaxSqlArgs() {
        int numRows = SqlStatement.MAX_VARIABLE_NUMBER + 1;
        Set<Long> rowIds = new HashSet<Long>();

        dao.beginTransaction();
        try {
            for (int i = 0; i < numRows; i++) {
                TestModel testModel = new TestModel();
                dao.persist(testModel);
                rowIds.add(testModel.getId());
            }
            dao.setTransactionSuccessful();
        } finally {
            dao.endTransaction();
        }
        assertTrue(rowIds.size() > SqlStatement.MAX_VARIABLE_NUMBER);
        assertTrue(dao.count(TestModel.class, Criterion.all) > SqlStatement.MAX_VARIABLE_NUMBER);

        Query query = Query.select(TestModel.ID).where(TestModel.ID.in(rowIds));
        testMaxSqlArgRowIds(query, rowIds.size());

        rowIds.clear();
        rowIds.addAll(Arrays.asList(1L, 2L, 3L, 4L));

        testMaxSqlArgRowIds(query, rowIds.size());
    }

    private void testMaxSqlArgRowIds(Query query, int expectedSize) {
        SquidCursor<TestModel> cursor = dao.query(TestModel.class, query);
        try {
            assertEquals(expectedSize, cursor.getCount());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public void testSubqueryJoin() {
        StringProperty managerName = Employee.NAME.as("managerName");
        Query query = Query
                .fromSubquery(Query.select(Employee.MANAGER_ID).from(Employee.TABLE).groupBy(Employee.MANAGER_ID),
                        "subquery");
        query.selectMore(managerName);
        query.join(Join.inner(Employee.TABLE, query.getTable().qualifyField(Employee.MANAGER_ID).eq(Employee.ID)))
                .orderBy(Employee.MANAGER_ID.asc());

        SquidCursor<Employee> cursor = dao.query(Employee.class, query);
        try {
            assertEquals(3, cursor.getCount());
            cursor.moveToFirst();
            assertEquals("bigBird", cursor.get(managerName));
            cursor.moveToNext();
            assertEquals("cookieMonster", cursor.get(managerName));
            cursor.moveToNext();
            assertEquals("bert", cursor.get(managerName));
        } finally {
            cursor.close();
        }
    }

    public void x_testReusableQueryPerformance() {
        String[] values = {"bigBird", "cookieMonster", "elmo", "oscar"};
        int numIterations = 10000;
        long start = System.currentTimeMillis();
        for (int i = 0; i < numIterations; i++) {
            Query query = Query.select().where(Employee.NAME.eq(values[i % values.length]));
            dao.query(Employee.class, query);
        }
        long end = System.currentTimeMillis();
        System.err.println("Unoptimized took " + (end - start) + " millis");

        AtomicReference<String> reference = new AtomicReference<String>();
        Query query = Query.select().where(Employee.NAME.eq(reference));
        start = System.currentTimeMillis();
        for (int i = 0; i < numIterations; i++) {
            reference.set(values[i % values.length]);
            dao.query(Employee.class, query);
        }
        end = System.currentTimeMillis();
        System.err.println("Optimized took " + (end - start) + " millis");
    }

    public void x_testReusableListQueryPerformance() {
        List<?>[] testSets = {
                Arrays.asList(new String[]{"bigBird", "cookieMonster", "elmo"}),
                Arrays.asList(new String[]{"bigBird", "cookieMonster"}),
                Arrays.asList(new String[]{"bert", "ernie"}),
                Arrays.asList(new String[]{"oscar"}),
                Arrays.asList(new String[]{})
        };

        int numIterations = 10000;
        long start = System.currentTimeMillis();
        for (int i = 0; i < numIterations; i++) {
            Query query = Query.select().where(Employee.NAME.in(testSets[i % testSets.length]));
            dao.query(Employee.class, query);
        }
        long end = System.currentTimeMillis();
        System.err.println("Unoptimized took " + (end - start) + " millis");
        System.gc();
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        AtomicReference<Collection<?>> ref = new AtomicReference<Collection<?>>();
        Query query = Query.select().where(Employee.NAME.in(ref));
        start = System.currentTimeMillis();
        for (int i = 0; i < numIterations; i++) {
            ref.set(testSets[i % testSets.length]);
            dao.query(Employee.class, query);
        }
        end = System.currentTimeMillis();
        System.err.println("Optimized took " + (end - start) + " millis");
    }

    public void testSelectFromView() {
        View view = View.temporaryFromQuery(Query.select(Employee.PROPERTIES)
                .from(Employee.TABLE).where(Employee.MANAGER_ID.eq(bigBird.getId())), "bigBirdsEmployees");

        database.tryCreateView(view);

        Query fromView = Query.fromView(view).orderBy(view.qualifyField(Employee.ID).asc());

        SquidCursor<Employee> cursor = dao.query(Employee.class, fromView);
        try {
            assertEquals(3, cursor.getCount());
            cursor.moveToFirst();
            assertEquals("cookieMonster", cursor.get(Employee.NAME));
            cursor.moveToNext();
            assertEquals("elmo", cursor.get(Employee.NAME));
            cursor.moveToNext();
            assertEquals("oscar", cursor.get(Employee.NAME));
        } finally {
            cursor.close();
        }
    }

    public void testCriterionWithNestedSelect() {
        TestModel modelOne = new TestModel().setFirstName("Sam").setLastName("Bosley");
        TestModel modelTwo = new TestModel().setFirstName("Kevin").setLastName("Lim");
        TestModel modelThree = new TestModel().setFirstName("Jonathan").setLastName("Koren");

        dao.persist(modelOne);
        dao.persist(modelTwo);
        dao.persist(modelThree);
        assertEquals(3, dao.count(TestModel.class, Criterion.all));

        dao.deleteWhere(TestModel.class,
                TestModel.ID.lt(Query.select(Function.max(TestModel.ID)).from(TestModel.TABLE)));
        SquidCursor<TestModel> cursor = null;
        try {
            cursor = dao.query(TestModel.class, Query.select());
            assertEquals(1, cursor.getCount());
            cursor.moveToFirst();
            assertEquals(3, cursor.get(TestModel.ID).longValue());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public void testJoinOnLiteralValue() {
        TestModel modelOne = new TestModel().setFirstName("Sam").setLastName("Bosley");
        TestModel modelTwo = new TestModel().setFirstName("Kevin").setLastName("Lim");
        TestModel modelThree = new TestModel().setFirstName("Jonathan").setLastName("Koren");

        Thing thingOne = new Thing().setFoo("Thing1").setBar(5);
        Thing thingTwo = new Thing().setFoo("Thing2").setBar(-1);
        Thing thingThree = new Thing().setFoo("Thing3").setBar(100);

        dao.persist(modelOne);
        dao.persist(modelTwo);
        dao.persist(modelThree);
        dao.persist(thingOne);
        dao.persist(thingTwo);
        dao.persist(thingThree);

        Query query = Query.select(TestModel.FIRST_NAME, TestModel.LAST_NAME).selectMore(Thing.FOO, Thing.BAR)
                .from(TestModel.TABLE)
                .join(Join.inner(Thing.TABLE, Thing.BAR.gt(0)));
        SquidCursor<TestModel> cursor = dao.query(TestModel.class, query);
        try {
            assertEquals(6, cursor.getCount());
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                assertTrue(cursor.get(Thing.BAR).intValue() > 0);
            }
        } finally {
            cursor.close();
        }
    }

    // When arguments are bound as strings, the query below will always return an empty
    // set because abs(1) != '1' in sqlite, despite all their type funkiness.
    // When this test passes, it means that the arguments are being bound with their
    // correct types, and not as strings
    public void testQueryBindingTypes() {
        insertBasicTestModel();
        Field<Integer> one = Field.field("1");
        SquidCursor<TestModel> cursor = dao.query(TestModel.class, Query.select().where(Function.abs(one).eq(1)));
        try {
            assertEquals(1, cursor.getCount());
        } finally {
            cursor.close();
        }
    }

    // We used to think there was a bug with binding arguments in having clauses.
    // Now that we're binding the correct argument types, there isn't a bug,
    // and this test demonstrates that.
    public void testBoundArgumentsWorkInHavingClause() {
        Query query = Query.select(Employee.PROPERTIES)
                .groupBy(Employee.MANAGER_ID)
                .having(Function.count(Employee.MANAGER_ID).gt(2));
        SquidCursor<Employee> cursor = dao.query(Employee.class, query);
        try {
            assertEquals(1, cursor.getCount());
            cursor.moveToFirst();
            assertEquals(bigBird.getId(), cursor.get(Employee.MANAGER_ID).longValue());
        } finally {
            cursor.close();
        }
    }

    public void testJoinWithUsingClause() {
        final String separator = "|";
        final Map<Long, String> expectedResults = new HashMap<Long, String>();
        expectedResults.put(bigBird.getId(), "1");
        expectedResults.put(cookieMonster.getId(), "2|3|4");
        expectedResults.put(elmo.getId(), "2|3|4");
        expectedResults.put(oscar.getId(), "2|3|4");
        expectedResults.put(bert.getId(), "5");
        expectedResults.put(ernie.getId(), "6");

        /*
         * select employees._id, employees.name, employees.managerId, subTable.subordinates as coworkers from employees
         * join (select e.managerId, group_concat(e._id, "|") as subordinates from employees as e group by e.managerId)
         * as subTable using (managerId);
         */

        Table employeesAlias = Employee.TABLE.as("e");
        LongProperty aliasedId = employeesAlias.qualifyField(Employee.ID);
        LongProperty aliasedManagerId = employeesAlias.qualifyField(Employee.MANAGER_ID);
        StringProperty subordinates = StringProperty.fromFunction(Function.groupConcat(aliasedId, separator),
                "subordinates");
        Query subquery = Query.select(aliasedManagerId, subordinates).from(employeesAlias).groupBy(aliasedManagerId);

        SqlTable<?> subTable = subquery.as("subTable");
        StringProperty coworkers = subTable.qualifyField(subordinates);
        Query query = Query.select(Employee.PROPERTIES).selectMore(coworkers)
                .from(Employee.TABLE)
                .join(Join.inner(subTable, Employee.MANAGER_ID));

        SquidCursor<Employee> cursor = dao.query(Employee.class, query);

        try {
            assertEquals(6, cursor.getCount());
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                long id = cursor.get(Employee.ID);
                String coworkersList = cursor.get(coworkers);
                assertEquals(expectedResults.get(id), coworkersList);
            }
        } finally {
            cursor.close();
        }
    }

    public void testSelectLiteral() {
        StringProperty literal = StringProperty.literal("literal", "name");
        LongProperty literalLong = LongProperty.literal(12, "twelve");
        SquidCursor<?> c = dao.query(null, Query.select(literal, literalLong));
        try {
            assertEquals(1, c.getCount());
            c.moveToFirst();
            assertEquals("literal", c.get(literal));
            assertEquals(12, c.get(literalLong).longValue());
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }

    public void testBindArgsProtectsInjection() {
        Query q = Query.select().where(Employee.NAME.eq("'Sam'); drop table " + Employee.TABLE.getName() + ";"));
        SquidCursor<Employee> cursor = dao.query(Employee.class, q);
        try {
            assertFalse(dao.count(Employee.class, Criterion.all) == 0);
        } finally {
            cursor.close();
        }
    }

    public void testFork() {
        Query base = Query.select().from(Employee.TABLE).limit(1);
        Query fork = base.fork().limit(2);
        base.limit(3);

        assertFalse(base == fork);
        assertEquals(3, base.getLimit());
        assertEquals(2, fork.getLimit());
        assertEquals(base.getTable(), fork.getTable());
    }

    public void testQueryFreeze() {
        Query base = Query.select().from(Employee.TABLE).limit(1).freeze();
        Query fork = base.limit(2);

        assertFalse(base == fork);
        assertEquals(1, base.getLimit());
        assertEquals(2, fork.getLimit());
        assertEquals(base.getTable(), fork.getTable());
    }

    public void testFrozenQueryWorksWithDao() {
        Query query = Query.select().limit(2).freeze();
        SquidCursor<Employee> cursor = dao.query(Employee.class, query);
        try {
            assertEquals(2, cursor.getCount());
            assertNull(query.getTable());
        } finally {
            cursor.close();
        }

        Employee employee = dao.fetchByQuery(Employee.class, query);
        assertNotNull(employee);
        assertNull(query.getTable());
        assertEquals(2, query.getLimit());
    }

    // the following four tests all use the same query but different compound operators

    public void testUnion() {
        Query query = Query.select().from(Employee.TABLE).where(Employee.MANAGER_ID.eq(1))
                .union(Query.select().from(Employee.TABLE).where(Employee.ID.eq(2)))
                .orderBy(Employee.ID.asc());
        SquidCursor<Employee> cursor = dao.query(Employee.class, query);
        try {
            assertEquals(3, cursor.getCount());
            cursor.moveToFirst();
            assertEquals(cookieMonster, new Employee(cursor));
            cursor.moveToNext();
            assertEquals(elmo, new Employee(cursor));
            cursor.moveToNext();
            assertEquals(oscar, new Employee(cursor));
        } finally {
            cursor.close();
        }
    }

    public void testUnionAll() {
        Query query = Query.select().from(Employee.TABLE).where(Employee.MANAGER_ID.eq(1))
                .unionAll(Query.select().from(Employee.TABLE).where(Employee.ID.eq(2)))
                .orderBy(Employee.ID.asc());
        SquidCursor<Employee> cursor = dao.query(Employee.class, query);
        try {
            assertEquals(4, cursor.getCount());
            cursor.moveToFirst();
            assertEquals(cookieMonster, new Employee(cursor));
            cursor.moveToNext();
            assertEquals(cookieMonster, new Employee(cursor));
            cursor.moveToNext();
            assertEquals(elmo, new Employee(cursor));
            cursor.moveToNext();
            assertEquals(oscar, new Employee(cursor));
        } finally {
            cursor.close();
        }
    }

    public void testExcept() {
        Query query = Query.select().from(Employee.TABLE).where(Employee.MANAGER_ID.eq(1))
                .except(Query.select().from(Employee.TABLE).where(Employee.ID.eq(2)))
                .orderBy(Employee.ID.asc());
        SquidCursor<Employee> cursor = dao.query(Employee.class, query);
        try {
            assertEquals(2, cursor.getCount());
            cursor.moveToFirst();
            assertEquals(elmo, new Employee(cursor));
            cursor.moveToNext();
            assertEquals(oscar, new Employee(cursor));
        } finally {
            cursor.close();
        }
    }

    public void testIntersect() {
        Query query = Query.select().from(Employee.TABLE).where(Employee.MANAGER_ID.eq(1))
                .intersect(Query.select().from(Employee.TABLE).where(Employee.ID.eq(2)))
                .orderBy(Employee.ID.asc());
        SquidCursor<Employee> cursor = dao.query(Employee.class, query);
        try {
            assertEquals(1, cursor.getCount());
            cursor.moveToFirst();
            assertEquals(cookieMonster, new Employee(cursor));
        } finally {
            cursor.close();
        }
    }

    public void testSelectDistinct() {
        Query query = Query.selectDistinct(Employee.MANAGER_ID).orderBy(Employee.MANAGER_ID.asc());
        SquidCursor<Employee> cursor = dao.query(Employee.class, query);
        try {
            assertEquals(4, cursor.getCount());
            cursor.moveToFirst();
            assertEquals(Long.valueOf(0), cursor.get(Employee.MANAGER_ID));
            cursor.moveToNext();
            assertEquals(Long.valueOf(1), cursor.get(Employee.MANAGER_ID));
            cursor.moveToNext();
            assertEquals(Long.valueOf(2), cursor.get(Employee.MANAGER_ID));
            cursor.moveToNext();
            assertEquals(Long.valueOf(5), cursor.get(Employee.MANAGER_ID));
        } finally {
            cursor.close();
        }
    }

    public void testAllFields() {
        Query query = Query.select().from(TestViewModel.VIEW)
                .leftJoin(Thing.TABLE, TestViewModel.TEST_MODEL_ID.eq(Thing.ID));
        List<Field<?>> fields = query.getFields();
        for (Property<?> p : TestViewModel.PROPERTIES) {
            assertTrue(fields.contains(p));
        }

        for (Property<?> p : Thing.PROPERTIES) {
            assertTrue(fields.contains(p));
        }

        assertEquals(TestViewModel.PROPERTIES.length + Thing.PROPERTIES.length, fields.size());
    }

    public void testReadAllFieldsIntoModel() {
        TestModel testModel = new TestModel().setFirstName("Sam");
        dao.persist(testModel);

        Thing thing = new Thing().setFoo("Thingy").setBar(5);
        dao.persist(thing);

        Query query = Query.select().from(TestViewModel.VIEW)
                .leftJoin(Thing.TABLE, TestViewModel.TEST_MODEL_ID.eq(Thing.ID));

        TestViewModel model = dao.fetchByQuery(TestViewModel.class, query);
        for (Property<?> p : TestViewModel.PROPERTIES) {
            assertTrue(model.containsValue(p));
        }

        for (Property<?> p : Thing.PROPERTIES) {
            assertTrue(model.containsValue(p));
        }
    }

    public void testParenthesizeWherePropagates() {
        Query subquery = Query.select(Thing.FOO).from(Thing.TABLE).where(Thing.BAR.gt(0));
        Query joinSubquery = Query.select(Thing.BAR).from(Thing.TABLE).where(Thing.FOO.isNotEmpty());
        Query compoundSubquery = Query.select(Thing.BAZ).from(Thing.TABLE).where(Thing.IS_ALIVE.isTrue());

        SubqueryTable subqueryTable = subquery.as("t1");
        SubqueryTable joinTable = joinSubquery.as("t2");
        Query query = Query.select().from(subqueryTable).innerJoin(joinTable, Criterion.all)
                .union(compoundSubquery);

        final int subqueryLength = subquery.toRawSql().length();
        final int joinSubqueryLength = joinSubquery.toRawSql().length();
        final int compoundSubqueryLength = compoundSubquery.toRawSql().length();
        final int queryLength = query.toRawSql().length();

        query.parenthesizeWhere(true);
        assertEquals(subqueryLength + 2, subquery.toRawSql().length());
        assertEquals(joinSubqueryLength + 2, joinSubquery.toRawSql().length());
        assertEquals(compoundSubqueryLength + 2, compoundSubquery.toRawSql().length());
        assertEquals(queryLength + 6, query.toRawSql().length());
    }

    public void testNeedsValidationUpdatedByMutation() {
        Query subquery = Query.select(Thing.PROPERTIES).from(Thing.TABLE);
        subquery.requestValidation();

        Query baseTestQuery = Query.select().from(Thing.TABLE).where(Thing.FOO.isNotEmpty()).freeze();
        assertFalse(baseTestQuery.needsValidation());

        Query testQuery = baseTestQuery.from(subquery.as("t1"));
        assertTrue(testQuery.needsValidation());
        testQuery = baseTestQuery.innerJoin(subquery.as("t2"), Criterion.all);
        assertTrue(testQuery.needsValidation());
        testQuery = baseTestQuery.union(subquery);
        assertTrue(testQuery.needsValidation());
    }

    public void testLiteralCriterions() {
        // null and not null evaluate to false
        assertEquals(0, dao.count(Employee.class, Criterion.literal(null)));
        assertEquals(0, dao.count(Employee.class, Criterion.literal(null).negate()));

        // numeric literal; values other than 0 (including negative) evaluate to true
        assertEquals(0, dao.count(Employee.class, Criterion.literal(0)));
        assertEquals(6, dao.count(Employee.class, Criterion.literal(10)));
        assertEquals(6, dao.count(Employee.class, Criterion.literal(-10)));
        assertEquals(6, dao.count(Employee.class, Criterion.literal(0).negate()));
        assertEquals(0, dao.count(Employee.class, Criterion.literal(10).negate()));

        // text literal; SQLite will try to coerce to a number
        assertEquals(0, dao.count(Employee.class, Criterion.literal("sqlite")));
        assertEquals(6, dao.count(Employee.class, Criterion.literal("sqlite").negate()));
        assertEquals(6, dao.count(Employee.class, Criterion.literal("1sqlite"))); // coerces to 1
        assertEquals(0, dao.count(Employee.class, Criterion.literal("1sqlite").negate()));

        // numeric column
        Criterion isHappyCriterion = Employee.IS_HAPPY.asCriterion();
        assertEquals(5, dao.count(Employee.class, isHappyCriterion));
        assertEquals(1, dao.count(Employee.class, isHappyCriterion.negate()));

        // text column
        Criterion nameCriterion = Employee.NAME.asCriterion();
        assertEquals(0, dao.count(Employee.class, nameCriterion));
        assertEquals(6, dao.count(Employee.class, nameCriterion.negate()));

        // function
        Criterion f = Function.functionWithArguments("length", "sqlite").asCriterion();
        assertEquals(6, dao.count(Employee.class, f));
        assertEquals(0, dao.count(Employee.class, f.negate()));
    }
}
