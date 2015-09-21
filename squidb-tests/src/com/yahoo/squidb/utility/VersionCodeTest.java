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
        final int[] testValues = new int[4];
        for (int i = 0; i < testValues.length; i++) {
            testValues[0] = testValues[1] = testValues[2] = testValues[3] = 1;
            testValues[i] = -1;

            testThrowsException(new Runnable() {

                @Override
                public void run() {
                    new VersionCode(testValues[0], testValues[1], testValues[2], testValues[3]);
                }
            }, IllegalArgumentException.class);
        }
    }

    public void testVersionIsLessThan() {
        VersionCode versionToTest = new VersionCode(4, 2, 0, 1, "test");

        // digits -- gte
        assertFalse(versionToTest.isLessThan(new VersionCode(0, 0, 0, 2)));
        assertFalse(versionToTest.isLessThan(new VersionCode(1, 2, 3, 4)));
        assertFalse(versionToTest.isLessThan(new VersionCode(3, 18, 7, 0)));
        assertFalse(versionToTest.isLessThan(new VersionCode(4, 1, 999, 2)));
        assertFalse(versionToTest.isLessThan(new VersionCode(4, 2, 0, 1)));
        // -- lt
        assertTrue(versionToTest.isLessThan(new VersionCode(4, 2, 1, 0)));
        assertTrue(versionToTest.isLessThan(new VersionCode(4, 3, 0, 0)));
        assertTrue(versionToTest.isLessThan(new VersionCode(5, 0, 5, 0)));

        // strings -- gte
        assertFalse(versionToTest.isLessThan("0"));
        assertFalse(versionToTest.isLessThan("3.7.51-beta"));
        assertFalse(versionToTest.isLessThan("4.1.37285"));
        assertFalse(versionToTest.isLessThan("4.2rc1"));
        assertFalse(versionToTest.isLessThan("4.2.0.1"));
        // -- lt
        assertTrue(versionToTest.isLessThan("4.2.1.5"));
        assertTrue(versionToTest.isLessThan("4.3.0-is-this-the-real-life"));
        assertTrue(versionToTest.isLessThan("5."));
        assertTrue(versionToTest.isLessThan("1000"));
    }

    public void testVersionIsAtLeast() {
        VersionCode versionToTest = new VersionCode(4, 2, 0, 1, "test");

        // digits -- gte
        assertTrue(versionToTest.isAtLeast(new VersionCode(0, 0, 0, 2)));
        assertTrue(versionToTest.isAtLeast(new VersionCode(1, 2, 3, 4)));
        assertTrue(versionToTest.isAtLeast(new VersionCode(3, 18, 7, 0)));
        assertTrue(versionToTest.isAtLeast(new VersionCode(4, 1, 999, 2)));
        assertTrue(versionToTest.isAtLeast(new VersionCode(4, 2, 0, 1)));
        // -- lt
        assertFalse(versionToTest.isAtLeast(new VersionCode(4, 2, 1, 0)));
        assertFalse(versionToTest.isAtLeast(new VersionCode(4, 3, 0, 0)));
        assertFalse(versionToTest.isAtLeast(new VersionCode(5, 0, 5, 0)));

        // strings -- gte
        assertTrue(versionToTest.isAtLeast("0"));
        assertTrue(versionToTest.isAtLeast("3.7.51-beta"));
        assertTrue(versionToTest.isAtLeast("4.1.37285"));
        assertTrue(versionToTest.isAtLeast("4.2rc1"));
        assertTrue(versionToTest.isAtLeast("4.2"));
        assertTrue(versionToTest.isAtLeast("4.2.0.1"));
        // -- lt
        assertFalse(versionToTest.isAtLeast("4.2.1.5"));
        assertFalse(versionToTest.isAtLeast("4.3.0-is-this-the-real-life"));
        assertFalse(versionToTest.isAtLeast("5."));
        assertFalse(versionToTest.isAtLeast("1000"));
    }

    public void testVersionParse() {
        assertEquals(new VersionCode(3, 8, 7, 11), VersionCode.parse("3.8.7.11"));
        assertEquals(new VersionCode(3, 8, 7, 0), VersionCode.parse("3.8.7"));
        assertEquals(new VersionCode(3, 8, 0, 0), VersionCode.parse("3.8"));
        assertEquals(new VersionCode(3, 0, 0, 0), VersionCode.parse("3"));
    }
}
