/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.sql;

import com.yahoo.squidb.test.DatabaseTestCase;
import com.yahoo.squidb.test.TestModel;

import java.util.ArrayList;
import java.util.List;

public class FieldTest extends DatabaseTestCase {

    public void testInCriterionWithEmptyListExecutesValidSql() {
        List<Long> values = null;
        dao.query(TestModel.class, Query.select().where(TestModel.ID.in(values)));
        values = new ArrayList<Long>();
        dao.query(TestModel.class, Query.select().where(TestModel.ID.in(values)));
    }

}
