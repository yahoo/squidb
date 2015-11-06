/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.data;

import com.yahoo.squidb.sql.Function;
import com.yahoo.squidb.sql.Property.BooleanProperty;
import com.yahoo.squidb.sql.Property.IntegerProperty;
import com.yahoo.squidb.sql.Property.StringProperty;
import com.yahoo.squidb.sql.Query;
import com.yahoo.squidb.test.DatabaseTestCase;
import com.yahoo.squidb.test.Thing;

public class SquidCursorTest extends DatabaseTestCase {

    public void testTypesafeGetter() {
        StringProperty literalString = StringProperty.literal("literalString", "testStr");
        IntegerProperty literalInteger = IntegerProperty.literal(2, "testInt");
        BooleanProperty literalBoolean = BooleanProperty.literal(true, "testBool");

        // Test casting Integer to Boolean
        BooleanProperty castBool = BooleanProperty.literal(false, literalInteger.getName());

        // Test casting Boolean to Integer
        IntegerProperty castInt = IntegerProperty.literal(0, literalBoolean.getName());

        // Test casting Integer to String
        StringProperty castString = StringProperty.literal("", literalInteger.getName());

        Query query = Query.select(literalString, literalInteger, literalBoolean);
        SquidCursor<?> cursor = database.query(null, query);
        try {
            assertTrue(cursor.moveToFirst());

            assertEquals("literalString", cursor.get(literalString));
            assertEquals(2, cursor.get(literalInteger).intValue());
            assertTrue(cursor.get(literalInteger) instanceof Integer);
            assertTrue(cursor.get(literalBoolean) instanceof Boolean);

            assertTrue(cursor.get(castBool));
            assertEquals(1, cursor.get(castInt).intValue());
            assertEquals("2", cursor.get(castString));
        } finally {
            cursor.close();
        }
    }

    public void testMultiWindowCursor() {
        // Create a cursor that should contain more than 2MB of data (the default CursorWindow size)
        // to make sure that windowing is working correctly
        Thing thing = new Thing();
        int numDigits = 500;
        int numRowsToInsert = 5 * 1024 * 1024 / numDigits;

        database.beginTransaction();
        try {
            for (int i = 0; i < numRowsToInsert; i++) {
                thing.setFoo(formattedIntegerForWindowTest(numDigits, i));
                database.createNew(thing);
            }
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }

        assertEquals(numRowsToInsert, database.countAll(Thing.class));
        SquidCursor<Thing> cursor = database.query(Thing.class, Query.select(Thing.FOO));
        try {
            assertEquals(numRowsToInsert, cursor.getCount());

            // Scan cursor twice so we know it can correctly jump back to the first window
            scanCursor(cursor, numDigits);
            scanCursor(cursor, numDigits);
        } finally {
            cursor.close();
        }
    }

    private String formattedIntegerForWindowTest(int numDigits, int i) {
        return String.format("%0" + numDigits + "d", i);
    }

    private void scanCursor(SquidCursor<Thing> cursor, int numDigits) {
        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
            int i = cursor.getPosition();
            assertEquals(formattedIntegerForWindowTest(numDigits, i), cursor.get(Thing.FOO));
        }
    }

    public void testMultipleSimultaneousCursors() {
        // We had a bug in the early days of the squidb-ios module where all cursor windows
        // shared a single buffer, and they did not share it well. This test should prevent
        // that from ever happening again.
        Thing thing = new Thing();
        int numRows = 100;
        database.beginTransaction();
        try {
            for (int i = 0; i < 100; i++) {
                thing.setBar(i);
                database.createNew(thing);
            }
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }

        assertEquals(numRows, database.countAll(Thing.class));

        SquidCursor<Thing> odds = database.query(Thing.class,
                Query.select(Thing.BAR).where(Function.modulo(Thing.BAR, 2).eq(1)).orderBy(Thing.BAR.asc()));
        SquidCursor<Thing> evens = database.query(Thing.class,
                Query.select(Thing.BAR).where(Function.modulo(Thing.BAR, 2).eq(0)).orderBy(Thing.BAR.asc()));

        assertEquals(numRows / 2, odds.getCount());
        assertEquals(numRows / 2, evens.getCount());

        for (int i = 0; i < numRows; i++) {
            if (i % 2 == 0) {
                evens.moveToNext();
                assertEquals(i, evens.get(Thing.BAR).intValue());
            } else {
                odds.moveToNext();
                assertEquals(i, odds.get(Thing.BAR).intValue());
            }
        }
        odds.close();

        // Rescan evens to make sure it still has a window after the first cursor was closed
        for (int i = 0; i < evens.getCount(); i++) {
            evens.moveToPosition(i);
            assertEquals(i * 2, evens.get(Thing.BAR).intValue());
        }
        evens.close();
    }

}
