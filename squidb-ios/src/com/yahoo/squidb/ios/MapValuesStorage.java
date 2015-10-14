/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.ios;

import com.yahoo.squidb.data.ValuesStorage;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Implementation of {@link ValuesStorage} that stores its values using a {@link Map}
 */
public class MapValuesStorage extends ValuesStorage {

    final Map<String, Object> values;

    public MapValuesStorage() {
        this.values = new HashMap<String, Object>();
    }

    public MapValuesStorage(Map<String, Object> values) {
        if (values == null) {
            throw new IllegalArgumentException("Can't create a MapValuesStorage with null Map");
        }
        this.values = values;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean containsKey(String key) {
        return values.containsKey(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object get(String key) {
        return values.get(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void remove(String key) {
        values.remove(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int size() {
        return values.size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void putNull(String key) {
        values.put(key, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void put(String key, Boolean value) {
        values.put(key, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void put(String key, Byte value) {
        values.put(key, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void put(String key, Double value) {
        values.put(key, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void put(String key, Float value) {
        values.put(key, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void put(String key, Integer value) {
        values.put(key, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void put(String key, Long value) {
        values.put(key, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void put(String key, Short value) {
        values.put(key, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void put(String key, String value) {
        values.put(key, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void put(String key, byte[] value) {
        values.put(key, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void putAll(ValuesStorage other) {
        if (other instanceof MapValuesStorage) {
            values.putAll(((MapValuesStorage) other).values);
        } else {
            Set<Map.Entry<String, Object>> valuesSet = other.valueSet();
            for (Map.Entry<String, Object> entry : valuesSet) {
                put(entry.getKey(), entry.getValue(), false);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<Map.Entry<String, Object>> valueSet() {
        return values.entrySet();
    }

    @Override
    public boolean equals(Object o) {
        return (o instanceof MapValuesStorage) &&
                values.equals(((MapValuesStorage) o).values);
    }

    @Override
    public int hashCode() {
        return values.hashCode();
    }
}
