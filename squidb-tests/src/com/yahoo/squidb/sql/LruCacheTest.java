package com.yahoo.squidb.sql;

import com.yahoo.squidb.sql.CompiledArgumentResolver.SimpleLruCache;
import com.yahoo.squidb.test.SquidTestCase;

public class LruCacheTest extends SquidTestCase {

    private SimpleLruCache<String, String> cacheWithCapacity(int capacity) {
        return new SimpleLruCache<>(capacity);
    }

    private void populateCache(SimpleLruCache<String, String> cache, int times) {
        for (int i = 1; i <= times; i++) {
            cache.put(Integer.toString(i), Integer.toString(i));
        }
    }

    public void testBasicCache() {
        SimpleLruCache<String, String> cache = cacheWithCapacity(5);
        populateCache(cache, 5);
        assertEquals("1", cache.get("1"));
        assertEquals("2", cache.get("2"));
        assertEquals("3", cache.get("3"));
        assertEquals("4", cache.get("4"));
        assertEquals("5", cache.get("5"));
    }

    public void testCacheEvictsWhenOverCapacity() {
        SimpleLruCache<String, String> cache = cacheWithCapacity(5);
        populateCache(cache, 6);
        assertEquals("6", cache.get("6"));
        assertNull(cache.get("1"));
    }

    public void testCacheBehavesCorrectlyWithVariousCapacities() {
        for (int i = 1; i < 5; i++) {
            testCapacity(i);
        }
    }

    private void testCapacity(int capacity) {
        SimpleLruCache<String, String> cache = cacheWithCapacity(capacity);
        populateCache(cache, capacity + 2);
        String cap1 = Integer.toString(capacity + 1);
        String cap2 = Integer.toString(capacity + 2);
        if (capacity > 1) {
            assertEquals(cap1, cache.get(cap1));
        }
        assertEquals(cap2, cache.get(cap2));
        assertNull(cache.get("1"));
        assertNull(cache.get("2"));
    }
}
