/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.data;

import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Wrapper around a {@link ValuesStorage} objects that disallows calls to methods that put or remove values
 */
public class UnmodifiableValuesStorage extends ValuesStorage {

    private final ValuesStorage values;

    public UnmodifiableValuesStorage(@Nonnull ValuesStorage values) {
        this.values = values;
    }

    @Override
    public boolean containsKey(@Nonnull String key) {
        return values.containsKey(key);
    }

    @Nullable
    @Override
    public Object get(@Nonnull String key) {
        return values.get(key);
    }

    @Override
    public void remove(@Nonnull String key) {
        throw new UnsupportedOperationException("Cannot call remove() on an ImmutableValuesStorage");
    }

    @Override
    public int size() {
        return values.size();
    }

    @Override
    public void putNull(@Nonnull String key) {
        throw new UnsupportedOperationException("Cannot call put methods on an ImmutableValuesStorage");
    }

    @Override
    public void put(@Nonnull String key, @Nullable Boolean value) {
        throw new UnsupportedOperationException("Cannot call put methods on an ImmutableValuesStorage");
    }

    @Override
    public void put(@Nonnull String key, @Nullable Byte value) {
        throw new UnsupportedOperationException("Cannot call put methods on an ImmutableValuesStorage");
    }

    @Override
    public void put(@Nonnull String key, @Nullable Double value) {
        throw new UnsupportedOperationException("Cannot call put methods on an ImmutableValuesStorage");
    }

    @Override
    public void put(@Nonnull String key, @Nullable Float value) {
        throw new UnsupportedOperationException("Cannot call put methods on an ImmutableValuesStorage");
    }

    @Override
    public void put(@Nonnull String key, @Nullable Integer value) {
        throw new UnsupportedOperationException("Cannot call put methods on an ImmutableValuesStorage");
    }

    @Override
    public void put(@Nonnull String key, @Nullable Long value) {
        throw new UnsupportedOperationException("Cannot call put methods on an ImmutableValuesStorage");
    }

    @Override
    public void put(@Nonnull String key, @Nullable Short value) {
        throw new UnsupportedOperationException("Cannot call put methods on an ImmutableValuesStorage");
    }

    @Override
    public void put(@Nonnull String key, @Nullable String value) {
        throw new UnsupportedOperationException("Cannot call put methods on an ImmutableValuesStorage");
    }

    @Override
    public void put(@Nonnull String key, @Nullable byte[] value) {
        throw new UnsupportedOperationException("Cannot call put methods on an ImmutableValuesStorage");
    }

    @Override
    public void putAll(@Nullable ValuesStorage other) {
        throw new UnsupportedOperationException("Cannot call putAll() on an ImmutableValuesStorage");
    }

    @Nonnull
    @Override
    public Set<Map.Entry<String, Object>> valueSet() {
        return values.valueSet();
    }

    @Nonnull
    @Override
    public Set<String> keySet() {
        return values.keySet();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("Cannot call clear() on an ImmutableValuesStorage");
    }

    @Override
    public boolean equals(@Nullable Object o) {
        return o instanceof ValuesStorage && values.equals(o);
    }

    @Override
    public int hashCode() {
        return values.hashCode();
    }
}
