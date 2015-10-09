/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.data.ios;

import com.yahoo.squidb.data.ValuesStorage;
import com.yahoo.squidb.data.ViewModel;
import com.yahoo.squidb.sql.Property;

import java.util.Map;

public abstract class IOSViewModel extends ViewModel {

    @Override
    protected ValuesStorage newValuesStorage() {
        return new MapValuesStorage();
    }

    /**
     * Copies values from the given Map. The values will be added to the model as read values (i.e. will not be
     * considered set values or mark the model as dirty).
     */
    public void readPropertiesFromMap(Map<String, Object> values, Property<?>... properties) {
        readPropertiesFromValuesStorage(new MapValuesStorage(values), properties);
    }

    /**
     * Analogous to {@link #readPropertiesFromMap(Map, Property[])} but adds the values to the model as set values,
     * i.e. marks the model as dirty with these values.
     */
    public void setPropertiesFromMap(Map<String, Object> values, Property<?>... properties) {
        setPropertiesFromValuesStorage(new MapValuesStorage(values), properties);
    }
}
