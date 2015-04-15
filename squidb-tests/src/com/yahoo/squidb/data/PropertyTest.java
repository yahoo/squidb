/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.data;

import com.yahoo.squidb.sql.Property.LongProperty;
import com.yahoo.squidb.sql.Query;
import com.yahoo.squidb.test.SquidTestCase;
import com.yahoo.squidb.test.TestModel;
import com.yahoo.squidb.test.TestViewModel;

public class PropertyTest extends SquidTestCase {

    public void testPropertyAliasing() {
        LongProperty p = TestModel.ID;

        LongProperty basicAlias = p.as("newAlias");
        assertEquals(p.table, basicAlias.table);
        assertEquals(p.getExpression(), basicAlias.getExpression());
        assertEquals("newAlias", basicAlias.getName());
        assertEquals("SELECT testModels._id AS newAlias", Query.select(basicAlias).toString());

        LongProperty aliasWithTable = p.as("newTable", "newAlias");
        assertEquals("newTable", aliasWithTable.table.getName());
        assertEquals(p.getExpression(), aliasWithTable.getExpression());
        assertEquals("newAlias", aliasWithTable.getName());
        assertEquals("SELECT newTable._id AS newAlias", Query.select(aliasWithTable).toString());

        LongProperty asSelectionFromTable = basicAlias.asSelectionFromTable(TestViewModel.VIEW, "superAlias");
        assertEquals(TestViewModel.VIEW.getName(), asSelectionFromTable.table.getName());
        assertEquals(basicAlias.getName(), asSelectionFromTable.getExpression());
        assertEquals("superAlias", asSelectionFromTable.getName());
        assertEquals("SELECT testView.newAlias AS superAlias", Query.select(asSelectionFromTable).toString());

    }

}
