/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.android;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import com.yahoo.squidb.json.JSONFunctions;
import com.yahoo.squidb.json.JSONMapper;
import com.yahoo.squidb.json.JSONPropertySupport;
import com.yahoo.squidb.test.DatabaseTestCase;

import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.JavaType;

import java.lang.reflect.Type;

public class JSONTestCase extends DatabaseTestCase {

    private static class JacksonMapper implements JSONMapper {

        private static final ObjectMapper MAPPER = new ObjectMapper();

        static {
            MAPPER.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        }

        @Override
        public String toJSON(Object toSerialize) throws Exception {
            return MAPPER.writeValueAsString(toSerialize);
        }

        @Override
        public <T> T fromJson(String jsonString, Type javaType) throws Exception {
            JavaType type = MAPPER.getTypeFactory().constructType(javaType);
            return MAPPER.readValue(jsonString, type);
        }
    }

    private static class GsonMapper implements JSONMapper {

        private static final Gson GSON = new GsonBuilder().serializeNulls().create();

        @Override
        public String toJSON(Object toSerialize) throws Exception {
            return GSON.toJson(toSerialize);
        }

        @Override
        public <T> T fromJson(String jsonString, Type javaType) throws Exception {
            return GSON.fromJson(jsonString, javaType);
        }
    }

    private static final JSONMapper[] MAPPERS = {
            new JacksonMapper(),
            new GsonMapper()
    };

    @Override
    protected void setupDatabase() {
        database = new AndroidTestDatabase();
    }

    protected void testWithAllMappers(Runnable toTest) {
        for (JSONMapper mapper : MAPPERS) {
            database.clear();
            JSONPropertySupport.setJSONMapper(mapper);
            toTest.run();
        }
    }

    protected void testForMinVersionCode(Runnable toTest) {
        if (database.getSqliteVersion().isAtLeast(JSONFunctions.JSON1_MIN_VERSION)) {
            toTest.run();
        }
    }
}
