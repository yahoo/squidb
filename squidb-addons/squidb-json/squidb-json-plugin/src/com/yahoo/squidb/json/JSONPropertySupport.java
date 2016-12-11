/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.json;

import com.yahoo.squidb.data.AbstractModel;
import com.yahoo.squidb.utility.Logger;

import java.lang.reflect.Type;

/**
 * Business logic of managing serialization and deserialization of JSON properties. Clients of this plugin should
 * be sure to call {@link #setJSONMapper(JSONMapper)} to initialize the serialization engine. See the JSONPropertyTest
 * class in the squidb tests project for example implementations of the JSONMapper interface.
 */
public class JSONPropertySupport {

    private static final String TAG = "squidb-json";

    private static JSONMapper MAPPER = null;

    public static void setJSONMapper(JSONMapper jsonMapper) {
        MAPPER = jsonMapper;
    }

    /**
     * Deserialize a JSON string property into the specified Java type
     */
    @SuppressWarnings("unchecked")
    public static <T> T getValueFromJSON(AbstractModel model, JSONProperty<T> property, Type javaType) {
        String transitoryKey = transitoryKeyForProperty(property);
        if (!model.hasTransitory(transitoryKey)) {
            T data = null;
            String json = model.get(property); // Will throw if model doesn't have property
            if (json != null) {
                try {
                    if (MAPPER == null) {
                        throw new NullPointerException("JSONPropertySupport needs to be initialized with a "
                                + "JSONMapper instance using setJSONMapper()");
                    }
                    data = MAPPER.fromJSON(json, javaType);
                } catch (Exception e) {
                    Logger.w(TAG, "Error deserializing JSON string: " + json, e);
                    model.clearValue(property);
                }
            }
            model.putTransitory(transitoryKey, data);
            return data;
        }

        return (T) model.getTransitory(transitoryKey);
    }

    /**
     * Sets the given JSON-serialized property to the given value
     *
     * @return true if the value object was successfully serialized, false otherwise
     */
    public static <T> boolean setValueAsJSON(AbstractModel model, JSONProperty<T> property, T data, Type javaType) {
        try {
            String json = null;
            if (data != null) {
                if (MAPPER == null) {
                    throw new NullPointerException("JSONPropertySupport needs to be initialized with a "
                            + "JSONMapper instance using setJSONMapper()");
                }
                json = MAPPER.toJSON(data, javaType);
                if (model.containsNonNullValue(property) && json.equals(model.get(property))) {
                    return false;
                }
            }
            model.set(property, json);
            model.putTransitory(transitoryKeyForProperty(property), data);
            return true;
        } catch (Exception e) {
            Logger.w(TAG, "Error serializing object to JSON string: " + data, e);
            return false;
        }
    }

    private static String transitoryKeyForProperty(JSONProperty<?> property) {
        return property.getName();
    }

}
