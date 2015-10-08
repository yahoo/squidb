/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.data.ios;

import com.yahoo.squidb.data.ValuesStorage;
import com.yahoo.squidb.data.ViewModel;

public abstract class IOSViewModel extends ViewModel {

    @Override
    protected ValuesStorage newValuesStorage() {
        return new HashMapValuesStorage();
    }
}
