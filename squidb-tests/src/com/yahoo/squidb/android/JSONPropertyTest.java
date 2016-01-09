/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.android;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import com.yahoo.squidb.json.JSONMapper;
import com.yahoo.squidb.json.SquidbJSONSupport;
import com.yahoo.squidb.test.DatabaseTestCase;

import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.JavaType;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JSONPropertyTest extends DatabaseTestCase {

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
        @SuppressWarnings("unchecked")
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

    public void testListProperty() {
        for (JSONMapper mapper : MAPPERS) {
            database.clear();
            SquidbJSONSupport.setJSONMapper(mapper);
            testListPropertyInternal();
        }
    }

    private void testListPropertyInternal() {
        AndroidTestModel model = new AndroidTestModel();
        List<String> numbers = Arrays.asList("0", "1", "2", "3");
        model.setSomeList(numbers);

        database.persist(model);

        model = database.fetch(AndroidTestModel.class, model.getId(), AndroidTestModel.PROPERTIES);
        List<String> readNumbers = model.getSomeList();
        assertEquals(numbers, readNumbers);
    }

    public void testMapProperty() {
        for (JSONMapper mapper : MAPPERS) {
            database.clear();
            SquidbJSONSupport.setJSONMapper(mapper);
            testMapPropertyInternal();
        }
    }

    private void testMapPropertyInternal() {
        AndroidTestModel model = new AndroidTestModel();
        Map<String, Integer> numbers = new HashMap<String, Integer>();
        numbers.put("1", 2);
        numbers.put("2", 4);
        numbers.put("3", 6);
        numbers.put("4", 8);

        model.setSomeMap(numbers);
        database.persist(model);

        model = database.fetch(AndroidTestModel.class, model.getId(), AndroidTestModel.PROPERTIES);
        Map<String, Integer> readNumbers = model.getSomeMap();
        assertEquals(numbers, readNumbers);
    }

    public void testComplicatedMapProperty() {
        for (JSONMapper mapper : MAPPERS) {
            database.clear();
            SquidbJSONSupport.setJSONMapper(mapper);
            testComplicatedMapPropertyInternal();
        }
    }

    private void testComplicatedMapPropertyInternal() {
        AndroidTestModel model = new AndroidTestModel();

        Map<String, Map<String, List<Integer>>> crazyMap = new HashMap<String, Map<String, List<Integer>>>();

        Map<String, List<Integer>> internalMap1 = new HashMap<String, List<Integer>>();
        internalMap1.put("123", Arrays.asList(1, 2, 3));
        internalMap1.put("4567", Arrays.asList(4, 5, 6, 7));

        crazyMap.put("ABC", internalMap1);
        Map<String, List<Integer>> internalMap2 = new HashMap<String, List<Integer>>();
        internalMap2.put("XYZ", Arrays.asList(Character.getNumericValue('x'), Character.getNumericValue('y'),
                Character.getNumericValue('z')));
        internalMap2.put("Empty", new ArrayList<Integer>());
        internalMap2.put("NilValue", null);
        crazyMap.put("XYZ", internalMap2);

        model.setComplicatedMap(crazyMap);
        database.persist(model);

        model = database.fetch(AndroidTestModel.class, model.getId(), AndroidTestModel.PROPERTIES);
        Map<String, Map<String, List<Integer>>> readMap = model.getComplicatedMap();
        assertEquals(crazyMap, readMap);
    }

    public void testObjectProperty() {
        for (JSONMapper mapper : MAPPERS) {
            database.clear();
            SquidbJSONSupport.setJSONMapper(mapper);
            testObjectPropertyInternal();
        }
    }

    private void testObjectPropertyInternal() {
        AndroidTestModel model = new AndroidTestModel();
        JSONPojo pojo = new JSONPojo();
        pojo.pojoStr = "ABC";
        pojo.pojoInt = 123;
        pojo.pojoDouble = 3.1415;
        pojo.pojoList = Arrays.asList("Z", "Y", "X");

        model.setSomePojo(pojo);
        database.persist(model);

        model = database.fetch(AndroidTestModel.class, model.getId(), AndroidTestModel.PROPERTIES);
        JSONPojo readPojo = model.getSomePojo();
        assertEquals(pojo.pojoStr, readPojo.pojoStr);
        assertEquals(pojo.pojoInt, readPojo.pojoInt);
        assertEquals(pojo.pojoDouble, readPojo.pojoDouble);
        assertEquals(pojo.pojoList, readPojo.pojoList);
    }
}
