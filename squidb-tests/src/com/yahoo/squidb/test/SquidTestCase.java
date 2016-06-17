/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.test;

import com.yahoo.squidb.sql.CompiledStatement;
import com.yahoo.squidb.sql.SqlStatement;
import com.yahoo.squidb.utility.Logger;

import junit.framework.TestCase;

public class SquidTestCase extends TestCase {

    static {
        // Don't need squidb logs for unit tests
        Logger.setLogLevel(Logger.Level.ASSERT);
    }

    public void testSquidTestCaseSetUpProperly() {
        // JUnit doesn't like it if a test class has no tests in it
        assertNotNull(this);
    }

    protected int getReplaceableArgCount(String sql) {
        int argCount = 0;
        int index = 0;
        while (index < sql.length()) {
            int indexOfVarArg = sql.indexOf(SqlStatement.REPLACEABLE_PARAMETER, index);
            if (indexOfVarArg >= 0) {
                argCount++;
                index = indexOfVarArg + 1;
            } else {
                break;
            }
        }

        return argCount;
    }

    protected void verifyCompiledSqlArgs(CompiledStatement compiled, int expectedArgCount, Object... expectedArgs) {
        int argCount = getReplaceableArgCount(compiled.sql);
        assertEquals(expectedArgCount, argCount);
        assertEquals(expectedArgCount, compiled.sqlArgs.length);
        for (int i = 0; i < expectedArgCount; i++) {
            assertEquals(expectedArgs[i], compiled.sqlArgs[i]);
        }
    }

    protected <E extends Exception> void testThrowsException(Runnable toTest, Class<E> expectedException) {
        try {
            toTest.run();
            fail("No exception was thrown");
        } catch (Exception e) {
            assertEquals(expectedException, e.getClass());
        }
    }

    protected void testThrowsRuntimeException(Runnable toTest) {
        try {
            toTest.run();
            fail("No runtime exception was thrown");
        } catch (RuntimeException e) {
            // Success
        }
    }
}
