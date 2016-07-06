/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.data;

import com.yahoo.squidb.test.BasicData;
import com.yahoo.squidb.test.DatabaseTestCase;
import com.yahoo.squidb.test.SpecificData;
import com.yahoo.squidb.test.TestEnum;

public class InheritanceModelTest extends DatabaseTestCase {

    public void testModelInheritance() {
        BasicData data = new BasicData();
        data.setData1("1").setData2("2").setData3("3").setSomeEnum(TestEnum.BANANA);
        database.persist(data);

        SpecificData sd = database.fetch(SpecificData.class, data.getRowId(), SpecificData.PROPERTIES);
        assertNotNull(sd);
        assertEquals("1", sd.getFirstName());
        assertEquals("2", sd.getLastName());
        assertEquals("3", sd.getAddress());
        assertEquals(TestEnum.BANANA, sd.getMyEnum());
    }

}
