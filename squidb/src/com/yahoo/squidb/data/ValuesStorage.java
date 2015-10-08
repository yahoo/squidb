/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.data;

import java.util.Map;
import java.util.Set;

public abstract class ValuesStorage {

    public abstract boolean containsKey(String key);

    public abstract Object get(String key);

    public abstract void remove(String key);

    public abstract int size();

    public abstract void putNull(String key);

    public abstract void put(String key, Boolean value);

    public abstract void put(String key, Byte value);

    public abstract void put(String key, Double value);

    public abstract void put(String key, Float value);

    public abstract void put(String key, Integer value);

    public abstract void put(String key, Long value);

    public abstract void put(String key, Short value);

    public abstract void put(String key, String value);

    public abstract void put(String key, byte[] value);

    public abstract void putAll(ValuesStorage other);

    public abstract Set<Map.Entry<String, Object>> valueSet();

    public void put(String key, Object value, boolean errorOnFail) {
        if (value == null) {
            putNull(key);
        } else if (value instanceof Boolean) {
            put(key, (Boolean) value);
        } else if (value instanceof Byte) {
            put(key, (Byte) value);
        } else if (value instanceof Double) {
            put(key, (Double) value);
        } else if (value instanceof Float) {
            put(key, (Float) value);
        } else if (value instanceof Integer) {
            put(key, (Integer) value);
        } else if (value instanceof Long) {
            put(key, (Long) value);
        } else if (value instanceof Short) {
            put(key, (Short) value);
        } else if (value instanceof String) {
            put(key, (String) value);
        } else if (value instanceof byte[]) {
            put(key, (byte[]) value);
        } else if (errorOnFail) {
            throw new UnsupportedOperationException("Could not handle type " + value.getClass());
        }
    }

    public abstract boolean equals(Object o);

    @Override
    public abstract int hashCode();
}
