/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.data.android;

import android.content.ContentValues;

import com.yahoo.squidb.data.ValuesStorage;

import java.util.Map;
import java.util.Set;

public class ContentValuesStorage extends ValuesStorage {

    final ContentValues values;

    public ContentValuesStorage() {
        this.values = new ContentValues();
    }

    public ContentValuesStorage(ContentValues values) {
        if (values == null) {
            throw new IllegalArgumentException("Can't create a ContentValuesStorage with null ContentValues");
        }
        this.values = values;
    }

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
        values.putNull(key);
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
        if (other instanceof ContentValuesStorage) {
            values.putAll(((ContentValuesStorage) other).values);
        } else {
            Set<Map.Entry<String, Object>> valuesSet = other.valueSet();
            for (Map.Entry<String, Object> entry : valuesSet) {
                put(entry.getKey(), entry.getValue(), false);
            }

        }
    }

    @Override
    public Set<Map.Entry<String, Object>> valueSet() {
        return values.valueSet();
    }

    @Override
    public boolean equals(Object o) {
        return (o instanceof ContentValuesStorage) &&
                values.equals(((ContentValuesStorage) o).values);
    }

    @Override
    public int hashCode() {
        return values.hashCode();
    }
}
