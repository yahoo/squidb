/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.data.ios;

import com.yahoo.squidb.data.ValuesStorage;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class HashMapValuesStorage extends ValuesStorage {

    final Map<String, Object> values = new HashMap<String, Object>();

    @Override
    public boolean containsKey(String key) {
        return values.containsKey(key);
    }

    @Override
    public Object get(String key) {
        return values.get(key);
    }

    @Override
    public void remove(String key) {
        values.remove(key);
    }

    @Override
    public int size() {
        return values.size();
    }

    @Override
    public void putNull(String key) {
        values.put(key, null);
    }

    @Override
    public void put(String key, Boolean value) {
        values.put(key, value);
    }

    @Override
    public void put(String key, Byte value) {
        values.put(key, value);
    }

    @Override
    public void put(String key, Double value) {
        values.put(key, value);
    }

    @Override
    public void put(String key, Float value) {
        values.put(key, value);
    }

    @Override
    public void put(String key, Integer value) {
        values.put(key, value);
    }

    @Override
    public void put(String key, Long value) {
        values.put(key, value);
    }

    @Override
    public void put(String key, Short value) {
        values.put(key, value);
    }

    @Override
    public void put(String key, String value) {
        values.put(key, value);
    }

    @Override
    public void put(String key, byte[] value) {
        values.put(key, value);
    }

    @Override
    public void putAll(ValuesStorage other) {
        if (other instanceof HashMapValuesStorage) {
            values.putAll(((HashMapValuesStorage) other).values);
        } else {
            Set<Map.Entry<String, Object>> valuesSet = other.valuesSet();
            for (Map.Entry<String, Object> entry : valuesSet) {
                put(entry.getKey(), entry.getValue(), false);
            }
        }
    }

    @Override
    public Set<Map.Entry<String, Object>> valuesSet() {
        return values.entrySet();
    }
}
