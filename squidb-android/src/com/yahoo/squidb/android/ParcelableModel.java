/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.android;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Extension of Parcelable interface that adds a method for reading data from a Parcel
 */
public interface ParcelableModel extends Parcelable {

    void readFromParcel(Parcel source);
}
