/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.sql;

import com.yahoo.squidb.test.DatabaseTestCase;
import com.yahoo.squidb.test.TestModel;

public class SqlUtilsTest extends DatabaseTestCase {

    public void testEscapeLikePattern() {
        assertEquals("", SqlUtils.escapeLikePattern(null, '^'));
        assertEquals("", SqlUtils.escapeLikePattern("", '^'));
        assertEquals("abc", SqlUtils.escapeLikePattern("abc", '^'));
        assertEquals("a^%c", SqlUtils.escapeLikePattern("a%c", '^'));
        assertEquals("a^%^%c", SqlUtils.escapeLikePattern("a%%c", '^'));
        assertEquals("a^_c", SqlUtils.escapeLikePattern("a_c", '^'));
        assertEquals("a^_^_^_c", SqlUtils.escapeLikePattern("a___c", '^'));
        assertEquals("a^^b^^c", SqlUtils.escapeLikePattern("a^b^c", '^'));

        // works with java escape sequences
        assertEquals("", SqlUtils.escapeLikePattern(null, '\\'));
        assertEquals("", SqlUtils.escapeLikePattern("", '\\'));
        assertEquals("123", SqlUtils.escapeLikePattern("123", '\\'));
        assertEquals("1\\%3", SqlUtils.escapeLikePattern("1%3", '\\'));
        assertEquals("1\\%\\%3", SqlUtils.escapeLikePattern("1%%3", '\\'));
        assertEquals("1\\_3", SqlUtils.escapeLikePattern("1_3", '\\'));
        assertEquals("1\\_\\_\\_3", SqlUtils.escapeLikePattern("1___3", '\\'));
        assertEquals("1\\\\2\\\\3", SqlUtils.escapeLikePattern("1\\2\\3", '\\'));

        // doesn't allow LIKE meta-characters as escape term
        testThrowsException(new Runnable() {
            @Override
            public void run() {
                SqlUtils.escapeLikePattern("foo", '%');
            }
        }, IllegalArgumentException.class);

        testThrowsException(new Runnable() {
            @Override
            public void run() {
                SqlUtils.escapeLikePattern("bar", '_');
            }
        }, IllegalArgumentException.class);
    }

    public void testSanitizeString() {
        assertEquals("'Sam''s'", SqlUtils.toSanitizedString("Sam's"));
        assertEquals("CAST(ZEROBLOB(1) AS TEXT)", SqlUtils.toSanitizedString("\0"));
        assertEquals("CAST(ZEROBLOB(2) AS TEXT)", SqlUtils.toSanitizedString("\0\0"));
        assertEquals("CAST(ZEROBLOB(1) AS TEXT) || 'ABC'", SqlUtils.toSanitizedString("\0ABC"));
        assertEquals("CAST(ZEROBLOB(2) AS TEXT) || 'ABC'", SqlUtils.toSanitizedString("\0\0ABC"));
        assertEquals("CAST(ZEROBLOB(1) AS TEXT) || 'A' || CAST(ZEROBLOB(1) AS TEXT) || 'B''C'",
                SqlUtils.toSanitizedString("\0A\0B'C"));
        assertEquals("CAST(ZEROBLOB(1) AS TEXT) || 'ABC' || CAST(ZEROBLOB(1) AS TEXT)",
                SqlUtils.toSanitizedString("\0ABC\0"));
        assertEquals("CAST(ZEROBLOB(1) AS TEXT) || 'ABC' || CAST(ZEROBLOB(2) AS TEXT)",
                SqlUtils.toSanitizedString("\0ABC\0\0"));
        assertEquals("'A' || CAST(ZEROBLOB(1) AS TEXT) || 'BC' || CAST(ZEROBLOB(1) AS TEXT)",
                SqlUtils.toSanitizedString("A\0BC\0"));
        assertEquals("'A' || CAST(ZEROBLOB(2) AS TEXT) || 'BC' || CAST(ZEROBLOB(1) AS TEXT)",
                SqlUtils.toSanitizedString("A\0\0BC\0"));
        assertEquals("'A' || CAST(ZEROBLOB(1) AS TEXT) || 'B' || CAST(ZEROBLOB(1) AS TEXT) || 'C'",
                SqlUtils.toSanitizedString("A\0B\0C"));
        assertEquals("'ABC' || CAST(ZEROBLOB(1) AS TEXT)", SqlUtils.toSanitizedString("ABC\0"));
    }

    public void testDatabaseWriteWithNullCharactersWorks() {
        testBadString("Sam\0B", 5);
        testBadString("Sam\0\0B", 6);
        testBadString("\0Sam\0B", 6);
        testBadString("\0Sam\0B\0", 7);
    }

    private void testBadString(String badString, int expectedLength) {
        assertEquals(expectedLength, badString.length());
        TestModel model = new TestModel().setFirstName(badString).setLastName("Bosley").setBirthday(testDate);
        database.persist(model);

        model = database.fetch(TestModel.class, model.getId());
        assertEquals(badString, model.getFirstName());

        database.update(TestModel.FIRST_NAME.in(badString), new TestModel().setFirstName("Sam"));

        model = database.fetch(TestModel.class, model.getId());
        assertEquals("Sam", model.getFirstName());
        database.delete(TestModel.class, model.getId());
    }
}
