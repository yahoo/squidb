/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.android;

import com.yahoo.squidb.json.JSONMapper;
import com.yahoo.squidb.json.SquidbJSONSupport;
import com.yahoo.squidb.test.DatabaseTestCase;

import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.JavaType;

import java.util.Arrays;
import java.util.Collection;
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
        public <T> T fromJson(String jsonString, Class<?> baseType, Class<?>... genericArgs) throws Exception {
            JavaType type;
            if (Collection.class.isAssignableFrom(baseType)) {
                type = MAPPER.getTypeFactory().constructCollectionType(
                        (Class<? extends Collection>) baseType, genericArgs[0]);
            } else if (Map.class.isAssignableFrom(baseType)) {
                type = MAPPER.getTypeFactory().constructMapType(
                        (Class<? extends Map>) baseType, genericArgs[0], genericArgs[1]);
            } else {
                type = MAPPER.getTypeFactory().constructType(baseType);
            }
            return MAPPER.readValue(jsonString, type);
        }
    }

    static {
        SquidbJSONSupport.setJSONMapper(new JacksonMapper());
    }

    @Override
    protected void setupDatabase() {
        database = new AndroidTestDatabase();
    }

    public void testListProperty() {
        AndroidTestModel model = new AndroidTestModel();
        List<String> numbers = Arrays.asList("0", "1", "2", "3");
        model.setSomeList(numbers);

        database.persist(model);

        model = database.fetch(AndroidTestModel.class, model.getId(), AndroidTestModel.PROPERTIES);
        List<String> readNumbers = model.getSomeList();
        assertEquals(numbers.size(), readNumbers.size());
        for (int i = 0; i < numbers.size(); i++) {
            assertEquals(numbers.get(i), readNumbers.get(i));
        }
    }

    public void testMapProperty() {
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
        assertEquals(numbers.size(), readNumbers.size());
        for (int i = 1; i <= numbers.size(); i++) {
            assertEquals(numbers.get(Integer.toString(i)), readNumbers.get(Integer.toString(i)));
            assertEquals(i * 2, readNumbers.get(Integer.toString(i)).intValue());
        }
    }
}
