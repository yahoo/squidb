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
import com.yahoo.squidb.test.TestEnum;
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
import java.util.concurrent.Semaphore;
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
        database.persist(bigBird);

        cookieMonster = new Employee();
        cookieMonster.setName("cookieMonster").setManagerId(bigBird.getRowId());
        database.persist(cookieMonster);

        elmo = new Employee();
        elmo.setName("elmo").setManagerId(bigBird.getRowId());
        database.persist(elmo);

        oscar = new Employee();
        oscar.setName("oscar").setManagerId(bigBird.getRowId()).setIsHappy(false);
        database.persist(oscar);

        bert = new Employee();
        bert.setName("bert").setManagerId(cookieMonster.getRowId());
        database.persist(bert);

        ernie = new Employee();
        ernie.setName("ernie").setManagerId(bert.getRowId());
        database.persist(ernie);
    }

    public void testSelectionArgsGeneration() {
        Query query = Query.select(TestModel.PROPERTIES)
                .where(TestModel.FIRST_NAME.eq("Sam")
                        .and(TestModel.BIRTHDAY.gt(17))
                        .and(TestModel.LAST_NAME.neq("Smith")));

        CompiledStatement compiledQuery = query.compile(database.getCompileContext());
        verifyCompiledSqlArgs(compiledQuery, 3, "Sam", 17, "Smith");
    }

    public void testOrderByField() {
        TestModel one = new TestModel().setFirstName("Sam").setLastName("Bosley");
        TestModel two = new TestModel().setFirstName("Kevin").setLastName("Lim");
        TestModel three = new TestModel().setFirstName("Jonathan").setLastName("Koren");

        database.persist(one);
        database.persist(two);
        database.persist(three);

        String[] nameOrder = new String[]{"Kevin", "Sam", "Jonathan"};
        SquidCursor<TestModel> nameOrderCursor = database.query(TestModel.class, Query.select(TestModel.PROPERTIES)
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
        SquidCursor<TestModel> idOrderCursor = database.query(TestModel.class, Query.select(TestModel.PROPERTIES)
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

    public void testReverseOrder() {
        long max = database.countAll(Employee.class);
        SquidCursor<Employee> cursor = database.query(Employee.class,
                Query.select(Employee.ID).orderBy(Employee.ID.asc().reverse()));
        try {
            assertEquals(max, cursor.getCount());
            assertTrue(max > 0);
            while (cursor.moveToNext()) {
                long nextId = cursor.get(Employee.ID);
                if (nextId > max) {
                    fail("IDs not in reverse order");
                }
                max = nextId;
            }
        } finally {
            cursor.close();
        }
    }

    public void testOrderByArray() {
        Long[] order = new Long[]{5L, 1L, 4L};
        SquidCursor<Employee> cursor = database.query(Employee.class,
                Query.select(Employee.ID).limit(order.length).orderBy(Employee.ID.byArray(order)));
        try {
            assertEquals(order.length, cursor.getCount());
            for (int i = 0; i < order.length; i++) {
                cursor.moveToPosition(i);
                assertEquals(order[i], cursor.get(Employee.ID));
            }
        } finally {
            cursor.close();
        }
    }

    public void testLikeWithNoEscape() {
        insertBasicTestModel();
        assertEquals(1, database.count(TestModel.class, TestModel.LAST_NAME.like("Bo_le%")));
        assertEquals(0, database.count(TestModel.class, TestModel.LAST_NAME.notLike("Bo_le%")));
        assertEquals(0, database.count(TestModel.class, TestModel.LAST_NAME.like("%leyx")));
        assertEquals(1, database.count(TestModel.class, TestModel.LAST_NAME.notLike("%leyx")));
    }

    public void testLikeWithEscape() {
        TestModel model = insertBasicTestModel();
        model.setFirstName("S_a%m");
        database.persist(model);

        assertEquals(1, database.count(TestModel.class, TestModel.FIRST_NAME.like("%\\_a\\%%", '\\')));
        assertEquals(0, database.count(TestModel.class, TestModel.FIRST_NAME.notLike("%\\_a\\%%", '\\')));
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

        SquidCursor<TestModel> cursor = database.query(TestModel.class, likeFirstName);
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

    public void testBetweenCriterion() {
        testBetween(Arrays.asList(2L, 3L, 4L, 5L), false);
        testBetween(Arrays.asList(1L, 6L), true);
    }

    private void testBetween(List<Long> expectedIds, boolean useNotBetween) {
        SquidCursor<Employee> cursor = database.query(Employee.class,
                Query.select(Employee.ID).where(useNotBetween ? Employee.ID.notBetween(2, 5) :
                        Employee.ID.between(2, 5)).orderBy(Employee.ID.asc()));
        try {
            assertEquals(expectedIds.size(), cursor.getCount());
            for (Long id : expectedIds) {
                cursor.moveToNext();
                assertEquals(id.longValue(), cursor.get(Employee.ID).longValue());
            }
        } finally {
            cursor.close();
        }
    }

    public void testGlobCriterion() {
        testGlob(Arrays.asList(bigBird, bert), false);
        testGlob(Arrays.asList(cookieMonster, elmo, oscar, ernie), true);
    }

    private void testGlob(List<Employee> expected, boolean useNotGlob) {
        SquidCursor<Employee> cursor = database.query(Employee.class,
                Query.select(Employee.ID, Employee.NAME).where(useNotGlob ? Employee.NAME.notGlob("b*") :
                        Employee.NAME.glob("b*"))
                        .orderBy(Employee.ID.asc()));
        try {
            assertEquals(expected.size(), cursor.getCount());
            for (Employee e : expected) {
                cursor.moveToNext();
                assertEquals(e.getRowId(), cursor.get(Employee.ID).longValue());
                assertEquals(e.getName(), cursor.get(Employee.NAME));
            }
        } finally {
            cursor.close();
        }
    }

    public void testAggregateCount() {
        TestModel model1 = insertBasicTestModel();
        TestModel model2 = new TestModel().setFirstName(model1.getFirstName()).setLastName("Smith");
        database.persist(model2);

        IntegerProperty groupCount = IntegerProperty.countProperty(TestModel.FIRST_NAME, false);
        Query query = Query.select(TestModel.PROPERTIES).selectMore(groupCount).groupBy(TestModel.FIRST_NAME);
        SquidCursor<TestModel> groupedCursor = database.query(TestModel.class, query);
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
        int rowsWithManager = database.count(Employee.class, Employee.MANAGER_ID.gt(0));
        assertEquals(5, rowsWithManager);

        List<String> resultEmployees = new ArrayList<>(5);
        List<String> resultManagers = new ArrayList<>(5);
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

        SquidCursor<Employee> cursor = database.query(Employee.class, query);
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
        TestModel model = database.fetchByQuery(TestModel.class, Query.select());
        assertTrue(model.containsNonNullValue(TestModel.FIRST_NAME));
        assertTrue(model.containsNonNullValue(TestModel.LAST_NAME));
        assertTrue(model.containsNonNullValue(TestModel.IS_HAPPY));
        assertTrue(model.containsNonNullValue(TestModel.BIRTHDAY));
    }

    public void testWithNonLiteralCriterion() {
        TestModel model = new TestModel().setFirstName("Sam").setLastName("Sam");
        database.persist(model);

        TestModel fetch = database
                .fetchByQuery(TestModel.class, Query.select().where(TestModel.FIRST_NAME.eq(TestModel.LAST_NAME)));
        assertNotNull(fetch);
        assertEquals(fetch.getFirstName(), fetch.getLastName());
    }

    private static final int NO_LIMIT = -1;
    private static final int NO_OFFSET = 0;

    public void testLimitAndOffset() {
        // no limit, no offset
        testLimitAndOffsetInternal(NO_LIMIT, NO_OFFSET);

        // limit without offset
        testLimitAndOffsetInternal(-3, NO_OFFSET);
        testLimitAndOffsetInternal(0, NO_OFFSET);
        testLimitAndOffsetInternal(2, NO_OFFSET);
        testLimitAndOffsetInternal(5, NO_OFFSET);

        // offset without limit
        testLimitAndOffsetInternal(NO_LIMIT, -2);
        testLimitAndOffsetInternal(NO_LIMIT, 0);
        testLimitAndOffsetInternal(NO_LIMIT, 3);
        testLimitAndOffsetInternal(NO_LIMIT, 6);

        // limit and offset
        testLimitAndOffsetInternal(3, 2);
        testLimitAndOffsetInternal(5, 3);
    }

    private void testLimitAndOffsetInternal(int limit, int offset) {
        // We'll check against IDs, so choose an order that shuffles the IDs somewhat
        Query query = Query.select().orderBy(Employee.NAME.desc());
        SquidCursor<Employee> cursor = database.query(Employee.class, query);

        int expectedCount = cursor.getCount();
        if (offset > NO_OFFSET) {
            expectedCount = Math.max(expectedCount - offset, 0);
        }
        if (limit > NO_LIMIT) {
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

        cursor = database.query(Employee.class, query.limit(limit, offset));
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

    public void testLimitAndOffsetWithExpressions() {
        // limit = 1 + (count(*) / 4), offset = count(*) / 2
        Field<Integer> limit = Function.add(1, Function.divide(
                Query.select(IntegerProperty.countProperty()).from(Employee.TABLE).asFunction(), 4));
        Field<Integer> offset = Function.divide(
                Query.select(IntegerProperty.countProperty()).from(Employee.TABLE).asFunction(), 2);

        Query query = Query.select().orderBy(Employee.NAME.asc()).limit(limit, offset);
        SquidCursor<Employee> cursor = database.query(Employee.class, query);
        try {
            assertEquals(2, cursor.getCount());
            cursor.moveToFirst();
            assertEquals(elmo, new Employee(cursor));
            cursor.moveToNext();
            assertEquals(ernie, new Employee(cursor));
        } finally {
            cursor.close();
        }
    }

    public void testInCriterion() {
        List<String> expectedNames = Arrays.asList("bigBird", "cookieMonster");
        Query query = Query.select().where(Employee.NAME.in("bigBird", "cookieMonster")).orderBy(Employee.NAME.asc());
        testInQuery(expectedNames, query);

        query = Query.select().where(Employee.NAME.notIn("bigBird", "cookieMonster")).orderBy(Employee.NAME.asc());
        testInQuery(Arrays.asList("bert", "elmo", "ernie", "oscar"), query);

        List<String> list = Arrays.asList("bigBird", "cookieMonster");
        query = Query.select().where(Employee.NAME.in(list)).orderBy(Employee.NAME.asc());
        testInQuery(expectedNames, query);

        // Test off-by-one error that used to occur when the in criterion wasn't the last criterion in the list
        query = Query.select().where(Employee.NAME.in(list).or(Field.field("1").neq(1))).orderBy(Employee.NAME.asc());
        testInQuery(expectedNames, query);
    }

    private void testInQuery(List<String> expectedNames, Query query) {
        SquidCursor<Employee> cursor = database.query(Employee.class, query);
        try {
            assertEquals(expectedNames.size(), cursor.getCount());
            for (String name : expectedNames) {
                cursor.moveToNext();
                assertEquals(name, cursor.get(Employee.NAME));
            }
        } finally {
            cursor.close();
        }
    }

    public void testIsEmptyCriterion() {
        TestModel model = new TestModel().setFirstName("").setLastName(null);
        database.persist(model);

        TestModel fetched = database.fetchByCriterion(TestModel.class,
                TestModel.FIRST_NAME.isEmpty().and(TestModel.LAST_NAME.isEmpty()), TestModel.ID);
        assertNotNull(fetched);
        assertEquals(model.getRowId(), fetched.getRowId());
    }

    public void testIsNotEmptyCriterion() {
        TestModel model = new TestModel().setFirstName("Sam").setLastName(null);
        database.persist(model);

        TestModel fetched = database.fetchByCriterion(TestModel.class,
                TestModel.FIRST_NAME.isNotEmpty().and(TestModel.LAST_NAME.isEmpty()), TestModel.ID);
        assertNotNull(fetched);
        assertEquals(model.getRowId(), fetched.getRowId());
    }

    public void testReusableQuery() {
        AtomicReference<String> name = new AtomicReference<>();
        Query query = Query.select().where(Employee.NAME.eq(name));
        testReusableQueryInternal(name, "bigBird", query);
        testReusableQueryInternal(name, "cookieMonster", query);
        testReusableQueryInternal(name, "elmo", query);
    }

    private void testReusableQueryInternal(AtomicReference<String> ref, String name, Query query) {
        ref.set(name);
        SquidCursor<Employee> cursor = database.query(Employee.class, query);
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

        SquidCursor<Employee> cursor = database.query(Employee.class, query);
        try {
            assertEquals(1, cursor.getCount());
            cursor.moveToFirst();
            assertEquals(1, cursor.get(Employee.ID).longValue());
        } finally {
            cursor.close();
        }
        id.set(2);
        cursor = database.query(Employee.class, query);
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
        SquidCursor<Employee> unhappyEmployee = database.query(Employee.class, query);
        try {
            assertEquals(1, unhappyEmployee.getCount());
            unhappyEmployee.moveToFirst();
            assertEquals(oscar.getRowId(), unhappyEmployee.get(Employee.ID).longValue());
        } finally {
            unhappyEmployee.close();
        }

        isHappy.set(true);
        SquidCursor<Employee> happyEmployees = database.query(Employee.class, query);
        try {
            assertEquals(5, happyEmployees.getCount());
        } finally {
            happyEmployees.close();
        }
    }

    public void testEnumResolvedUsingName() {
        Query query = Query.select(TestModel.SOME_ENUM).from(TestModel.TABLE)
                .where(TestModel.SOME_ENUM.eq(TestEnum.APPLE));
        CompiledStatement compiledStatement = query.compile(database.getCompileContext());
        verifyCompiledSqlArgs(compiledStatement, 1, "APPLE");
    }

    public void testDatabaseProvidedArgumentResolver() {
        database.useCustomArgumentBinder = true;
        Query query = Query.select(TestModel.SOME_ENUM).from(TestModel.TABLE)
                .where(TestModel.SOME_ENUM.eq(TestEnum.APPLE));

        CompiledStatement compiledStatement = query.compile(database.getCompileContext());
        verifyCompiledSqlArgs(compiledStatement, 1, 0);
    }

    public void testSimpleSubquerySelect() {
        Query query = Query.fromSubquery(Query.select(Employee.NAME).from(Employee.TABLE), "subquery");
        StringProperty name = query.getTable().qualifyField(Employee.NAME);
        query.where(name.eq("bigBird"));
        SquidCursor<Employee> cursor = database.query(Employee.class, query);
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
        Set<String> collection = new HashSet<>();
        Query query = Query.select().where(Employee.NAME.in(collection));
        testReusableQueryWithInCriterionInternal(collection, query, "bigBird", "cookieMonster", "elmo");
        testReusableQueryWithInCriterionInternal(collection, query, "bigBird", "cookieMonster");
        testReusableQueryWithInCriterionInternal(collection, query, "oscar");
        testReusableQueryWithInCriterionInternal(collection, query);
    }

    private void testReusableQueryWithInCriterionInternal(Collection<String> collection, Query query, String... list) {
        collection.clear();
        collection.addAll(Arrays.asList(list));
        SquidCursor<Employee> cursor = database.query(Employee.class, query);
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
        Set<Long> rowIds = new HashSet<>();

        database.beginTransaction();
        try {
            for (int i = 0; i < numRows; i++) {
                TestModel testModel = new TestModel();
                database.persist(testModel);
                rowIds.add(testModel.getRowId());
            }
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
        assertTrue(rowIds.size() > SqlStatement.MAX_VARIABLE_NUMBER);
        assertTrue(database.countAll(TestModel.class) > SqlStatement.MAX_VARIABLE_NUMBER);

        Query query = Query.select(TestModel.ID).where(TestModel.ID.in(rowIds));
        testMaxSqlArgRowIds(query, rowIds.size());

        rowIds.clear();
        rowIds.addAll(Arrays.asList(1L, 2L, 3L, 4L));

        testMaxSqlArgRowIds(query, rowIds.size());
    }

    private void testMaxSqlArgRowIds(Query query, int expectedSize) {
        SquidCursor<TestModel> cursor = database.query(TestModel.class, query);
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

        SquidCursor<Employee> cursor = database.query(Employee.class, query);
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

//    public void x_testReusableQueryPerformance() {
//        String[] values = {"bigBird", "cookieMonster", "elmo", "oscar"};
//        int numIterations = 10000;
//        long start = System.currentTimeMillis();
//        for (int i = 0; i < numIterations; i++) {
//            Query query = Query.select().where(Employee.NAME.eq(values[i % values.length]));
//            database.query(Employee.class, query);
//        }
//        long end = System.currentTimeMillis();
//        System.err.println("Unoptimized took " + (end - start) + " millis");
//
//        AtomicReference<String> reference = new AtomicReference<>();
//        Query query = Query.select().where(Employee.NAME.eq(reference));
//        start = System.currentTimeMillis();
//        for (int i = 0; i < numIterations; i++) {
//            reference.set(values[i % values.length]);
//            database.query(Employee.class, query);
//        }
//        end = System.currentTimeMillis();
//        System.err.println("Optimized took " + (end - start) + " millis");
//    }
//
//    public void x_testReusableListQueryPerformance() {
//        List<?>[] testSets = {
//                Arrays.asList("bigBird", "cookieMonster", "elmo"),
//                Arrays.asList("bigBird", "cookieMonster"),
//                Arrays.asList("bert", "ernie"),
//                Collections.singletonList("oscar"),
//                Collections.emptyList()
//        };
//
//        int numIterations = 10000;
//        long start = System.currentTimeMillis();
//        for (int i = 0; i < numIterations; i++) {
//            Query query = Query.select().where(Employee.NAME.in(testSets[i % testSets.length]));
//            database.query(Employee.class, query);
//        }
//        long end = System.currentTimeMillis();
//        System.err.println("Unoptimized took " + (end - start) + " millis");
//        System.gc();
//        try {
//            Thread.sleep(5000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//        AtomicReference<Collection<?>> ref = new AtomicReference<>();
//        Query query = Query.select().where(Employee.NAME.in(ref));
//        start = System.currentTimeMillis();
//        for (int i = 0; i < numIterations; i++) {
//            ref.set(testSets[i % testSets.length]);
//            database.query(Employee.class, query);
//        }
//        end = System.currentTimeMillis();
//        System.err.println("Optimized took " + (end - start) + " millis");
//    }

    public void testSelectFromView() {
        View view = View.fromQuery(Query.select(Employee.PROPERTIES)
                .from(Employee.TABLE).where(Employee.MANAGER_ID.eq(bigBird.getRowId())), "bigBirdsEmployees");

        database.tryCreateView(view);

        Query fromView = Query.fromView(view).orderBy(view.qualifyField(Employee.ID).asc());

        SquidCursor<Employee> cursor = database.query(Employee.class, fromView);
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

        database.persist(modelOne);
        database.persist(modelTwo);
        database.persist(modelThree);
        assertEquals(3, database.countAll(TestModel.class));

        database.deleteWhere(TestModel.class,
                TestModel.ID.lt(Query.select(Function.max(TestModel.ID)).from(TestModel.TABLE)));
        SquidCursor<TestModel> cursor = null;
        try {
            cursor = database.query(TestModel.class, Query.select());
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

        database.persist(modelOne);
        database.persist(modelTwo);
        database.persist(modelThree);
        database.persist(thingOne);
        database.persist(thingTwo);
        database.persist(thingThree);

        Query query = Query.select(TestModel.FIRST_NAME, TestModel.LAST_NAME).selectMore(Thing.FOO, Thing.BAR)
                .from(TestModel.TABLE)
                .join(Join.inner(Thing.TABLE, Thing.BAR.gt(0)));
        SquidCursor<TestModel> cursor = database.query(TestModel.class, query);
        try {
            assertEquals(6, cursor.getCount());
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                assertTrue(cursor.get(Thing.BAR) > 0);
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
        SquidCursor<TestModel> cursor = database.query(TestModel.class, Query.select().where(Function.abs(one).eq(1)));
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
        SquidCursor<Employee> cursor = database.query(Employee.class, query);
        try {
            assertEquals(1, cursor.getCount());
            cursor.moveToFirst();
            assertEquals(bigBird.getRowId(), cursor.get(Employee.MANAGER_ID).longValue());
        } finally {
            cursor.close();
        }
    }

    public void testJoinWithUsingClause() {
        testJoinWithUsingClauseInternal(false);
        testJoinWithUsingClauseInternal(true);
    }

    private void testJoinWithUsingClauseInternal(boolean leftJoin) {
        final String separator = "|";
        final Map<Long, String> expectedResults = new HashMap<>();
        expectedResults.put(cookieMonster.getRowId(), "2|3|4");
        expectedResults.put(elmo.getRowId(), "2|3|4");
        expectedResults.put(oscar.getRowId(), "2|3|4");
        if (!leftJoin) {
            expectedResults.put(bigBird.getRowId(), "1");
            expectedResults.put(bert.getRowId(), "5");
            expectedResults.put(ernie.getRowId(), "6");
        }

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
        Query subquery = Query.select(aliasedManagerId, subordinates).from(employeesAlias)
                .groupBy(aliasedManagerId);

        if (leftJoin) {
            subquery.having(Function.count().gt(1));
        }

        SqlTable<?> subTable = subquery.as("subTable");
        StringProperty coworkers = subTable.qualifyField(subordinates);
        Query query = Query.select(Employee.PROPERTIES).selectMore(coworkers)
                .from(Employee.TABLE);
        if (leftJoin) {
            query.leftJoin(subTable, Employee.MANAGER_ID);
        } else {
            query.innerJoin(subTable, Employee.MANAGER_ID);
        }

        SquidCursor<Employee> cursor = database.query(Employee.class, query);

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
        SquidCursor<?> c = database.query(null, Query.select(literal, literalLong));
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
        SquidCursor<Employee> cursor = database.query(Employee.class, q);
        try {
            assertFalse(database.countAll(Employee.class) == 0);
        } finally {
            cursor.close();
        }
    }

    public void testFork() {
        Query base = Query.select().from(Employee.TABLE).limit(1);
        Query fork = base.fork().limit(2);
        base.limit(3);

        assertFalse(base == fork);
        assertEquals(Field.field("3"), base.getLimit());
        assertEquals(Field.field("2"), fork.getLimit());
        assertEquals(base.getTable(), fork.getTable());
    }

    public void testQueryFreeze() {
        Query base = Query.select().from(Employee.TABLE).limit(1).freeze();
        Query fork = base.limit(2);

        assertFalse(base == fork);
        assertEquals(Field.field("1"), base.getLimit());
        assertEquals(Field.field("2"), fork.getLimit());
        assertEquals(base.getTable(), fork.getTable());
    }

    public void testFrozenQueryWorksWithDatabase() {
        Query query = Query.select().limit(2).freeze();
        SquidCursor<Employee> cursor = database.query(Employee.class, query);
        try {
            assertEquals(2, cursor.getCount());
            assertNull(query.getTable());
        } finally {
            cursor.close();
        }

        Employee employee = database.fetchByQuery(Employee.class, query);
        assertNotNull(employee);
        assertNull(query.getTable());
        assertEquals(Field.field("2"), query.getLimit());
    }

    // the following four tests all use the same query but different compound operators

    public void testUnion() {
        Query query = Query.select().from(Employee.TABLE).where(Employee.MANAGER_ID.eq(1))
                .union(Query.select().from(Employee.TABLE).where(Employee.ID.eq(2)))
                .orderBy(Employee.ID.asc());
        SquidCursor<Employee> cursor = database.query(Employee.class, query);
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
        SquidCursor<Employee> cursor = database.query(Employee.class, query);
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
        SquidCursor<Employee> cursor = database.query(Employee.class, query);
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
        SquidCursor<Employee> cursor = database.query(Employee.class, query);
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
        SquidCursor<Employee> cursor = database.query(Employee.class, query);
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
        database.persist(testModel);

        Thing thing = new Thing().setFoo("Thingy").setBar(5);
        database.persist(thing);

        Query query = Query.select().from(TestViewModel.VIEW)
                .leftJoin(Thing.TABLE, TestViewModel.TEST_MODEL_ID.eq(Thing.ID));

        TestViewModel model = database.fetchByQuery(TestViewModel.class, query);
        for (Property<?> p : TestViewModel.PROPERTIES) {
            assertTrue(model.containsValue(p));
        }

        for (Property<?> p : Thing.PROPERTIES) {
            assertTrue(model.containsValue(p));
        }
    }

    public void testValidationPropagatesToSubqueryJoinAndCompoundSelect() {
        Query subquery = Query.select(Thing.FOO).from(Thing.TABLE).where(Thing.BAR.gt(0));
        Query joinSubquery = Query.select(Thing.BAR).from(Thing.TABLE).where(Thing.FOO.isNotEmpty());
        Query compoundSubquery = Query.select(Thing.BAZ).from(Thing.TABLE).where(Thing.IS_ALIVE.isTrue());

        SubqueryTable subqueryTable = subquery.as("t1");
        SubqueryTable joinTable = joinSubquery.as("t2");
        Query query = Query.select().from(subqueryTable).innerJoin(joinTable, (Criterion[]) null)
                .union(compoundSubquery);

        final int queryLength = query.compile(database.getCompileContext()).sql.length();

        String withValidation = query.sqlForValidation(database.getCompileContext());
        assertEquals(queryLength + 6, withValidation.length());
    }

    public void testValidationConcurrencyWithSharedState() {
        // If the code to compile queries with extra parentheses for validation isn't threadsafe,
        // one of these should throw a SQL parsing exception. If the test passes, all is well
        final AtomicBoolean compiledOnce = new AtomicBoolean(false);
        final Semaphore blockCriterion = new Semaphore(0);
        final Semaphore blockThread = new Semaphore(0);
        Criterion weirdCriterion = new BinaryCriterion(Thing.BAR, Operator.eq, 0) {
            @Override
            protected void populate(SqlBuilder builder, boolean forSqlValidation) {
                super.populate(builder, forSqlValidation);
                if (compiledOnce.compareAndSet(false, true)) {
                    try {
                        blockThread.release();
                        blockCriterion.acquire();
                    } catch (InterruptedException e) {
                        fail("InterruptedException");
                    }
                }
            }
        };
        Query subquery = Query.select(Thing.FOO).from(Thing.TABLE).where(weirdCriterion);
        final SubqueryTable subqueryTable = subquery.as("t1");

        Query query = Query.select().from(subqueryTable);
        query.requestValidation();

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    blockThread.acquire();

                    Query query2 = Query.select().from(subqueryTable);
                    query2.requestValidation();

                    SquidCursor<Thing> cursor = database.query(Thing.class, query2);
                    cursor.close();
                    blockCriterion.release();
                } catch (InterruptedException e) {
                    fail("InterruptedException");
                }
            }
        });
        t.start();

        SquidCursor<Thing> cursor = database.query(Thing.class, query);
        cursor.close();

        try {
            t.join();
        } catch (InterruptedException e) {
            fail("InterruptedException");
        }
    }

    public void testNeedsValidationUpdatedBySubqueryTable() {
        Query subquery = Query.select(Thing.PROPERTIES).from(Thing.TABLE).where(Criterion.literal(123));
        subquery.requestValidation();
        assertTrue(subquery.compile(database.getCompileContext()).sql.contains("WHERE (?)"));

        Query baseTestQuery = Query.select().from(Thing.TABLE).where(Thing.FOO.isNotEmpty()).freeze();
        assertFalse(baseTestQuery.needsValidation());

        Query testQuery = baseTestQuery.from(subquery.as("t1"));
        assertTrue(testQuery.compile(database.getCompileContext()).needsValidation);
        assertTrue(testQuery.sqlForValidation(database.getCompileContext()).contains("WHERE ((?))"));

        testQuery = baseTestQuery.innerJoin(subquery.as("t2"), (Criterion[]) null);
        assertTrue(testQuery.compile(database.getCompileContext()).needsValidation);
        assertTrue(testQuery.sqlForValidation(database.getCompileContext()).contains("WHERE ((?))"));

        testQuery = baseTestQuery.union(subquery);
        assertTrue(testQuery.compile(database.getCompileContext()).needsValidation);
        assertTrue(testQuery.sqlForValidation(database.getCompileContext()).contains("WHERE ((?))"));
    }

    public void testNeedsValidationUpdatedByQueryFunction() {
        Query subquery = Query.select(Function.max(Thing.ID)).from(Thing.TABLE).where(Criterion.literal(123));
        subquery.requestValidation();
        assertTrue(subquery.compile(database.getCompileContext()).sql.contains("WHERE (?)"));

        Query baseTestQuery = Query.select().from(Thing.TABLE).where(Thing.FOO.isNotEmpty()).freeze();
        assertFalse(baseTestQuery.needsValidation());

        Query testQuery = baseTestQuery.selectMore(subquery.asFunction());
        assertTrue(testQuery.compile(database.getCompileContext()).needsValidation);
        assertTrue(testQuery.sqlForValidation(database.getCompileContext()).contains("WHERE ((?))"));
    }

    public void testLiteralCriterions() {
        // null and not null evaluate to false
        assertEquals(0, database.count(Employee.class, Criterion.literal(null)));
        assertEquals(0, database.count(Employee.class, Criterion.literal(null).negate()));

        // numeric literal; values other than 0 (including negative) evaluate to true
        assertEquals(0, database.count(Employee.class, Criterion.literal(0)));
        assertEquals(6, database.count(Employee.class, Criterion.literal(10)));
        assertEquals(6, database.count(Employee.class, Criterion.literal(-10)));
        assertEquals(6, database.count(Employee.class, Criterion.literal(0).negate()));
        assertEquals(0, database.count(Employee.class, Criterion.literal(10).negate()));

        // text literal; SQLite will try to coerce to a number
        assertEquals(0, database.count(Employee.class, Criterion.literal("sqlite")));
        assertEquals(6, database.count(Employee.class, Criterion.literal("sqlite").negate()));
        assertEquals(6, database.count(Employee.class, Criterion.literal("1sqlite"))); // coerces to 1
        assertEquals(0, database.count(Employee.class, Criterion.literal("1sqlite").negate()));

        // numeric column
        Criterion isHappyCriterion = Employee.IS_HAPPY.asCriterion();
        assertEquals(5, database.count(Employee.class, isHappyCriterion));
        assertEquals(1, database.count(Employee.class, isHappyCriterion.negate()));

        // text column
        Criterion nameCriterion = Employee.NAME.asCriterion();
        assertEquals(0, database.count(Employee.class, nameCriterion));
        assertEquals(6, database.count(Employee.class, nameCriterion.negate()));

        // function
        Criterion f = Function.functionWithArguments("length", "sqlite").asCriterion();
        assertEquals(6, database.count(Employee.class, f));
        assertEquals(0, database.count(Employee.class, f.negate()));
    }

    public void testQueryAsFunction() {
        Table qualifiedTable = Employee.TABLE.as("e1");
        Query subquery = Query.select(Function.add(qualifiedTable.qualifyField(Employee.ID), 1))
                .from(qualifiedTable).where(Employee.ID.eq(qualifiedTable.qualifyField(Employee.ID)));
        Function<Long> fromQuery = subquery.asFunction();
        LongProperty idPlus1 = LongProperty.fromFunction(fromQuery, "idPlus1");
        Query baseQuery = Query.select(Employee.ID, idPlus1);

        SquidCursor<Employee> cursor = database.query(Employee.class, baseQuery);
        try {
            assertEquals(database.countAll(Employee.class), cursor.getCount());
            while (cursor.moveToNext()) {
                assertEquals(cursor.get(Employee.ID) + 1, cursor.get(idPlus1).longValue());
            }
        } finally {
            cursor.close();
        }
    }

    public void testReadUnicodeStrings() {
        // A bunch of random unicode characters
        String unicode = "\u2e17\u301c\ufe58\uff0d\ufe32";
        String reversedUnicode = "\ufe32\uff0d\ufe58\u301c\u2e17";
        TestModel model = insertBasicTestModel(unicode, reversedUnicode, System.currentTimeMillis());

        TestModel fetched = database.fetch(TestModel.class, model.getRowId());
        assertEquals(unicode, fetched.getFirstName());
        assertEquals(reversedUnicode, fetched.getLastName());
    }
}
