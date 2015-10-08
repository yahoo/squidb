/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.data.ios;

import com.yahoo.squidb.data.TableModel;
import com.yahoo.squidb.data.ValuesStorage;

public abstract class IOSTableModel extends TableModel {

    @Override
    protected ValuesStorage newValuesStorage() {
        return new HashMapValuesStorage();
    }
}
