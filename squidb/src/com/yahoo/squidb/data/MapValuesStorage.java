/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.data;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Implementation of {@link ValuesStorage} that stores its values using a {@link Map}
 */
public class MapValuesStorage extends ValuesStorage {

    private final Map<String, Object> values = new HashMap<>();

    /**
     * Construct an empty MapValuesStorage
     */
    public MapValuesStorage() {
    }

    /**
     * Construct a MapValuesStorage populated with a copy of the values from the given map. This may throw an
     * IllegalArgumentException if any of the values in the map are of an unsupported type (i.e. not
     * a String, primitive, or byte[])
     */
    public MapValuesStorage(@Nullable Map<String, ?> values) {
        if (values != null) {
            for (Map.Entry<String, ?> entry : values.entrySet()) {
                String key = entry.getKey();
                if (key != null) {
                    put(entry.getKey(), entry.getValue(), true);
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean containsKey(@Nonnull String key) {
        return values.containsKey(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Nullable
    public Object get(@Nonnull String key) {
        return values.get(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void remove(@Nonnull String key) {
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
    public void putNull(@Nonnull String key) {
        values.put(key, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void put(@Nonnull String key, @Nullable Boolean value) {
        values.put(key, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void put(@Nonnull String key, @Nullable Byte value) {
        values.put(key, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void put(@Nonnull String key, @Nullable Double value) {
        values.put(key, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void put(@Nonnull String key, @Nullable Float value) {
        values.put(key, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void put(@Nonnull String key, @Nullable Integer value) {
        values.put(key, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void put(@Nonnull String key, @Nullable Long value) {
        values.put(key, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void put(@Nonnull String key, @Nullable Short value) {
        values.put(key, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void put(@Nonnull String key, @Nullable String value) {
        values.put(key, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void put(@Nonnull String key, @Nullable byte[] value) {
        values.put(key, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void putAll(@Nullable ValuesStorage other) {
        if (other != null) {
            if (other instanceof MapValuesStorage) {
                values.putAll(((MapValuesStorage) other).values);
            } else {
                Set<Map.Entry<String, Object>> valuesSet = other.valueSet();
                for (Map.Entry<String, Object> entry : valuesSet) {
                    put(entry.getKey(), entry.getValue(), false);
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear() {
        values.clear();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Nonnull
    public Set<Map.Entry<String, Object>> valueSet() {
        return values.entrySet();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Nonnull
    public Set<String> keySet() {
        return values.keySet();
    }

    @Override
    public boolean equals(@Nullable Object o) {
        return (o instanceof MapValuesStorage) &&
                values.equals(((MapValuesStorage) o).values);
    }

    @Override
    public int hashCode() {
        return values.hashCode();
    }
}
