/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.android;

import android.content.ContentValues;
import android.os.Parcel;
import android.os.Parcelable;

import com.yahoo.squidb.data.TableModel;
import com.yahoo.squidb.data.ValuesStorage;
import com.yahoo.squidb.sql.Property;
import com.yahoo.squidb.utility.Logger;

public abstract class AndroidTableModel extends TableModel implements Parcelable {

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
        dest.writeParcelable((ContentValuesStorage) setValues, 0);
        dest.writeParcelable((ContentValuesStorage) values, 0);
    }

    @Override
    public void readFromParcel(Object source) {
        if (!(source instanceof Parcel)) {
            Logger.w(Logger.LOG_TAG, "readFromParcel called with non-Parcel argument", new Throwable());
            return;
        }
        Parcel parcel = (Parcel) source;
        this.setValues = parcel.readParcelable(ContentValuesStorage.class.getClassLoader());
        this.values = parcel.readParcelable(ContentValuesStorage.class.getClassLoader());
    }
}
