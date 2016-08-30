/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.sql;

import com.yahoo.squidb.test.DatabaseTestCase;
import com.yahoo.squidb.test.TestEnum;
import com.yahoo.squidb.test.TestModel;

import java.util.concurrent.atomic.AtomicBoolean;

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

    public void testSanitizeStringEscapesSingleQuotes() {
        ArgumentResolver defaultArgumentResolver = new DefaultArgumentResolver();
        assertEquals("'Sam''s'", SqlUtils.toSanitizedString("Sam's", defaultArgumentResolver));
    }

    public void testSanitizeStringUsesArgumentResolver() {
        ArgumentResolver argumentResolver = new DefaultArgumentResolver();
        assertEquals("'APPLE'", SqlUtils.toSanitizedString(TestEnum.APPLE, argumentResolver));
        assertEquals("1", SqlUtils.toSanitizedString(new AtomicBoolean(true), argumentResolver));

        argumentResolver = new DefaultArgumentResolver() {
            @Override
            protected boolean canResolveCustomType(Object arg) {
                return arg instanceof Enum<?>;
            }

            @Override
            protected Object resolveCustomType(Object arg) {
                return ((Enum<?>) arg).ordinal();
            }
        };
        assertEquals("0", SqlUtils.toSanitizedString(TestEnum.APPLE, argumentResolver));
    }

    public void testSanitizeStringHandlesNullCharacters() {
        ArgumentResolver defaultArgumentResolver = new DefaultArgumentResolver();
        assertEquals("CAST(ZEROBLOB(1) AS TEXT)", SqlUtils.toSanitizedString("\0", defaultArgumentResolver));
        assertEquals("CAST(ZEROBLOB(2) AS TEXT)", SqlUtils.toSanitizedString("\0\0", defaultArgumentResolver));
        assertEquals("CAST(ZEROBLOB(1) AS TEXT) || 'ABC'",
                SqlUtils.toSanitizedString("\0ABC", defaultArgumentResolver));
        assertEquals("CAST(ZEROBLOB(2) AS TEXT) || 'ABC'",
                SqlUtils.toSanitizedString("\0\0ABC", defaultArgumentResolver));
        assertEquals("CAST(ZEROBLOB(1) AS TEXT) || 'A' || CAST(ZEROBLOB(1) AS TEXT) || 'B''C'",
                SqlUtils.toSanitizedString("\0A\0B'C", defaultArgumentResolver));
        assertEquals("CAST(ZEROBLOB(1) AS TEXT) || 'ABC' || CAST(ZEROBLOB(1) AS TEXT)",
                SqlUtils.toSanitizedString("\0ABC\0", defaultArgumentResolver));
        assertEquals("CAST(ZEROBLOB(1) AS TEXT) || 'ABC' || CAST(ZEROBLOB(2) AS TEXT)",
                SqlUtils.toSanitizedString("\0ABC\0\0", defaultArgumentResolver));
        assertEquals("'A' || CAST(ZEROBLOB(1) AS TEXT) || 'BC' || CAST(ZEROBLOB(1) AS TEXT)",
                SqlUtils.toSanitizedString("A\0BC\0", defaultArgumentResolver));
        assertEquals("'A' || CAST(ZEROBLOB(2) AS TEXT) || 'BC' || CAST(ZEROBLOB(1) AS TEXT)",
                SqlUtils.toSanitizedString("A\0\0BC\0", defaultArgumentResolver));
        assertEquals("'A' || CAST(ZEROBLOB(1) AS TEXT) || 'B' || CAST(ZEROBLOB(1) AS TEXT) || 'C'",
                SqlUtils.toSanitizedString("A\0B\0C", defaultArgumentResolver));
        assertEquals("'ABC' || CAST(ZEROBLOB(1) AS TEXT)",
                SqlUtils.toSanitizedString("ABC\0", defaultArgumentResolver));
    }

    public void testBlobLiterals() {
        ArgumentResolver defaultArgumentResolver = new DefaultArgumentResolver();
        assertEquals("X''", SqlUtils.toSanitizedString(new byte[0], defaultArgumentResolver));
        assertEquals("X'94'", SqlUtils.toSanitizedString(new byte[]{(byte) 0x94}, defaultArgumentResolver));
        assertEquals("X'5daa'", SqlUtils.toSanitizedString(new byte[]{(byte) 0x5d, (byte) 0xaa},
                defaultArgumentResolver));

        assertEquals("X'08312f'", SqlUtils.toSanitizedString(
                new byte[]{(byte) 0x08, (byte) 0x31, (byte) 0x2f}, defaultArgumentResolver));

        assertEquals("X'7be6cd4b'", SqlUtils.toSanitizedString(
                new byte[]{(byte) 0x7b, (byte) 0xe6, (byte) 0xcd, (byte) 0x4b}, defaultArgumentResolver));
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

        model = database.fetch(TestModel.class, model.getRowId());
        assertEquals(badString, model.getFirstName());

        database.update(TestModel.FIRST_NAME.in(badString), new TestModel().setFirstName("Sam"));

        model = database.fetch(TestModel.class, model.getRowId());
        assertEquals("Sam", model.getFirstName());
        database.delete(TestModel.class, model.getRowId());
    }
}
