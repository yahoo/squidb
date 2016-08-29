/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.sql;

import com.yahoo.squidb.data.AbstractModel;
import com.yahoo.squidb.data.SquidCursor;
import com.yahoo.squidb.sql.Property.BooleanProperty;
import com.yahoo.squidb.sql.Property.DoubleProperty;
import com.yahoo.squidb.sql.Property.EnumProperty;
import com.yahoo.squidb.sql.Property.IntegerProperty;
import com.yahoo.squidb.sql.Property.LongProperty;
import com.yahoo.squidb.sql.Property.StringProperty;
import com.yahoo.squidb.test.DatabaseTestCase;
import com.yahoo.squidb.test.TestEnum;
import com.yahoo.squidb.test.TestModel;
import com.yahoo.squidb.test.TestSubqueryModel;
import com.yahoo.squidb.test.TestViewModel;
import com.yahoo.squidb.test.TestVirtualModel;
import com.yahoo.squidb.test.Thing;

public class PropertyTest extends DatabaseTestCase {

    public void testPropertyAliasing() {
        LongProperty p = TestModel.ID;
        assertEquals(p.getQualifiedExpression(), "testModels._id");
        assertEquals(p.getExpression(), "_id");
        assertEquals(p.getName(), "_id");

        LongProperty basicAlias = p.as("newAlias");
        assertEquals(p.tableModelName, basicAlias.tableModelName);
        assertEquals(p.getExpression(), basicAlias.getExpression());
        assertEquals("newAlias", basicAlias.getName());
        assertEquals("SELECT testModels._id AS newAlias", Query.select(basicAlias).toString());

        LongProperty aliasWithTable = p.as("newTable", "newAlias");
        assertEquals(TestModel.class, aliasWithTable.tableModelName.modelClass);
        assertEquals("newTable", aliasWithTable.tableModelName.tableName);
        assertEquals(p.getExpression(), aliasWithTable.getExpression());
        assertEquals("newAlias", aliasWithTable.getName());
        assertEquals("SELECT newTable._id AS newAlias", Query.select(aliasWithTable).toString());

        LongProperty asSelectionFromTableNoAlias = TestViewModel.VIEW.qualifyField(basicAlias);
        assertEquals(TestViewModel.class, asSelectionFromTableNoAlias.tableModelName.modelClass);
        assertEquals(TestViewModel.VIEW.getName(), asSelectionFromTableNoAlias.tableModelName.tableName);
        assertEquals(basicAlias.getName(), asSelectionFromTableNoAlias.getExpression());
        assertEquals(basicAlias.getName(), asSelectionFromTableNoAlias.getName());
        assertFalse(asSelectionFromTableNoAlias.hasAlias());
        assertEquals("SELECT testView.newAlias AS newAlias", Query.select(asSelectionFromTableNoAlias).toString());

        LongProperty asSelectionFromTableWithAlias = basicAlias.asSelectionFromTable(TestViewModel.VIEW, "superAlias");
        assertEquals(TestViewModel.class, asSelectionFromTableWithAlias.tableModelName.modelClass);
        assertEquals(TestViewModel.VIEW.getName(), asSelectionFromTableWithAlias.tableModelName.tableName);
        assertEquals(basicAlias.getName(), asSelectionFromTableWithAlias.getExpression());
        assertEquals("superAlias", asSelectionFromTableWithAlias.getName());
        assertEquals("SELECT testView.newAlias AS superAlias", Query.select(asSelectionFromTableWithAlias).toString());

        assertEquals(TestVirtualModel.ROWID.getQualifiedExpression(), "virtual_models.rowid");
        assertEquals(TestVirtualModel.ROWID.getExpression(), "rowid");
        assertEquals(TestVirtualModel.ROWID.getName(), "rowid");

        assertEquals(Thing.ID.getQualifiedExpression(), "things.id");
        assertEquals(Thing.ID.getExpression(), "id");
        assertEquals(Thing.ID.getName(), "id");
    }

    public void testEqualsAndHashCode() {
        LongProperty test1 = new LongProperty(
                new TableModelName(TestModel.class, TestModel.TABLE.getName()), "testCol");
        LongProperty test2 = new LongProperty(
                new TableModelName(TestModel.class, TestModel.TABLE.getName()), "testCol");

        assertEquals(test1, test2);
        assertEquals(test1.hashCode(), test2.hashCode());

        StringProperty test3 = new StringProperty(
                new TableModelName(TestModel.class, TestModel.TABLE.getName()), "testCol");
        StringProperty test4 = new StringProperty(
                new TableModelName(TestModel.class, TestModel.TABLE.getName()), "testCol", "DEFAULT 'A'");

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

    public void testAliasedTableAliasesAllProperties() {
        testTableAndViewAliasing(TestModel.class, TestModel.TABLE);
    }

    public void testAliasedVirtualTableAliasesAllProperties() {
        testTableAndViewAliasing(TestVirtualModel.class, TestVirtualModel.TABLE);
    }

    public void testAliasedViewAliasesAllProperties() {
        testTableAndViewAliasing(TestViewModel.class, TestViewModel.VIEW);
    }

    public void testAliasedSubqueryAliasesAllProperties() {
        testTableAndViewAliasing(TestSubqueryModel.class, TestSubqueryModel.SUBQUERY);
    }

    private void testTableAndViewAliasing(Class<? extends AbstractModel> modelClass, SqlTable<?> tableOrView) {
        SqlTable<?> testModelAlias = tableOrView.as("testModelAlias");
        TableModelName expectedTableModelName = new TableModelName(modelClass, "testModelAlias");
        if (testModelAlias instanceof Table) {
            LongProperty testModelAliasId = ((Table) testModelAlias).getRowIdProperty();
            String idName = testModelAlias instanceof VirtualTable ? "rowid" : "_id";
            assertEquals("testModelAlias." + idName, testModelAliasId.getQualifiedExpression());
            assertEquals(expectedTableModelName, testModelAliasId.tableModelName);
        }

        Property<?>[] originalProperties = tableOrView.getProperties();
        Property<?>[] aliasedProperties = testModelAlias.getProperties();
        assertEquals(originalProperties.length, aliasedProperties.length);
        for (int i = 0; i < aliasedProperties.length; i++) {
            String expectedExpression = "testModelAlias." + originalProperties[i].getName();
            assertEquals(expectedExpression, aliasedProperties[i].getQualifiedExpression());
            assertEquals(expectedTableModelName, aliasedProperties[i].tableModelName);
        }

        Query query = Query.select().from(testModelAlias);
        StringBuilder expectedSql = new StringBuilder("SELECT ");
        for (int i = 0; i < originalProperties.length; i++) {
            String expectedExpression = "testModelAlias." + originalProperties[i].getName() +
                    " AS " + originalProperties[i].getName();
            expectedSql.append(expectedExpression);
            if (i < originalProperties.length - 1) {
                expectedSql.append(", ");
            }
        }
        expectedSql.append(" FROM ");
        if (tableOrView instanceof SubqueryTable) {
            expectedSql.append("(");
            String compiledSql = ((SubqueryTable) tableOrView).query.compile(database.getCompileContext()).sql;
            expectedSql.append(compiledSql).append(") AS testModelAlias");
        } else {
            expectedSql.append(tableOrView.getName()).append(" AS testModelAlias");
        }
        assertEquals(expectedSql.toString(), query.compile(database.getCompileContext()).sql);
        SquidCursor<?> cursor = database.query(modelClass, query); // Test that this is valid SQL
        try {
            assertEquals(0, cursor.getCount());
        } finally {
            cursor.close();
        }
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

        EnumProperty enumLiteral = EnumProperty.literal(TestEnum.APPLE, "enumLit");
        assertEquals("SELECT 'APPLE' AS enumLit", Query.select(enumLiteral).toString());
    }

}
