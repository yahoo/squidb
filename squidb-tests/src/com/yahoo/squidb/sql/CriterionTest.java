/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.sql;

import com.yahoo.squidb.test.SquidTestCase;
import com.yahoo.squidb.test.TestModel;

import java.util.Collections;

public class CriterionTest extends SquidTestCase {

    public void testCriterionNegation() {
        assertNegationEqualsTheOther(TestModel.FIRST_NAME.eq("Sam"), TestModel.FIRST_NAME.neq("Sam"));
        assertNegationEqualsTheOther(TestModel.FIRST_NAME.isNull(), TestModel.FIRST_NAME.isNotNull());
        assertNegationEqualsTheOther(TestModel.LUCKY_NUMBER.gt(5), TestModel.LUCKY_NUMBER.lte(5));
        assertNegationEqualsTheOther(TestModel.LUCKY_NUMBER.gte(5), TestModel.LUCKY_NUMBER.lt(5));
        assertNegationEqualsTheOther(TestModel.FIRST_NAME.in(Collections.EMPTY_SET),
                Criterion.not(TestModel.FIRST_NAME.in(Collections.EMPTY_SET)));
        assertNegationEqualsTheOther(TestModel.FIRST_NAME.isNull(), TestModel.FIRST_NAME.isNotNull());
        assertNegationEqualsTheOther(Criterion.all, Criterion.none);
    }

    private void assertNegationEqualsTheOther(Criterion c1, Criterion c2) {
        assertEquals(c1, c2.negate());
        assertEquals(c2, c1.negate());
        assertEquals(c1, c1.negate().negate());
        assertEquals(c2, c2.negate().negate());
    }

    public void testInstanceVsStaticAnd() {
        Criterion c1 = TestModel.FIRST_NAME.eq("Sam");
        Criterion c2 = TestModel.LAST_NAME.eq("Bosley");
        Criterion c3 = TestModel.LUCKY_NUMBER.eq(7);

        Criterion c = c1.and(c2);
        Criterion cand = c.and(c3);
        assertNotSame(c, cand); // Tests immutability of conjunction criterions when appending with the same operator

        assertEquals(Criterion.and(c1, c2, c3), c1.and(c2).and(c3));
        assertEquals(Criterion.or(c1, c2, c3), c1.or(c2).or(c3));

        assertEquals(Criterion.or(Criterion.and(c1, c2), c3), c1.and(c2).or(c3));
        assertEquals(Criterion.and(Criterion.or(c1, c2), c3), c1.or(c2).and(c3));

        assertEquals(Criterion.and(c1, Criterion.or(c2, c3)), c1.and(c2.or(c3)));
        assertEquals(Criterion.or(c1, Criterion.and(c2, c3)), c1.or(c2.and(c3)));
    }

    public void testEmptyRawSelectionIsEquivalentToAll() {
        assertEquals(Criterion.all, Criterion.fromRawSelection(null, null));
        assertEquals(Criterion.all, Criterion.fromRawSelection("", null));
    }
}
