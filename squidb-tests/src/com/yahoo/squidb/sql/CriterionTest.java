/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.sql;

import com.yahoo.squidb.test.SquidTestCase;
import com.yahoo.squidb.test.TestModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CriterionTest extends SquidTestCase {

    public void testCriterionNegation() {
        assertNegationEqualsTheOther(TestModel.FIRST_NAME.eq("Sam"), TestModel.FIRST_NAME.neq("Sam"));
        assertNegationEqualsTheOther(TestModel.FIRST_NAME.isNull(), TestModel.FIRST_NAME.isNotNull());
        assertNegationEqualsTheOther(TestModel.LUCKY_NUMBER.gt(5), TestModel.LUCKY_NUMBER.lte(5));
        assertNegationEqualsTheOther(TestModel.LUCKY_NUMBER.gte(5), TestModel.LUCKY_NUMBER.lt(5));
        assertNegationEqualsTheOther(TestModel.FIRST_NAME.in(Collections.EMPTY_SET),
                TestModel.FIRST_NAME.notIn(Collections.EMPTY_SET));
        assertNegationEqualsTheOther(TestModel.FIRST_NAME.isNull(), TestModel.FIRST_NAME.isNotNull());
        assertNegationEqualsTheOther(TestModel.LUCKY_NUMBER.between(1, 2), TestModel.LUCKY_NUMBER.notBetween(1, 2));
        assertNegationEqualsTheOther(TestModel.FIRST_NAME.like("A"), TestModel.FIRST_NAME.notLike("A"));
        assertNegationEqualsTheOther(TestModel.FIRST_NAME.glob("A*"), TestModel.FIRST_NAME.notGlob("A*"));
    }

    private void assertNegationEqualsTheOther(Criterion c1, Criterion c2) {
        assertEquals(c1, c2.negate());
        assertEquals(c2, c1.negate());
        assertEquals(c1, c1.negate().negate());
        assertEquals(c2, c2.negate().negate());
    }

    public void testInstanceVsStaticConjunction() {
        Criterion c1 = TestModel.FIRST_NAME.eq("Sam");
        Criterion c2 = TestModel.LAST_NAME.eq("Bosley");
        Criterion c3 = TestModel.LUCKY_NUMBER.eq(7);

        Criterion c = c1.and(c2);
        Criterion cand = c.and(c3);
        assertNotSame(c, cand); // Tests immutability of conjunction criterions when appending with the same operator

        assertEquals(Criterion.and(c1, c2, c3), c1.and(c2).and(c3));
        assertEquals(Criterion.and(Arrays.asList(c1, c2, c3)), c1.and(c2).and(c3));
        assertEquals(Criterion.or(c1, c2, c3), c1.or(c2).or(c3));
        assertEquals(Criterion.or(Arrays.asList(c1, c2, c3)), c1.or(c2).or(c3));

        assertEquals(Criterion.or(Criterion.and(c1, c2), c3), c1.and(c2).or(c3));
        assertEquals(Criterion.and(Criterion.or(c1, c2), c3), c1.or(c2).and(c3));

        assertEquals(Criterion.and(c1, Criterion.or(c2, c3)), c1.and(c2.or(c3)));
        assertEquals(Criterion.or(c1, Criterion.and(c2, c3)), c1.or(c2.and(c3)));
    }

    public void testInCriterionWithEmptySets() {
        String expectedInString = "(testModels._id IN ())";
        String expectedNotInString = "(testModels._id NOT IN ())";
        Object[] emptyArray = new Object[0];

        assertEquals(expectedInString, TestModel.ID.in((List<Long>) null).toString());
        assertEquals(expectedInString, TestModel.ID.in((Object[]) null).toString());
        assertEquals(expectedInString, TestModel.ID.in((Query) null).toString());
        assertEquals(expectedInString, TestModel.ID.in(new ArrayList<Long>()).toString());
        assertEquals(expectedInString, TestModel.ID.in((Object[]) emptyArray).toString());
        assertEquals(expectedInString, TestModel.ID.in().toString());

        assertEquals(expectedNotInString, TestModel.ID.notIn((List<Long>) null).toString());
        assertEquals(expectedNotInString, TestModel.ID.notIn((Object[]) null).toString());
        assertEquals(expectedNotInString, TestModel.ID.notIn((Query) null).toString());
        assertEquals(expectedNotInString, TestModel.ID.notIn(new ArrayList<Long>()).toString());
        assertEquals(expectedNotInString, TestModel.ID.notIn((Object[]) emptyArray).toString());
        assertEquals(expectedNotInString, TestModel.ID.notIn().toString());
    }

    public void testEmptyRawSelectionReturnsNull() {
        assertNull(Criterion.fromRawSelection(null, null));
        assertNull(Criterion.fromRawSelection("", null));
    }

    public void testNullCriterionsInBadPlacesThrowExceptions() {
        testThrowsException(new Runnable() {
            @Override
            public void run() {
                Criterion.and(null, TestModel.ID.eq(1));
            }
        }, IllegalArgumentException.class);

        testThrowsException(new Runnable() {
            @Override
            public void run() {
                Criterion.or(null, TestModel.ID.eq(1));
            }
        }, IllegalArgumentException.class);

        testThrowsException(new Runnable() {
            @Override
            public void run() {
                Function.caseWhen(null, 1);
            }
        }, IllegalArgumentException.class);

        testThrowsException(new Runnable() {
            @Override
            public void run() {
                Criterion.not(null);
            }
        }, IllegalArgumentException.class);
    }
}
