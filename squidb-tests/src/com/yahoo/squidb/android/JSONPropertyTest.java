/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.android;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import com.yahoo.squidb.data.JSONTestCase;
import com.yahoo.squidb.json.JSONMapper;
import com.yahoo.squidb.json.JSONPropertySupport;
import com.yahoo.squidb.sql.Query;

import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.JavaType;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class JSONPropertyTest extends JSONTestCase {

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
        public <T> T fromJSON(String jsonString, Type javaType) throws Exception {
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
        public <T> T fromJSON(String jsonString, Type javaType) throws Exception {
            return GSON.fromJson(jsonString, javaType);
        }
    }

    private static final JSONMapper[] MAPPERS = {
            new OrgJsonMapper(),
            new JacksonMapper(),
            new GsonMapper()
    };

    @SuppressWarnings("unchecked")
    private static class OrgJsonMapper implements JSONMapper {

        @Override
        public String toJSON(Object toSerialize) throws Exception {
            Object orgJsonObject = toOrgJsonObject(toSerialize);
            return orgJsonObject == null ? null : orgJsonObject.toString();
        }

        @Override
        public <T> T fromJSON(String jsonString, Type javaType) throws Exception {
            if (jsonString == null) {
                return null;
            }
            if (jsonString.isEmpty()) {
                return (T) "";
            }
            if (javaType instanceof ParameterizedType && List.class
                    .equals(((ParameterizedType) javaType).getRawType())) {
                JSONArray array = new JSONArray(jsonString);
                return (T) deserializeArray(array, ((ParameterizedType) javaType).getActualTypeArguments()[0]);
            } else {
                JSONObject object = new JSONObject(jsonString);
                return deserializeObject(object, javaType);
            }
        }

        private <T> T deserializeObject(Object object, Type type) throws JSONException {
            if (JSONObject.NULL == object) {
                return null;
            }
            if (!(object instanceof JSONObject)) {
                return (T) object;
            }
            JSONObject jsonObject = (JSONObject) object;
            if (JSONPojo.class.equals(type)) {
                JSONPojo result = new JSONPojo();
                result.pojoInt = jsonObject.getInt("pojoInt");
                result.pojoDouble = jsonObject.getDouble("pojoDouble");
                result.pojoStr = jsonObject.getString("pojoStr");
                result.pojoList = deserializeArray(jsonObject.getJSONArray("pojoList"), Integer.class);
                return (T) result;
            } else if (type instanceof ParameterizedType && Map.class
                    .equals(((ParameterizedType) type).getRawType())) {
                return (T) deserializeMap(jsonObject, ((ParameterizedType) type).getActualTypeArguments()[1]);
            }
            throw new JSONException("Unable to parse object " + object);
        }

        private <T> Map<String, T> deserializeMap(JSONObject object, Type valueType) throws JSONException {
            Map<String, T> result = new HashMap<String, T>();
            Iterator<String> keys = object.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                Object value = object.get(key);
                if (value instanceof JSONArray) {
                    result.put(key, (T) deserializeArray((JSONArray) value,
                            ((ParameterizedType) valueType).getActualTypeArguments()[0]));
                } else {
                    result.put(key, (T) deserializeObject(object.get(key), valueType));
                }

            }
            return result;
        }

        private <T> List<T> deserializeArray(JSONArray array, Type type) throws JSONException {
            List<T> result = new ArrayList<T>();
            for (int i = 0; i < array.length(); i++) {
                result.add((T) deserializeObject(array.get(i), type));
            }
            return result;
        }

        private Object toOrgJsonObject(Object toSerialize) throws JSONException {
            if (toSerialize == null) {
                return JSONObject.NULL;
            }
            if (toSerialize instanceof Map) {
                JSONObject result = new JSONObject();
                for (Map.Entry<?, ?> entry : ((Map<?, ?>) toSerialize).entrySet()) {
                    result.put((String) entry.getKey(), toOrgJsonObject(entry.getValue()));
                }
                return result;
            } else if (toSerialize instanceof Collection) {
                return new JSONArray((Collection) toSerialize);
            } else if (toSerialize instanceof JSONPojo) {
                JSONPojo pojo = (JSONPojo) toSerialize;
                JSONObject result = new JSONObject();
                result.put("pojoStr", pojo.pojoStr);
                result.put("pojoInt", pojo.pojoInt);
                result.put("pojoDouble", pojo.pojoDouble);
                result.put("pojoList", toOrgJsonObject(pojo.pojoList));
                return result;
            } else {
                return toSerialize;
            }
        }
    }

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

    public void testListProperty() {
        testWithAllMappers(new Runnable() {
            @Override
            public void run() {
                AndroidTestModel model = new AndroidTestModel();
                List<String> numbers = Arrays.asList("0", "1", "2", "3");
                model.setSomeList(numbers);

                database.persist(model);

                model = database.fetch(AndroidTestModel.class, model.getId(), AndroidTestModel.PROPERTIES);
                List<String> readNumbers = model.getSomeList();
                assertEquals(numbers, readNumbers);
            }
        });
    }

    public void testMapProperty() {
        testWithAllMappers(new Runnable() {
            @Override
            public void run() {
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
        });
    }

    public void testComplicatedMapProperty() {
        testWithAllMappers(new Runnable() {
            @Override
            public void run() {
                AndroidTestModel model = new AndroidTestModel();

                Map<String, Map<String, List<Integer>>> crazyMap = mockComplicatedMap();

                model.setComplicatedMap(crazyMap);
                database.persist(model);

                model = database.fetch(AndroidTestModel.class, model.getId(), AndroidTestModel.PROPERTIES);
                Map<String, Map<String, List<Integer>>> readMap = model.getComplicatedMap();
                assertEquals(crazyMap, readMap);
            }
        });
    }

    public void testObjectProperty() {
        testWithAllMappers(new Runnable() {
            @Override
            public void run() {
                AndroidTestModel model = new AndroidTestModel();
                JSONPojo pojo = mockPojo();

                model.setSomePojo(pojo);
                database.persist(model);

                model = database.fetch(AndroidTestModel.class, model.getId(), AndroidTestModel.PROPERTIES);
                JSONPojo readPojo = model.getSomePojo();
                assertEquals(pojo.pojoStr, readPojo.pojoStr);
                assertEquals(pojo.pojoInt, readPojo.pojoInt);
                assertEquals(pojo.pojoDouble, readPojo.pojoDouble);
                assertEquals(pojo.pojoList, readPojo.pojoList);
            }
        });
    }

    public void testViewModelJsonProperty() {
        testWithAllMappers(new Runnable() {
            @Override
            public void run() {
                AndroidTestModel model = new AndroidTestModel();
                JSONPojo pojo = mockPojo();
                Map<String, Map<String, List<Integer>>> crazyMap = mockComplicatedMap();

                model.setSomePojo(pojo).setComplicatedMap(crazyMap);
                database.persist(model);

                AndroidTestViewModel viewModel = database.fetchByQuery(AndroidTestViewModel.class,
                        Query.select().from(AndroidTestViewModel.SUBQUERY));

                JSONPojo readPojo = viewModel.getJsonProp();
                assertEquals(pojo.pojoStr, readPojo.pojoStr);
                assertEquals(pojo.pojoInt, readPojo.pojoInt);
                assertEquals(pojo.pojoDouble, readPojo.pojoDouble);
                assertEquals(pojo.pojoList, readPojo.pojoList);

                Map<String, Map<String, List<Integer>>> readMap = viewModel.getCrazyMap();
                assertEquals(crazyMap, readMap);
            }
        });
    }

    private Map<String, Map<String, List<Integer>>> mockComplicatedMap() {
        HashMap<String, Map<String, List<Integer>>> crazyMap = new HashMap<String, Map<String, List<Integer>>>();

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
        return crazyMap;
    }

    private JSONPojo mockPojo() {
        JSONPojo pojo = new JSONPojo();
        pojo.pojoStr = "ABC";
        pojo.pojoInt = 123;
        pojo.pojoDouble = 3.1415;
        pojo.pojoList = Arrays.asList("Z", "Y", "X");
        return pojo;
    }
}
