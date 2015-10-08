/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.data.android;

import android.content.ContentValues;
import android.os.Parcel;

import com.yahoo.squidb.data.ValuesStorage;
import com.yahoo.squidb.data.ViewModel;
import com.yahoo.squidb.sql.Property;

public abstract class AndroidViewModel extends ViewModel implements ParcelableModel<AndroidViewModel> {

    @Override
    protected ValuesStorage newValuesStorage() {
        return new ContentValuesStorage();
    }

    /**
     * Copies values from the given {@link ContentValues} into the model. The values will be added to the model as read
     * values (i.e. will not be considered set values or mark the model as dirty).
     */
    public void readPropertiesFromContentValues(ContentValues values, Property<?>... properties) {
        readPropertiesFromValuesStorage(new ContentValuesStorage(values), properties);
    }

    /**
     * Analogous to {@link #readPropertiesFromContentValues(ContentValues, Property[])} but adds the values to the
     * model as set values, i.e. marks the model as dirty with these values.
     */
    public void setPropertiesFromContentValues(ContentValues values, Property<?>... properties) {
        setPropertiesFromValuesStorage(new ContentValuesStorage(values), properties);
    }

    // --- parcelable helpers

    /**
     * {@inheritDoc}
     */
    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(((ContentValuesStorage) setValues).values, 0);
        dest.writeParcelable(((ContentValuesStorage) values).values, 0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readFromParcel(Parcel source) {
        this.setValues = new ContentValuesStorage(
                (ContentValues) source.readParcelable(ContentValues.class.getClassLoader()));
        this.values = new ContentValuesStorage(
                (ContentValues) source.readParcelable(ContentValues.class.getClassLoader()));
    }
}
