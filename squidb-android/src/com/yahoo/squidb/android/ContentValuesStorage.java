/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.android;

import android.content.ContentValues;
import android.os.Parcel;
import android.os.Parcelable;

import com.yahoo.squidb.data.ValuesStorage;

import java.util.Map;
import java.util.Set;

/**
 * Implementation of {@link ValuesStorage} that stores its values using {@link ContentValues}
 */
public class ContentValuesStorage extends ValuesStorage implements Parcelable {

    private final ContentValues values;

    /**
     * Construct an empty ContentValuesStorage
     */
    public ContentValuesStorage() {
        this.values = new ContentValues();
    }

    /**
     * Construct a ContentValuesStorage populated with a copy of the values from the given ContentValues.
     */
    public ContentValuesStorage(ContentValues values) {
        this(values, true);
    }

    /**
     * Constructor that allows initializing the object with a reference to the given values rather than copying them.
     * This is an optimization for short-lived objects (e.g. for read/setPropertiesFromContentValues) or for inflating
     * this object from a parcel, when copying is not necessary
     */
    ContentValuesStorage(ContentValues values, boolean copyValues) {
        if (values != null && !copyValues) {
            this.values = values;
        } else {
            this.values = new ContentValues();
            if (values != null) {
                this.values.putAll(values);
            }
        }
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
        values.putNull(key);
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
        if (other instanceof ContentValuesStorage) {
            values.putAll(((ContentValuesStorage) other).values);
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
    public void clear() {
        values.clear();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<Map.Entry<String, Object>> valueSet() {
        return values.valueSet();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<String> keySet() {
        return values.keySet();
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

    @Override
    public int describeContents() {
        return values.describeContents();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(values, flags);
    }

    public static final Parcelable.Creator<ContentValuesStorage> CREATOR = new Creator<ContentValuesStorage>() {
        @Override
        public ContentValuesStorage createFromParcel(Parcel source) {
            ContentValues values = source.readParcelable(ContentValues.class.getClassLoader());
            return new ContentValuesStorage(values, false);
        }

        @Override
        public ContentValuesStorage[] newArray(int size) {
            return new ContentValuesStorage[size];
        }
    };
}
