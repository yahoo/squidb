/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.data;

import com.yahoo.squidb.test.DatabaseTestCase;
import com.yahoo.squidb.test.TestMultiColumnUpsertable;
import com.yahoo.squidb.test.TestSingleColumnUpsertable;

public class UpsertTest extends DatabaseTestCase {

    public void testSingleColumnUpsertInsertsWhenDoesNotExist() {
        TestSingleColumnUpsertable item = upsertNewSingleColumnUpsertable();

        assertEquals(1, database.countAll(TestSingleColumnUpsertable.class));
        TestSingleColumnUpsertable fetchedItem = database.fetchByCriterion(TestSingleColumnUpsertable.class,
                TestSingleColumnUpsertable.GUID.eq("key"));
        assertEquals(item, fetchedItem);
    }

    public void testSingleColumnUpsertUpdatesWhenDoesExist() {
        upsertNewSingleColumnUpsertable();

        TestSingleColumnUpsertable upsertItem = new TestSingleColumnUpsertable();
        upsertItem.setGuid("key").setValue1("newValue1");
        database.upsert(upsertItem);
        assertTrue(upsertItem.isSaved());
        assertFalse(upsertItem.isModified());

        assertEquals(1, database.countAll(TestSingleColumnUpsertable.class));
        TestSingleColumnUpsertable fetchedItem = database.fetchByCriterion(TestSingleColumnUpsertable.class,
                TestSingleColumnUpsertable.GUID.eq("key"));
        assertNotNull(fetchedItem);
        assertEquals("newValue1", fetchedItem.getValue1());
        assertEquals("value2", fetchedItem.getValue2());
    }

    public void testSingleColumnUpsertMissingKeyThrowsException() {
        testThrowsException(new Runnable() {
            @Override
            public void run() {
                TestSingleColumnUpsertable item = new TestSingleColumnUpsertable();
                item.setValue1("value1").setValue2("value2");
                database.upsert(item);
            }
        }, IllegalStateException.class);
    }

    public void testSingleColumnUpsertWithRowidBypassesUpsert() {
        TestSingleColumnUpsertable item = upsertNewSingleColumnUpsertable();

        item.clearValue(TestSingleColumnUpsertable.GUID);
        item.setValue1("newValue1");
        database.upsert(item);
        assertTrue(item.isSaved());
        assertFalse(item.isModified());

        assertEquals(1, database.countAll(TestSingleColumnUpsertable.class));
        TestSingleColumnUpsertable fetchedItem = database.fetchByCriterion(TestSingleColumnUpsertable.class,
                TestSingleColumnUpsertable.GUID.eq("key"));
        assertNotNull(fetchedItem);
        assertEquals("newValue1", fetchedItem.getValue1());
        assertEquals("value2", fetchedItem.getValue2());
    }

    private TestSingleColumnUpsertable upsertNewSingleColumnUpsertable() {
        TestSingleColumnUpsertable item = new TestSingleColumnUpsertable();
        item.setGuid("key").setValue1("value1").setValue2("value2");
        database.upsert(item);
        assertTrue(item.isSaved());
        assertFalse(item.isModified());
        return item;
    }

    public void testMultiColumnUpsertInsertsWhenDoesNotExist() {
        TestMultiColumnUpsertable item = upsertNewMultiColumnUpsertable();

        assertEquals(1, database.countAll(TestMultiColumnUpsertable.class));
        TestMultiColumnUpsertable fetchedItem = database.fetchByCriterion(TestMultiColumnUpsertable.class,
                TestMultiColumnUpsertable.KEY_1.eq("key1").and(TestMultiColumnUpsertable.KEY_2.eq("key2")));
        assertEquals(item, fetchedItem);
    }

    public void testMultiColumnUpsertUpdatesWhenDoesExist() {
        upsertNewMultiColumnUpsertable();

        TestMultiColumnUpsertable upsertItem = new TestMultiColumnUpsertable();
        upsertItem.setKey1("key1").setKey2("key2").setValue1("newValue1");
        database.upsert(upsertItem);
        assertTrue(upsertItem.isSaved());
        assertFalse(upsertItem.isModified());

        assertEquals(1, database.countAll(TestMultiColumnUpsertable.class));
        TestMultiColumnUpsertable fetchedItem = database.fetchByCriterion(TestMultiColumnUpsertable.class,
                TestMultiColumnUpsertable.KEY_1.eq("key1").and(TestMultiColumnUpsertable.KEY_2.eq("key2")));
        assertNotNull(fetchedItem);
        assertEquals("newValue1", fetchedItem.getValue1());
        assertEquals("value2", fetchedItem.getValue2());
    }

    public void testMultiColumnUpsertMissingAnyKeyThrowsException() {
        testThrowsException(new Runnable() {
            @Override
            public void run() {
                TestMultiColumnUpsertable item = new TestMultiColumnUpsertable();
                item.setKey1("key1").setValue1("value1").setValue2("value2");
                database.upsert(item);
            }
        }, IllegalStateException.class);

        testThrowsException(new Runnable() {
            @Override
            public void run() {
                TestMultiColumnUpsertable item = new TestMultiColumnUpsertable();
                item.setKey2("key2").setValue1("value1").setValue2("value2");
                database.upsert(item);
            }
        }, IllegalStateException.class);
    }

    public void testMultiColumnUpsertWithRowidBypassesUpsert() {
        TestMultiColumnUpsertable item = upsertNewMultiColumnUpsertable();

        item.clearValue(TestMultiColumnUpsertable.KEY_1);
        item.clearValue(TestMultiColumnUpsertable.KEY_2);
        item.setValue1("newValue1");
        database.upsert(item);
        assertTrue(item.isSaved());
        assertFalse(item.isModified());

        assertEquals(1, database.countAll(TestMultiColumnUpsertable.class));
        TestMultiColumnUpsertable fetchedItem = database.fetchByCriterion(TestMultiColumnUpsertable.class,
                TestMultiColumnUpsertable.KEY_1.eq("key1").and(TestMultiColumnUpsertable.KEY_2.eq("key2")));
        assertNotNull(fetchedItem);
        assertEquals("newValue1", fetchedItem.getValue1());
        assertEquals("value2", fetchedItem.getValue2());
    }

    private TestMultiColumnUpsertable upsertNewMultiColumnUpsertable() {
        TestMultiColumnUpsertable item = new TestMultiColumnUpsertable();
        item.setKey1("key1").setKey2("key2").setValue1("value1").setValue2("value2");
        database.upsert(item);
        assertTrue(item.isSaved());
        assertFalse(item.isModified());
        return item;
    }
}
