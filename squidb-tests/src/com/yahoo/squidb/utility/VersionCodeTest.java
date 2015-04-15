/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.utility;

import com.yahoo.squidb.test.SquidTestCase;

public class VersionCodeTest extends SquidTestCase {

    public void testEmptyStringThrowsIllegalArgumentException() {
        testThrowsException(new Runnable() {

            @Override
            public void run() {
                VersionCode.parse("");
            }
        }, IllegalArgumentException.class);
    }

    public void testInvalidStringThrowsIllegalArgumentException() {
        testThrowsException(new Runnable() {

            @Override
            public void run() {
                VersionCode.parse("foo.bar.baz");
            }
        }, IllegalArgumentException.class);
    }

    public void testConstructorThrowsIllegalArgumentExceptionForNegativeValues() {
        final int[] testValues = new int[3];
        for (int i = 0; i < 3; i++) {
            testValues[0] = testValues[1] = testValues[2] = 1;
            testValues[i] = -1;

            testThrowsException(new Runnable() {

                @Override
                public void run() {
                    new VersionCode(testValues[0], testValues[1], testValues[2]);
                }
            }, IllegalArgumentException.class);
        }
    }

    public void testVersionIsLessThan() {
        VersionCode versionToTest = new VersionCode(4, 2, 0, "test");

        // digits -- gte
        assertFalse(versionToTest.isLessThan(new VersionCode(0, 0, 0)));
        assertFalse(versionToTest.isLessThan(new VersionCode(1, 2, 3)));
        assertFalse(versionToTest.isLessThan(new VersionCode(3, 18, 7)));
        assertFalse(versionToTest.isLessThan(new VersionCode(4, 1, 999)));
        assertFalse(versionToTest.isLessThan(new VersionCode(4, 2, 0)));
        // -- lt
        assertTrue(versionToTest.isLessThan(new VersionCode(4, 2, 1)));
        assertTrue(versionToTest.isLessThan(new VersionCode(4, 3, 0)));
        assertTrue(versionToTest.isLessThan(new VersionCode(5, 0, 5)));

        // strings -- gte
        assertFalse(versionToTest.isLessThan(VersionCode.parse("0")));
        assertFalse(versionToTest.isLessThan(VersionCode.parse("3.7.51-beta")));
        assertFalse(versionToTest.isLessThan(VersionCode.parse("4.1.37285")));
        assertFalse(versionToTest.isLessThan(VersionCode.parse("4.2rc1")));
        assertFalse(versionToTest.isLessThan(VersionCode.parse("4.2")));
        // -- lt
        assertTrue(versionToTest.isLessThan(VersionCode.parse("4.2.1")));
        assertTrue(versionToTest.isLessThan(VersionCode.parse("4.3.0-is-this-the-real-life")));
        assertTrue(versionToTest.isLessThan(VersionCode.parse("5.")));
        assertTrue(versionToTest.isLessThan(VersionCode.parse("1000")));
    }

    public void testVersionIsAtLeast() {
        VersionCode versionToTest = new VersionCode(4, 2, 0, "test");

        // digits -- gte
        assertTrue(versionToTest.isAtLeast(new VersionCode(0, 0, 0)));
        assertTrue(versionToTest.isAtLeast(new VersionCode(1, 2, 3)));
        assertTrue(versionToTest.isAtLeast(new VersionCode(3, 18, 7)));
        assertTrue(versionToTest.isAtLeast(new VersionCode(4, 1, 999)));
        assertTrue(versionToTest.isAtLeast(new VersionCode(4, 2, 0)));
        // -- lt
        assertFalse(versionToTest.isAtLeast(new VersionCode(4, 2, 1)));
        assertFalse(versionToTest.isAtLeast(new VersionCode(4, 3, 0)));
        assertFalse(versionToTest.isAtLeast(new VersionCode(5, 0, 5)));

        // strings -- gte
        assertTrue(versionToTest.isAtLeast(VersionCode.parse("0")));
        assertTrue(versionToTest.isAtLeast(VersionCode.parse("3.7.51-beta")));
        assertTrue(versionToTest.isAtLeast(VersionCode.parse("4.1.37285")));
        assertTrue(versionToTest.isAtLeast(VersionCode.parse("4.2rc1")));
        assertTrue(versionToTest.isAtLeast(VersionCode.parse("4.2")));
        // -- lt
        assertFalse(versionToTest.isAtLeast(VersionCode.parse("4.2.1")));
        assertFalse(versionToTest.isAtLeast(VersionCode.parse("4.3.0-is-this-the-real-life")));
        assertFalse(versionToTest.isAtLeast(VersionCode.parse("5.")));
        assertFalse(versionToTest.isAtLeast(VersionCode.parse("1000")));
    }
}
