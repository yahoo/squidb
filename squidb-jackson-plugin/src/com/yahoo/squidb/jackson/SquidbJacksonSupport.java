/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.jackson;

import com.yahoo.squidb.data.AbstractModel;
import com.yahoo.squidb.sql.Property.StringProperty;

import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.JavaType;

import java.io.IOException;

public class SquidbJacksonSupport {

    public static final ObjectMapper MAPPER = new ObjectMapper();

    static {
        MAPPER.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @SuppressWarnings("unchecked")
    /**
     * Get a Jackson-serialized string property as its real java type. Right now,
     * only {@link List} and {@link Map} are supported for serialized types.
     */
    public static <T> T getObjectValue(AbstractModel model, StringProperty property, JavaType type) {
        if (!model.hasTransitory(property.getName())) {
            T data = null;
            if (model.containsNonNullValue(property)) {
                try {
                    data = MAPPER.readValue(model.get(property), type);
                } catch (IOException e) {
                    model.clearValue(property);
                }
            }
            model.putTransitory(property.getName(), data);
            return data;
        }

        return (T) model.getTransitory(property.getName());
    }

    /**
     * Sets the given Jackson-serialized property to the given value
     *
     * @return true if the value object was successfully serialized, false otherwise
     * @see {@link #getObjectValue}
     */
    public static boolean setObjectProperty(AbstractModel model, StringProperty property, Object data) {
        try {
            String json = null;
            if (data != null) {
                json = MAPPER.writeValueAsString(data);
                if (model.containsNonNullValue(property)
                        && json.equals(model.get(property))) {
                    return false;
                }
            }
            model.set(property, json);
            model.putTransitory(property.getName(), data);
            return true;
        } catch (IOException e) {
            // leave existing values
            return false;
        }
    }

}
