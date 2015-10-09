/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.data;

import java.util.Map;
import java.util.Set;

/**
 * ValuesStorage is an abstract class for wrapping a key-value storage object. The wrapped object could be a
 * ContentValues (which works on Android and is good for implementing Parcelable) or it could be a simple HashMap
 * (which works on iOS with j2objc). The interface is modeled after the ContentValues interface.
 */
public abstract class ValuesStorage {

    /**
     * @return true if the object contains a value for the given key, false otherwise
     */
    public abstract boolean containsKey(String key);

    /**
     * @return the value mapped to this key, or null if one does not exist. May return null if the value stored is null
     */
    public abstract Object get(String key);

    /**
     * Remove the value with the given key, if it exists
     */
    public abstract void remove(String key);

    /**
     * @return the number of key-value pairs in this ValuesStorage
     */
    public abstract int size();

    /**
     * Adds a null value to the set.
     *
     * @param key the name of the value to put
     */
    public abstract void putNull(String key);

    /**
     * Adds a value to the set.
     *
     * @param key the name of the value to put
     * @param value the data for the value to put
     */
    public abstract void put(String key, Boolean value);

    /**
     * Adds a value to the set.
     *
     * @param key the name of the value to put
     * @param value the data for the value to put
     */
    public abstract void put(String key, Byte value);

    /**
     * Adds a value to the set.
     *
     * @param key the name of the value to put
     * @param value the data for the value to put
     */
    public abstract void put(String key, Double value);

    /**
     * Adds a value to the set.
     *
     * @param key the name of the value to put
     * @param value the data for the value to put
     */
    public abstract void put(String key, Float value);

    /**
     * Adds a value to the set.
     *
     * @param key the name of the value to put
     * @param value the data for the value to put
     */
    public abstract void put(String key, Integer value);

    /**
     * Adds a value to the set.
     *
     * @param key the name of the value to put
     * @param value the data for the value to put
     */
    public abstract void put(String key, Long value);

    /**
     * Adds a value to the set.
     *
     * @param key the name of the value to put
     * @param value the data for the value to put
     */
    public abstract void put(String key, Short value);

    /**
     * Adds a value to the set.
     *
     * @param key the name of the value to put
     * @param value the data for the value to put
     */
    public abstract void put(String key, String value);

    /**
     * Adds a value to the set.
     *
     * @param key the name of the value to put
     * @param value the data for the value to put
     */
    public abstract void put(String key, byte[] value);

    /**
     * Adds all values from the passed in ValuesStorage.
     *
     * @param other the ValuesStorage from which to copy
     */
    public abstract void putAll(ValuesStorage other);

    /**
     * @return a set of all of the keys and values in the values storage
     */
    public abstract Set<Map.Entry<String, Object>> valueSet();

    /**
     * Add a value of unknown type to the set if it is one of the accepted types. Accepted types are any primitive
     * (Integer, Boolean, etc.), String, and byte[].
     *
     * @param key the name of the value to put
     * @param value the data for the value to put
     * @param errorOnFail pass true if this method should throw an exception if the value was not one of the accepted
     * types, or pass false to fail silently
     */
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
