/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.sql;

import com.yahoo.squidb.data.SquidCursor;
import com.yahoo.squidb.sql.Property.BooleanProperty;
import com.yahoo.squidb.sql.Property.DoubleProperty;
import com.yahoo.squidb.sql.Property.IntegerProperty;
import com.yahoo.squidb.sql.Property.LongProperty;
import com.yahoo.squidb.sql.Property.StringProperty;
import com.yahoo.squidb.test.Constants;
import com.yahoo.squidb.test.DatabaseTestCase;
import com.yahoo.squidb.test.Employee;
import com.yahoo.squidb.test.TestModel;

import java.util.concurrent.atomic.AtomicReference;

public class SqlFunctionsTest extends DatabaseTestCase {

    private TestModel model1;
    private TestModel model2;
    private TestModel model3;

    @Override
    protected void setupDatabase() {
        super.setupDatabase();

        long now = System.currentTimeMillis();
        model1 = new TestModel()
                .setFirstName("Sam")
                .setLastName("Bosley")
                .setBirthday(now)
                .setLuckyNumber(-3);
        database.persist(model1);
        model2 = new TestModel()
                .setFirstName("Kevin")
                .setLastName("Lim")
                .setBirthday(now - Constants.WEEK_IN_MILLIS)
                .setLuckyNumber(0);
        database.persist(model2);
        model3 = new TestModel()
                .setFirstName("Jonathan")
                .setLastName("Koren")
                .setBirthday(now - Constants.HOUR_IN_MILLIS);
        database.persist(model3);
    }

    public void testUpper() {
        StringProperty upper =
                StringProperty.fromFunction(Function.upper(TestModel.FIRST_NAME), "upper");
        TestModel fetch = database.fetch(TestModel.class, model1.getId(), upper);
        assertEquals("SAM", fetch.get(upper));
    }

    public void testLower() {
        StringProperty lower =
                StringProperty.fromFunction(Function.lower(TestModel.LAST_NAME), "lower");
        TestModel fetch = database.fetch(TestModel.class, model3.getId(), lower);
        assertEquals("koren", fetch.get(lower));
    }

    public void testLengthOfString() {
        IntegerProperty length =
                IntegerProperty.fromFunction(Function.length(TestModel.FIRST_NAME), "length");
        TestModel fetch = database.fetch(TestModel.class, model2.getId(), length);
        assertEquals(5, fetch.get(length).intValue());
    }

    public void testLengthOfNumeric() {
        IntegerProperty length =
                IntegerProperty.fromFunction(Function.length(TestModel.BIRTHDAY), "length");
        TestModel fetch = database.fetch(TestModel.class, model1.getId(), length);
        assertEquals(Long.toString(model1.getBirthday()).length(), fetch.get(length).intValue());
    }

    public void testUpperOfLower() {
        StringProperty upperOfLower =
                StringProperty.fromFunction(Function.upper(Function.lower(TestModel.LAST_NAME)), "upperOfLower");
        TestModel fetch = database.fetch(TestModel.class, model1.getId(), upperOfLower);
        assertEquals("BOSLEY", fetch.get(upperOfLower));
    }

    public void testLowerOfUpper() {
        StringProperty lowerOfUpper =
                StringProperty.fromFunction(Function.lower(Function.upper(TestModel.LAST_NAME)), "lowerOfUpper");
        TestModel fetch = database.fetch(TestModel.class, model1.getId(), lowerOfUpper);
        assertEquals("bosley", fetch.get(lowerOfUpper));
    }

    public void testFunctionOnAmbiguousColumnName() {
        IntegerProperty happyCount = IntegerProperty.countProperty(Employee.IS_HAPPY, false);
        Query test = Query.select(TestModel.ID, TestModel.FIRST_NAME, TestModel.IS_HAPPY, happyCount)
                .join(Join.inner(Employee.TABLE, Employee.IS_HAPPY.eq(TestModel.IS_HAPPY)));

        // just test that the query compiles with the function
        database.query(TestModel.class, test);
    }

    public void testSelectFunction() {
        Function<String> upper = Function.upper(TestModel.LAST_NAME);
        SquidCursor<TestModel> cursor = database
                .query(TestModel.class, Query.select(TestModel.PROPERTIES).selectMore(upper));
        try {
            cursor.moveToFirst();
            new TestModel(cursor); // Should succeed without throwing an exception
        } finally {
            cursor.close();
        }
    }

    public void testBooleanFunctionPropertyConstants() {
        BooleanProperty alwaysTrue = BooleanProperty.fromFunction(Function.TRUE, "alwaysTrue");
        BooleanProperty alwaysFalse = BooleanProperty.fromFunction(Function.FALSE, "alwaysFalse");
        SquidCursor<TestModel> cursor = database.query(TestModel.class, Query.select(alwaysTrue, alwaysFalse));
        try {
            cursor.moveToFirst();
            assertTrue(cursor.get(alwaysTrue));
            assertFalse(cursor.get(alwaysFalse));
        } finally {
            cursor.close();
        }
    }

    public void testBooleanFunctionOnCriterion() {
        BooleanProperty onCriterion = BooleanProperty
                .fromFunction(Function.caseWhen(TestModel.FIRST_NAME.eq("Sam")), "firstNameSam");
        SquidCursor<TestModel> cursor = database
                .query(TestModel.class, Query.select(onCriterion).orderBy(Order.asc(TestModel.ID)));
        try {
            cursor.moveToFirst();
            assertTrue(cursor.get(onCriterion));
            cursor.moveToNext();
            assertFalse(cursor.get(onCriterion));
        } finally {
            cursor.close();
        }
    }

    public void testSubstr() {
        testSubstrInternal(2, 0);
        testSubstrInternal(2, 2);
        testSubstrInternal(3, 4);

        String literal = "ABC/DEF";
        StringProperty prefix = StringProperty.literal(literal.substring(0, literal.indexOf('/') + 1), "prefix");
        StringProperty full = StringProperty.literal(literal, "full");

        Field<String> fullField = Field.field(full.getName());
        Field<String> prefixField = Field.field(prefix.getName());
        SquidCursor<?> cursor = database.query(null,
                Query.select(Function.substr(fullField, Function.add(Function.length(prefixField), 1)))
                        .from(Query.select(full, prefix).as("subquery")));
        try {
            assertTrue(cursor.moveToFirst());
            assertEquals("DEF", cursor.getString(0));
        } finally {
            cursor.close();
        }
    }

    private void testSubstrInternal(int offset, int length) {
        Function<String> substr;
        if (length == 0) {
            substr = Function.substr(TestModel.LAST_NAME, offset);
        } else {
            substr = Function.substr(TestModel.LAST_NAME, offset, length);
        }
        StringProperty substrProperty = StringProperty.fromFunction(substr, "substr");
        int trueStart = offset - 1;
        int end = length == 0 ? model1.getLastName().length() : trueStart + length;

        TestModel model = database.fetch(TestModel.class, model1.getId(), substrProperty);
        String substrLastName = model.get(substrProperty);
        String expected = model1.getLastName().substring(trueStart, end);
        assertEquals(expected, substrLastName);
    }

    public void testStrConcat() {
        Function<String> concat = Function.strConcat(TestModel.FIRST_NAME, TestModel.LAST_NAME);
        StringProperty concatProperty = StringProperty.fromFunction(concat, "concat");
        TestModel model = database.fetch(TestModel.class, model1.getId(), concatProperty);
        assertEquals("SamBosley", model.get(concatProperty));

        concat = Function.strConcat(TestModel.FIRST_NAME, " ", TestModel.LAST_NAME);
        concatProperty = StringProperty.fromFunction(concat, "concat");
        model = database.fetch(TestModel.class, model1.getId(), concatProperty);
        assertEquals("Sam Bosley", model.get(concatProperty));
    }

    public void testCoalesce() {
        model2.setFirstName(null); // coalesce should find last name
        database.persist(model2);

        model3.setFirstName(null).setLastName(null); // coalesce should find fallback name
        database.persist(model3);

        final String FALLBACK_NAME = "Squid";
        Function<String> coalesce = Function.coalesce(TestModel.FIRST_NAME, TestModel.LAST_NAME, FALLBACK_NAME);
        StringProperty modelName = StringProperty.fromFunction(coalesce, "name");

        // select *, coalesce(firstName, lastName, 'Squid') as name from testModel order by _id asc;
        SquidCursor<TestModel> cursor = database.query(TestModel.class, Query.select(TestModel.PROPERTIES)
                .selectMore(modelName).orderBy(TestModel.ID.asc()));

        assertEquals(3, cursor.getCount());
        try {
            cursor.moveToFirst();
            assertEquals(model1.getFirstName(), cursor.get(modelName));
            cursor.moveToNext();
            assertEquals(model2.getLastName(), cursor.get(modelName));
            cursor.moveToNext();
            assertEquals(FALLBACK_NAME, cursor.get(modelName));
        } finally {
            cursor.close();
        }
    }

    public void testVariableArgumentsWorkInFunctions() {
        AtomicReference<String> name = new AtomicReference<String>("Sam");
        Function<Integer> caseWhen = Function.caseWhen(TestModel.FIRST_NAME.eq(name));
        BooleanProperty nameMatches = BooleanProperty.fromFunction(caseWhen, "nameMatches");

        Query query = Query.select(TestModel.ID, TestModel.FIRST_NAME, nameMatches).where(TestModel.ID.eq(1));
        TestModel model = database.fetchByQuery(TestModel.class, query);

        assertNotNull(model);
        assertEquals(name.get(), model.getFirstName());
        assertTrue(model.get(nameMatches));

        name.set("Bob");

        model = database.fetchByQuery(TestModel.class, query);
        assertNotNull(model);
        assertNotSame(name.get(), model.getFirstName());
        assertFalse(model.get(nameMatches));
    }

    public void testOrderByFunction() {
        AtomicReference<String> name = new AtomicReference<String>("Sam");
        Function<Integer> caseWhen = Function.caseWhen(TestModel.FIRST_NAME.eq(name));
        BooleanProperty nameMatches = BooleanProperty.fromFunction(caseWhen, "nameMatches");

        Query query = Query.select(TestModel.ID, TestModel.FIRST_NAME, nameMatches).orderBy(nameMatches.asc());
        SquidCursor<TestModel> cursor = database.query(TestModel.class, query);
        try {
            assertEquals(3, cursor.getCount());
            cursor.moveToFirst();
            assertFalse(cursor.get(nameMatches));
            cursor.moveToNext();
            assertFalse(cursor.get(nameMatches));
            cursor.moveToNext();
            assertTrue(cursor.get(nameMatches));
            assertEquals(name.get(), cursor.get(TestModel.FIRST_NAME));
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        name.set("Kevin");
        query = Query.select(TestModel.ID, TestModel.FIRST_NAME, nameMatches).orderBy(nameMatches.desc());
        cursor = database.query(TestModel.class, query);
        try {
            assertEquals(3, cursor.getCount());
            cursor.moveToFirst();
            assertTrue(cursor.get(nameMatches));
            assertEquals(name.get(), cursor.get(TestModel.FIRST_NAME));
            cursor.moveToNext();
            assertFalse(cursor.get(nameMatches));
            cursor.moveToNext();
            assertFalse(cursor.get(nameMatches));
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public void testMath() {
        Function<Integer> addition = Function.add(1, 2, 3, 4, 5);
        IntegerProperty sum = IntegerProperty.fromFunction(addition, "sum");
        testMath(sum, 15);

        Function<Integer> subtraction = Function.subtract(100, 30, 20);
        IntegerProperty difference = IntegerProperty.fromFunction(subtraction, "difference");
        testMath(difference, 50);

        Function<Integer> multiplcation = Function.multiply(1, 2, 3, 4, 5);
        IntegerProperty product = IntegerProperty.fromFunction(multiplcation, "product");
        testMath(product, 120);

        Function<Integer> division = Function.divide(1000, 10, 5);
        IntegerProperty quotient = IntegerProperty.fromFunction(division, "quotient");
        testMath(quotient, 20);

        Function<Integer> modulus = Function.modulo(512, 9);
        IntegerProperty remainder = IntegerProperty.fromFunction(modulus, "remainder");
        testMath(remainder, 8);

        Function<Integer> bitAnd = Function.bitwiseAnd(0xcafe0000, 0xba00, 0xbe);
        IntegerProperty and = IntegerProperty.fromFunction(bitAnd, "bitAnd");
        testMath(and, 0);

        Function<Integer> bitOr = Function.bitwiseOr(0xcafe0000, 0xba00, 0xbe);
        IntegerProperty or = IntegerProperty.fromFunction(bitOr, "bitOr");
        testMath(or, 0xcafebabe);
    }

    private <T extends Number> void testMath(Property<T> property, T expectedValue) {
        SquidCursor<?> cursor = database.query(null, Query.select(property));
        try {
            cursor.moveToFirst();
            T value = cursor.get(property);
            assertEquals(expectedValue, value);
        } finally {
            cursor.close();
        }
    }

    public void testMin() {
        LongProperty minId = LongProperty.fromFunction(Function.min(TestModel.ID), "minId");
        SquidCursor<TestModel> cursor = database.query(TestModel.class, Query.select(minId));
        try {
            cursor.moveToFirst();
            assertEquals(model1.getId(), cursor.get(minId).longValue());
        } finally {
            cursor.close();
        }
    }

    public void testMax() {
        LongProperty maxId = LongProperty.fromFunction(Function.max(TestModel.ID), "maxId");
        SquidCursor<TestModel> cursor = database.query(TestModel.class, Query.select(maxId));
        try {
            cursor.moveToFirst();
            assertEquals(model3.getId(), cursor.get(maxId).longValue());
        } finally {
            cursor.close();
        }
    }

    public void testAvgAndAvgDistinct() {
        setUpAggregateTest();

        DoubleProperty avg = DoubleProperty.fromFunction(Function.avg(TestModel.LUCKY_NUMBER), "avg");
        DoubleProperty avgDistinct = DoubleProperty.fromFunction(
                Function.avgDistinct(TestModel.LUCKY_NUMBER), "avgDistinct");

        SquidCursor<TestModel> cursor = database.query(TestModel.class, Query.select(avg, avgDistinct));
        try {
            cursor.moveToFirst();
            assertEquals(2.0, cursor.get(avg));
            assertEquals(4.0, cursor.get(avgDistinct));
        } finally {
            cursor.close();
        }
    }

    public void testGroupConcat() {
        setUpAggregateTest();

        StringProperty firstNameConcat = StringProperty.fromFunction(
                Function.groupConcat(TestModel.FIRST_NAME), "fname_concat");
        StringProperty firstNameConcatSeparator = StringProperty.fromFunction(
                Function.groupConcat(TestModel.FIRST_NAME, "|"), "fname_concat_separator");
        StringProperty firstNameDistinct = StringProperty.fromFunction(
                Function.groupConcatDistinct(TestModel.FIRST_NAME), "fname_distinct");
        SquidCursor<TestModel> cursor = database.query(TestModel.class,
                Query.select(firstNameConcat, firstNameConcatSeparator, firstNameDistinct)
                        .groupBy(TestModel.FIRST_NAME));
        try {
            assertEquals(2, cursor.getCount());
            cursor.moveToFirst();
            assertEquals("A,A,A", cursor.get(firstNameConcat));
            assertEquals("A|A|A", cursor.get(firstNameConcatSeparator));
            assertEquals("A", cursor.get(firstNameDistinct));
            cursor.moveToNext();
            assertEquals("B,B,B", cursor.get(firstNameConcat));
            assertEquals("B|B|B", cursor.get(firstNameConcatSeparator));
            assertEquals("B", cursor.get(firstNameDistinct));
        } finally {
            cursor.close();
        }
    }

    public void testSumAndSumDistinct() {
        setUpAggregateTest();

        IntegerProperty sum = IntegerProperty.fromFunction(
                Function.sum(TestModel.LUCKY_NUMBER), "sum");
        IntegerProperty sumDistinct = IntegerProperty.fromFunction(
                Function.sumDistinct(TestModel.LUCKY_NUMBER), "sumDistinct");
        SquidCursor<TestModel> cursor = database.query(TestModel.class, Query.select(sum, sumDistinct));
        try {
            assertEquals(1, cursor.getCount());
            cursor.moveToFirst();
            assertEquals(12, cursor.get(sum).intValue());
            assertEquals(8, cursor.get(sumDistinct).intValue());
        } finally {
            cursor.close();
        }
    }

    private void setUpAggregateTest() {
        database.clear();
        long now = System.currentTimeMillis();
        peristModelForDistinctAggregates("A", "A", now - 3, 1);
        peristModelForDistinctAggregates("A", "B", now - 2, 1);
        peristModelForDistinctAggregates("A", "C", now - 1, 1);

        peristModelForDistinctAggregates("B", "D", now + 1, 1);
        peristModelForDistinctAggregates("B", "E", now + 2, 1);
        peristModelForDistinctAggregates("B", "F", now + 3, 7);
    }

    private void peristModelForDistinctAggregates(String firstName, String lastName, long birthday, int luckyNumber) {
        database.persist(new TestModel().setFirstName(firstName).setLastName(lastName)
                .setBirthday(birthday).setLuckyNumber(luckyNumber));
    }

    public void testCaseWhen() {
        final String PASS = "PASS";
        final String FAIL = "FAIL";
        Function<String> caseWhen = Function.caseExpr("Hello")
                .when("a", FAIL)
                .when("b", FAIL)
                .when("c", FAIL)
                .elseExpr(PASS)
                .end();
        Property<?> test = StringProperty.fromFunction(caseWhen, "test");
        assertExpectedValues(Query.select(test), test, PASS);

        // no ELSE branch, returns null if no WHEN branch passes
        caseWhen = Function.caseExpr("Hello")
                .when("a", FAIL)
                .when("b", FAIL)
                .when("c", FAIL)
                .end();
        test = StringProperty.fromFunction(caseWhen, "test");
        assertExpectedValues(Query.select(test), test, new Object[]{null});

        // null as base expression only evaluates ELSE branch
        caseWhen = Function.caseExpr(Field.NULL)
                .when(null, FAIL)
                .when(Field.NULL, FAIL)
                .elseExpr(PASS)
                .end();
        test = StringProperty.fromFunction(caseWhen, "test");
        assertExpectedValues(Query.select(test), test, PASS);

        caseWhen = Function.caseWhen(IntegerProperty.literal(1, null).gt(2), FAIL)
                .when(IntegerProperty.literal(3, null).gt(0), PASS)
                .elseExpr(FAIL)
                .end();
        test = StringProperty.fromFunction(caseWhen, "test");
        assertExpectedValues(Query.select(test), test, PASS);

        // without base expression
        caseWhen = Function.caseWhen(TestModel.LUCKY_NUMBER.gt(0), "positive")
                .when(TestModel.LUCKY_NUMBER.lt(0), "negative")
                .elseExpr("zero")
                .end();
        test = StringProperty.fromFunction(caseWhen, "test");
        assertExpectedValues(Query.select(test).from(TestModel.TABLE).orderBy(TestModel.ID.asc()), test, "negative",
                "zero", "positive");

        // with base expression
        caseWhen = Function.caseExpr(TestModel.LUCKY_NUMBER)
                .when(0, "zero")
                .when(7, "default")
                .elseExpr("other")
                .end();
        test = StringProperty.fromFunction(caseWhen, "test");
        assertExpectedValues(Query.select(test).from(TestModel.TABLE).orderBy(TestModel.ID.asc()), test, "other",
                "zero", "default");

        // convenience functions
        Function<Integer> longNameFunc = Function.caseWhen(Function.length(TestModel.FIRST_NAME).gt(6));
        IntegerProperty hasLongName = IntegerProperty.fromFunction(longNameFunc, "hasLongName");
        assertExpectedValues(Query.select(hasLongName).from(TestModel.TABLE).orderBy(TestModel.ID.asc()), hasLongName,
                0, 0, 1);

        Function<String> longNameFunc2 = Function.caseWhen(Function.length(TestModel.FIRST_NAME).gt(6), PASS, FAIL);
        StringProperty hasLongName2 = StringProperty.fromFunction(longNameFunc2, "hasLongName");
        assertExpectedValues(Query.select(hasLongName2).from(TestModel.TABLE).orderBy(TestModel.ID.asc()), hasLongName2,
                FAIL, FAIL, PASS);
    }

    private void assertExpectedValues(Query query, Property<?> property, Object... expectedValues) {
        final int expectedCount = expectedValues == null ? 0 : expectedValues.length;
        SquidCursor<?> cursor = database.query(null, query);
        try {
            assertEquals(expectedCount, cursor.getCount());
            for (int i = 0; i < expectedCount; i++) {
                cursor.moveToPosition(i);
                assertEquals(expectedValues[i], cursor.get(property));
            }
        } finally {
            cursor.close();
        }
    }

    public void testCast() {
        Function<String> castToString = Function.cast(Field.field("x'61'"), "TEXT");
        SquidCursor<?> cursor = database.query(null, Query.select(castToString));
        try {
            cursor.moveToFirst();
            assertEquals("a", cursor.getString(0));
        } finally {
            cursor.close();
        }
    }
}
