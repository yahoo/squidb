package com.yahoo.squidb.sql;

import com.yahoo.squidb.test.DatabaseTestCase;
import com.yahoo.squidb.test.TestMultiColumnKey;
import com.yahoo.squidb.test.TestNonIntegerPrimaryKey;

public class PrimaryKeyTest extends DatabaseTestCase {

    public void testPrimaryKeyModelsWithRowId() {
        assertNotNull(TestMultiColumnKey.ROWID); // Really just asserting at compile time that this exists
        assertNotNull(TestNonIntegerPrimaryKey.ROWID); // Really just asserting at compile time that this exists

        assertEquals(TestMultiColumnKey.TABLE.getRowIdProperty(), TestMultiColumnKey.ROWID);
        assertEquals(TestMultiColumnKey.PROPERTIES[0], TestMultiColumnKey.ROWID);

        assertEquals(TestNonIntegerPrimaryKey.TABLE.getRowIdProperty(), TestNonIntegerPrimaryKey.ROWID);
        assertEquals(TestNonIntegerPrimaryKey.PROPERTIES[0], TestNonIntegerPrimaryKey.ROWID);
    }

    public void testNonIntegerPrimaryKey() {
        TestNonIntegerPrimaryKey test = new TestNonIntegerPrimaryKey()
                .setKey("A")
                .setValue("B");
        assertTrue(database.persist(test));
        assertEquals(1, test.getRowId());

        TestNonIntegerPrimaryKey fetched = database.fetch(TestNonIntegerPrimaryKey.class, test.getRowId());
        assertEquals(test, fetched);

        TestNonIntegerPrimaryKey fetched2 = database.fetchByCriterion(TestNonIntegerPrimaryKey.class,
                TestNonIntegerPrimaryKey.KEY.eq("A"));
        assertEquals(test, fetched2);
    }

    public void testMultiColumnPrimaryKey() {
        TestMultiColumnKey test = new TestMultiColumnKey()
                .setKeyCol1("A")
                .setKeyCol2("B")
                .setKeyCol3("C")
                .setOtherData("ABC");
        assertTrue(database.persist(test));
        assertEquals(1, test.getRowId());

        TestMultiColumnKey fetched = database.fetch(TestMultiColumnKey.class, test.getRowId());
        assertEquals(test, fetched);

        TestMultiColumnKey fetched2 = database.fetchByCriterion(TestMultiColumnKey.class,
                TestMultiColumnKey.KEY_COL_1.eq("A")
                        .and(TestMultiColumnKey.KEY_COL_2.eq("B")
                                .and(TestMultiColumnKey.KEY_COL_3.eq("C"))));
        assertEquals(test, fetched2);
    }

}
