/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.data.android;

import android.os.Parcel;
import android.os.Parcelable;

import com.yahoo.squidb.data.AbstractModel;

interface ParcelableModel<T extends AbstractModel> extends Parcelable {

    /**
     * Generally this method should not be called directly. It is called by {@link ModelCreator} when inflating model
     * objects
     */
    void readFromParcel(Parcel source);

    /**
     * In addition to implementing this method, parcelable model classes should create a static final variable
     * named "CREATOR" in order to satisfy the requirements of the Parcelable interface.
     */
    Parcelable.Creator<? extends T> getCreator();

}
