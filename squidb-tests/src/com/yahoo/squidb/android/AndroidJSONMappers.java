/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.android;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import com.yahoo.squidb.data.JSONPropertyTest;
import com.yahoo.squidb.json.JSONMapper;

import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.JavaType;

import java.lang.reflect.Type;

public class AndroidJSONMappers {

    public static final JSONPropertyTest.MapperAndCounter[] MAPPERS = {
            new JSONPropertyTest.MapperAndCounter(new JSONPropertyTest.OrgJsonMapper()),
            new JSONPropertyTest.MapperAndCounter(new GsonMapper()),
            new JSONPropertyTest.MapperAndCounter(new JacksonMapper())
    };

    private static class JacksonMapper implements JSONMapper {

        private static final ObjectMapper MAPPER = new ObjectMapper();

        static {
            MAPPER.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        }

        @Override
        public String toJSON(Object toSerialize, Type javaType) throws Exception {
            return MAPPER.writeValueAsString(toSerialize);
        }

        @Override
        public <T> T fromJSON(String jsonString, Type javaType) throws Exception {
            JavaType type = MAPPER.getTypeFactory().constructType(javaType);
            return MAPPER.readValue(jsonString, type);
        }
    }

    private static class GsonMapper implements JSONMapper {

        private static final Gson GSON = new GsonBuilder().serializeNulls().create();

        @Override
        public String toJSON(Object toSerialize, Type javaType) throws Exception {
            return GSON.toJson(toSerialize, javaType);
        }

        @Override
        public <T> T fromJSON(String jsonString, Type javaType) throws Exception {
            return GSON.fromJson(jsonString, javaType);
        }
    }

}
