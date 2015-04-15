/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.sql;

import com.yahoo.squidb.test.SquidTestCase;

public class SqlUtilsTest extends SquidTestCase {

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
}
