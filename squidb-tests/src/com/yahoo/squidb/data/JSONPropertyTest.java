/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.data;

import com.yahoo.squidb.json.JSONMapper;
import com.yahoo.squidb.json.JSONPropertySupport;
import com.yahoo.squidb.sql.Property;
import com.yahoo.squidb.sql.Query;
import com.yahoo.squidb.test.DatabaseTestCase;
import com.yahoo.squidb.test.Employee;
import com.yahoo.squidb.test.TestModel;
import com.yahoo.squidb.test.TestViewModel;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class JSONPropertyTest extends DatabaseTestCase {

    public static class MapperAndCounter implements JSONMapper {
        final JSONMapper mapper;
        int toJSONCount = 0;
        int fromJSONCount = 0;

        public MapperAndCounter(JSONMapper mapper) {
            this.mapper = mapper;
        }

        @Override
        public String toJSON(Object toSerialize, Type javaType) throws Exception {
            toJSONCount++;
            return mapper.toJSON(toSerialize, javaType);
        }

        @Override
        public <T> T fromJSON(String jsonString, Type javaType) throws Exception {
            fromJSONCount++;
            return mapper.fromJSON(jsonString, javaType);
        }
    }

    private static MapperAndCounter currentMapper;

    public static MapperAndCounter[] MAPPERS = {
            new MapperAndCounter(new OrgJsonMapper())
    };

    @SuppressWarnings("unchecked")
    public static class OrgJsonMapper implements JSONMapper {
        // Note: This is not a good example of a JSONMapper implementation. It works ok for Lists, Maps, and our
        // JSONPojo test object, but that's pretty much all. It's hacky, but it works with iOS/j2objc for verifying
        // JSONProperty behavior in our unit tests. One could imagine writing a more general JSONMapper that used
        // the org.json classes using code generation or something, but this is enough for our purposes.

        @Override
        public String toJSON(Object toSerialize, Type javaType) throws Exception {
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
            Map<String, T> result = new HashMap<>();
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
            List<T> result = new ArrayList<>();
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

    protected void testWithAllMappers(Runnable toTest) {
        for (MapperAndCounter mapper : MAPPERS) {
            mapper.fromJSONCount = 0;
            mapper.toJSONCount = 0;
            currentMapper = mapper;
            database.clear();
            JSONPropertySupport.setJSONMapper(mapper);
            toTest.run();
        }
    }

    private String transitoryKeyForProperty(Property<?> property) {
        return "json__" + property.getName();
    }

    public void testListProperty() {
        testWithAllMappers(new Runnable() {
            @Override
            public void run() {
                TestModel model = new TestModel();
                List<String> numbers = Arrays.asList("0", "1", "2", "3");
                model.setSomeList(numbers);

                database.persist(model);

                model = database.fetch(TestModel.class, model.getRowId(), TestModel.PROPERTIES);
                int currentFromJSONCount = currentMapper.fromJSONCount;
                assertFalse(model.hasTransitory(transitoryKeyForProperty(TestModel.SOME_LIST)));
                assertEquals(numbers, model.getSomeList());
                assertEquals(currentFromJSONCount + 1, currentMapper.fromJSONCount);
                // Second call reads from cache; assert that works too
                assertTrue(model.hasTransitory(transitoryKeyForProperty(TestModel.SOME_LIST)));
                assertEquals(numbers, model.getSomeList());
                assertEquals(currentFromJSONCount + 1, currentMapper.fromJSONCount);
            }
        });
    }

    public void testMapProperty() {
        testWithAllMappers(new Runnable() {
            @Override
            public void run() {
                TestModel model = new TestModel();
                Map<String, Integer> numbers = new HashMap<>();
                numbers.put("1", 2);
                numbers.put("2", 4);
                numbers.put("3", 6);
                numbers.put("4", 8);

                model.setSomeMap(numbers);
                database.persist(model);

                model = database.fetch(TestModel.class, model.getRowId(), TestModel.PROPERTIES);
                int currentFromJSONCount = currentMapper.fromJSONCount;
                assertFalse(model.hasTransitory(transitoryKeyForProperty(TestModel.SOME_MAP)));
                assertEquals(numbers, model.getSomeMap());
                assertEquals(currentFromJSONCount + 1, currentMapper.fromJSONCount);
                // Second call reads from cache; assert that works too
                assertTrue(model.hasTransitory(transitoryKeyForProperty(TestModel.SOME_MAP)));
                assertEquals(numbers, model.getSomeMap());
                assertEquals(currentFromJSONCount + 1, currentMapper.fromJSONCount);
            }
        });
    }

    public void testComplicatedMapProperty() {
        testWithAllMappers(new Runnable() {
            @Override
            public void run() {
                TestModel model = new TestModel();

                Map<String, Map<String, List<Integer>>> crazyMap = mockComplicatedMap();

                model.setComplicatedMap(crazyMap);
                database.persist(model);

                model = database.fetch(TestModel.class, model.getRowId(), TestModel.PROPERTIES);
                int currentFromJSONCount = currentMapper.fromJSONCount;
                assertFalse(model.hasTransitory(transitoryKeyForProperty(TestModel.COMPLICATED_MAP)));
                assertEquals(crazyMap, model.getComplicatedMap());
                assertEquals(currentFromJSONCount + 1, currentMapper.fromJSONCount);
                // Second call reads from cache; assert that works too
                assertTrue(model.hasTransitory(transitoryKeyForProperty(TestModel.COMPLICATED_MAP)));
                assertEquals(crazyMap, model.getComplicatedMap());
                assertEquals(currentFromJSONCount + 1, currentMapper.fromJSONCount);
            }
        });
    }

    public void testObjectProperty() {
        testWithAllMappers(new Runnable() {
            @Override
            public void run() {
                TestModel model = new TestModel();
                JSONPojo pojo = mockPojo();

                model.setSomePojo(pojo);
                database.persist(model);

                model = database.fetch(TestModel.class, model.getRowId(), TestModel.PROPERTIES);
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
                TestModel model = new TestModel();
                JSONPojo pojo = mockPojo();
                Map<String, Map<String, List<Integer>>> crazyMap = mockComplicatedMap();

                model.setSomePojo(pojo).setComplicatedMap(crazyMap);
                database.persist(model);

                Employee forView = new Employee().setName("A");
                database.persist(forView);

                TestViewModel viewModel = database.fetchByQuery(TestViewModel.class,
                        Query.select().from(TestViewModel.VIEW));

                JSONPojo readPojo = viewModel.getJsonProp();
                assertEquals(pojo.pojoStr, readPojo.pojoStr);
                assertEquals(pojo.pojoInt, readPojo.pojoInt);
                assertEquals(pojo.pojoDouble, readPojo.pojoDouble);
                assertEquals(pojo.pojoList, readPojo.pojoList);

                int currentFromJSONCount = currentMapper.fromJSONCount;
                assertFalse(viewModel.hasTransitory(transitoryKeyForProperty(TestViewModel.CRAZY_MAP)));
                assertEquals(crazyMap, viewModel.getCrazyMap());
                assertEquals(currentFromJSONCount + 1, currentMapper.fromJSONCount);
                // Second call reads from cache; assert that works too
                assertTrue(viewModel.hasTransitory(transitoryKeyForProperty(TestViewModel.CRAZY_MAP)));
                assertEquals(crazyMap, viewModel.getCrazyMap());
                assertEquals(currentFromJSONCount + 1, currentMapper.fromJSONCount);
            }
        });
    }

    public void testTransitoryCacheClearedWhenModelCleared() {
        testWithAllMappers(new Runnable() {
            @Override
            public void run() {
                TestModel model = new TestModel();
                List<String> list = Arrays.asList("A", "B", "C");
                model.setSomeList(list);
                model.clear();
                // Empty list is default; if cache not cleared comparison will fail
                assertEquals(Collections.emptyList(), model.getSomeList());

                // Test that we can catch cache invalidation even when transitory values are not cleared
                model.setSomeList(list);
                model.clearValue(TestModel.SOME_LIST);
                int currentFromJSONCount = currentMapper.fromJSONCount;
                assertEquals(Collections.emptyList(), model.getSomeList());
                assertEquals(currentFromJSONCount + 1, currentMapper.fromJSONCount);
                // Second call reads from cache; assert that works too
                assertTrue(model.hasTransitory(transitoryKeyForProperty(TestModel.SOME_LIST)));
                assertEquals(Collections.emptyList(), model.getSomeList());
                assertEquals(currentFromJSONCount + 1, currentMapper.fromJSONCount);
            }
        });
    }

    public void testTransitoryCacheClearedWhenModelRepopulated() {
        testWithAllMappers(new Runnable() {
            @Override
            public void run() {
                TestModel model = new TestModel();
                List<String> list = Arrays.asList("A", "B", "C");
                model.setSomeList(list);

                ValuesStorage newStorage = new MapValuesStorage();
                newStorage.put(TestModel.SOME_LIST.getName(), "[\"D\", \"E\", \"F\"]");
                model.readPropertiesFromValuesStorage(newStorage, TestModel.SOME_LIST);
                assertEquals(Arrays.asList("D", "E", "F"), model.getSomeList());
                // Second call reads from cache; assert that works too
                assertTrue(model.hasTransitory(transitoryKeyForProperty(TestModel.SOME_LIST)));
                assertEquals(Arrays.asList("D", "E", "F"), model.getSomeList());

                newStorage.put(TestModel.SOME_LIST.getName(), "[\"H\", \"I\", \"J\"]");
                model.setPropertiesFromValuesStorage(newStorage, TestModel.SOME_LIST);
                int currentFromJSONCount = currentMapper.fromJSONCount;
                assertEquals(Arrays.asList("H", "I", "J"), model.getSomeList());
                assertEquals(currentFromJSONCount + 1, currentMapper.fromJSONCount);
                // Second call reads from cache; assert that works too
                assertTrue(model.hasTransitory(transitoryKeyForProperty(TestModel.SOME_LIST)));
                assertEquals(Arrays.asList("H", "I", "J"), model.getSomeList());
                assertEquals(currentFromJSONCount + 1, currentMapper.fromJSONCount);
            }
        });
    }

    public void testGetterThrowsIfJSONPropertyNotPresent() {
        testWithAllMappers(new Runnable() {
            @Override
            public void run() {
                final TestModel model = new TestModel();
                testThrowsException(new Runnable() {
                    @Override
                    public void run() {
                        model.getSomeMap(); // This property has no default
                    }
                }, UnsupportedOperationException.class);
                int currentFromJSONCount = currentMapper.fromJSONCount;
                assertFalse(model.hasTransitory(transitoryKeyForProperty(TestModel.SOME_LIST)));
                assertEquals(Collections.emptyList(), model.getSomeList()); // This property has a default
                assertEquals(currentFromJSONCount + 1, currentMapper.fromJSONCount);
                // Second call reads from cache; assert that works too
                assertTrue(model.hasTransitory(transitoryKeyForProperty(TestModel.SOME_LIST)));
                assertEquals(Collections.emptyList(), model.getSomeList()); // This property has a default
                assertEquals(currentFromJSONCount + 1, currentMapper.fromJSONCount);
            }
        });
    }

    private Map<String, Map<String, List<Integer>>> mockComplicatedMap() {
        HashMap<String, Map<String, List<Integer>>> crazyMap = new HashMap<>();

        Map<String, List<Integer>> internalMap1 = new HashMap<>();
        internalMap1.put("123", Arrays.asList(1, 2, 3));
        internalMap1.put("4567", Arrays.asList(4, 5, 6, 7));

        crazyMap.put("ABC", internalMap1);
        Map<String, List<Integer>> internalMap2 = new HashMap<>();
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
