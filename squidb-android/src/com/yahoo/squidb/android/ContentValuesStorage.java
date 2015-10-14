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
            if (values == null) {
                values = new ContentValues();
            }
            return new ContentValuesStorage(values);
        }

        @Override
        public ContentValuesStorage[] newArray(int size) {
            return new ContentValuesStorage[size];
        }
    };
}
