/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.json;

import com.yahoo.squidb.data.AbstractModel;
import com.yahoo.squidb.sql.SqlUtils;
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

    private static class JSONObjectHolder<T> {
        final T parsedObject;
        final String jsonString;

        JSONObjectHolder(T parsedObject, String jsonString) {
            this.parsedObject = parsedObject;
            this.jsonString = jsonString;
        }
    }

    /**
     * Deserialize a JSON string property into the specified Java type
     */
    public static <T> T getValueFromJSON(AbstractModel model, JSONProperty<T> property, Type javaType) {
        String transitoryKey = transitoryKeyForProperty(property);
        checkCacheIntegrity(model, property, transitoryKey);

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
                    // TODO: Should this throw or at least not cache null?
                    Logger.w(TAG, "Error deserializing JSON string: " + json, e);
                    model.clearValue(property);
                }
            }
            putJSONTransitory(model, transitoryKey, data, json);
            return data;
        }

        JSONObjectHolder<T> holder = getJSONTransitory(model, transitoryKey);
        return holder.parsedObject;
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
            putJSONTransitory(model, transitoryKeyForProperty(property), data, json);
            return true;
        } catch (Exception e) {
            Logger.w(TAG, "Error serializing object to JSON string: " + data, e);
            // TODO: Should this throw?
            return false;
        }
    }

    private static String transitoryKeyForProperty(JSONProperty<?> property) {
        return "json__" + property.getName();
    }

    private static <T> void putJSONTransitory(AbstractModel model, String transitoryKey, T data, String jsonString) {
        model.putTransitory(transitoryKey, new JSONObjectHolder<>(data, jsonString));
    }

    // We need to be able to check that our cached value is still correct, because some methods like
    // model.clear(property) aren't able to clear associated transitory values
    private static void checkCacheIntegrity(AbstractModel model, JSONProperty<?> property, String transitoryKey) {
        if (!model.hasTransitory(transitoryKey)) {
            return;
        }
        JSONObjectHolder<?> holder = getJSONTransitory(model, transitoryKey);
        if (model.containsValue(property) || model.getDefaultValues().containsKey(property.getName())) {
            String jsonValue = model.get(property);
            if (SqlUtils.equals(holder.jsonString, jsonValue)) {
                return;
            }
        }
        model.clearTransitory(transitoryKey);
    }

    @SuppressWarnings("unchecked")
    private static <T> JSONObjectHolder<T> getJSONTransitory(AbstractModel model, String transitoryKey) {
        return (JSONObjectHolder<T>) model.getTransitory(transitoryKey);
    }

}
