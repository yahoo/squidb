/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.android;

import com.yahoo.squidb.test.DatabaseTestCase;

import java.util.Arrays;
import java.util.List;

public class JacksonPropertyTest extends DatabaseTestCase {

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

}
