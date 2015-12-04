/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.data;

import com.yahoo.squidb.sql.Function;
import com.yahoo.squidb.sql.Property;
import com.yahoo.squidb.sql.Property.BooleanProperty;
import com.yahoo.squidb.sql.Property.DoubleProperty;
import com.yahoo.squidb.sql.Property.IntegerProperty;
import com.yahoo.squidb.sql.Property.LongProperty;
import com.yahoo.squidb.sql.Property.StringProperty;
import com.yahoo.squidb.sql.Query;
import com.yahoo.squidb.sql.Table;
import com.yahoo.squidb.test.SquidTestCase;
import com.yahoo.squidb.test.TestModel;
import com.yahoo.squidb.test.TestViewModel;
import com.yahoo.squidb.test.TestVirtualModel;
import com.yahoo.squidb.test.Thing;

public class PropertyTest extends SquidTestCase {

    public void testPropertyAliasing() {
        LongProperty p = TestModel.ID;
        assertEquals(p.getQualifiedExpression(), "testModels._id");
        assertEquals(p.getExpression(), "_id");
        assertEquals(p.getName(), "_id");

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

        LongProperty asSelectionFromTableNoAlias = TestViewModel.VIEW.qualifyField(basicAlias);
        assertEquals(TestViewModel.VIEW.getName(), asSelectionFromTableNoAlias.table.getName());
        assertEquals(basicAlias.getName(), asSelectionFromTableNoAlias.getExpression());
        assertEquals(basicAlias.getName(), asSelectionFromTableNoAlias.getName());
        assertFalse(asSelectionFromTableNoAlias.hasAlias());
        assertEquals("SELECT testView.newAlias AS newAlias", Query.select(asSelectionFromTableNoAlias).toString());

        LongProperty asSelectionFromTableWithAlias = basicAlias.asSelectionFromTable(TestViewModel.VIEW, "superAlias");
        assertEquals(TestViewModel.VIEW.getName(), asSelectionFromTableWithAlias.table.getName());
        assertEquals(basicAlias.getName(), asSelectionFromTableWithAlias.getExpression());
        assertEquals("superAlias", asSelectionFromTableWithAlias.getName());
        assertEquals("SELECT testView.newAlias AS superAlias", Query.select(asSelectionFromTableWithAlias).toString());

        assertEquals(TestVirtualModel.ID.getQualifiedExpression(), "virtual_models.rowid");
        assertEquals(TestVirtualModel.ID.getExpression(), "rowid");
        assertEquals(TestVirtualModel.ID.getName(), "rowid");

        assertEquals(Thing.ID.getQualifiedExpression(), "things.id");
        assertEquals(Thing.ID.getExpression(), "id");
        assertEquals(Thing.ID.getName(), "id");
    }

    public void testEqualsAndHashCode() {
        LongProperty test1 = new LongProperty(TestModel.TABLE, "testCol");
        LongProperty test2 = new LongProperty(TestModel.TABLE, "testCol");

        assertEquals(test1, test2);
        assertEquals(test1.hashCode(), test2.hashCode());

        StringProperty test3 = new StringProperty(TestModel.TABLE, "testCol");
        StringProperty test4 = new StringProperty(TestModel.TABLE, "testCol", "DEFAULT 'A'");

        assertEquals(test3, test4);
        assertEquals(test3.hashCode(), test4.hashCode());

        Function<Integer> func1 = Function.count();
        Function<Integer> func2 = Function.rawFunction("COUNT(*)");

        assertEquals(func1, func2);
        assertEquals(func1.hashCode(), func2.hashCode());

        IntegerProperty test5 = Property.IntegerProperty.fromFunction(func1, "count");
        IntegerProperty test6 = Property.IntegerProperty.fromFunction(func2, "count");

        assertEquals(test5, test6);
        assertEquals(test5.hashCode(), test6.hashCode());
    }

    public void testAliasedTableHasIdProperty() {
        Table testModelAlias = TestModel.TABLE.as("testModelAlias");
        LongProperty testModelAliasId = testModelAlias.getIdProperty();
        assertEquals("testModelAlias._id", testModelAliasId.getQualifiedExpression());
    }

    public void testLiteralProperties() {
        StringProperty stringLiteral = StringProperty.literal("abc", "strLit");
        assertEquals("SELECT 'abc' AS strLit", Query.select(stringLiteral).toString());

        IntegerProperty intLiteral = IntegerProperty.literal(1, "intLit");
        assertEquals("SELECT 1 AS intLit", Query.select(intLiteral).toString());

        long longVal = System.currentTimeMillis();
        LongProperty longLiteral = LongProperty.literal(longVal, "longLit");
        assertEquals("SELECT " + longVal + " AS longLit", Query.select(longLiteral).toString());

        DoubleProperty doubleLiteral = DoubleProperty.literal(1.1, "doubleLit");
        assertEquals("SELECT 1.1 AS doubleLit", Query.select(doubleLiteral).toString());

        BooleanProperty trueLiteral = BooleanProperty.literal(true, "trueLit");
        assertEquals("SELECT 1 AS trueLit", Query.select(trueLiteral).toString());

        BooleanProperty falseLiteral = BooleanProperty.literal(false, "falseLit");
        assertEquals("SELECT 0 AS falseLit", Query.select(falseLiteral).toString());
    }

}
