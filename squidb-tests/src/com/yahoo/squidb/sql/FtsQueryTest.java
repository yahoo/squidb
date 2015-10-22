/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.sql;

import com.yahoo.squidb.data.ICursor;
import com.yahoo.squidb.data.SquidCursor;
import com.yahoo.squidb.test.DatabaseTestCase;
import com.yahoo.squidb.test.TestVirtualModel;

public class FtsQueryTest extends DatabaseTestCase {

    TestVirtualModel model1;
    TestVirtualModel model2;
    TestVirtualModel model3;
    TestVirtualModel model4;
    TestVirtualModel model5;

    @Override
    protected void setupDatabase() {
        super.setupDatabase();

        model1 = new TestVirtualModel()
                .setTitle("Programmer")
                .setBody("(n.) a person who converts caffeine and pizza into sometimes-working code.");
        database.persist(model1);

        model2 = new TestVirtualModel()
                .setTitle("Java code")
                .setBody("A programmer had a problem, so he decided to use Java. Now he has a ProblemFactory.");
        database.persist(model2);

        model3 = new TestVirtualModel()
                .setTitle("How programmers think")
                .setBody("A programmer is at work when his wife calls and asks him to go to the store. She says "
                        + "she needs a gallon of milk, and if they have fresh eggs, buy a dozen. He comes home with "
                        + "12 gallons of milk.");
        database.persist(model3);

        model4 = new TestVirtualModel()
                .setTitle("Support")
                .setBody("Programming is like sex: One mistake and you have to support it for the rest of your life.");
        database.persist(model4);

        model5 = new TestVirtualModel()
                .setTitle("Smooth operator")
                .setBody("A SQL query walks into a bar, strolls up to a couple of tables and asks, "
                        + "\"May I join you?\"");
        database.persist(model5);
    }

    public void testQueryUsingIdProperty() {
        // sanity check that ID works as intended
        testQueryResults(TestVirtualModel.ID.lt(3), model1, model2);
    }

    public void testMatchCriterion() {
        boolean enhancedQuerySyntax = false;
        ICursor compileOptions = database.rawQuery("PRAGMA compile_options", null);
        try {
            while (compileOptions.moveToNext()) {
                String option = compileOptions.getString(0);
                if ("ENABLE_FTS3_PARENTHESIS".equals(option)) {
                    enhancedQuerySyntax = true;
                    break;
                }
            }
        } finally {
            compileOptions.close();
        }
        // match a column
        testQueryResults(TestVirtualModel.BODY.match("programmer"), model2, model3);
        // match any column
        testQueryResults(TestVirtualModel.TABLE.match("programmer"), model1, model2, model3);
        // match prefix
        testQueryResults(TestVirtualModel.BODY.match("program*"), model2, model3, model4);
        // AND
        testQueryResults(TestVirtualModel.BODY.match("program* java"), model2);
        // OR
        testQueryResults(TestVirtualModel.BODY.match("java OR sql"), model2, model5);
        // NOT
        String notMatch = "program* " + (enhancedQuerySyntax ? "NOT " : "-") + "java";
        testQueryResults(TestVirtualModel.BODY.match(notMatch), model3, model4);
        // scoped terms
        testQueryResults(TestVirtualModel.BODY.match("programmer title:java"), model2);
        testQueryResults(TestVirtualModel.TABLE.match("body:code"), model1);
        // near
        testQueryResults(TestVirtualModel.BODY.match("program* NEAR java"), model2);
        testQueryResults(TestVirtualModel.BODY.match("program* NEAR/7 java"));

        // match first token. only available since sqlite 3.7.9
        if (database.getSqliteVersion().isAtLeast("3.7.9")) {
            testQueryResults(TestVirtualModel.BODY.match("^program*"), model4);
        } else {
            testQueryResults(TestVirtualModel.BODY.match("^program*"), model2, model3, model4);
        }
    }

    private void testQueryResults(Criterion criterion, TestVirtualModel... expectedResults) {
        Query query = Query.select(TestVirtualModel.PROPERTIES).where(criterion);
        SquidCursor<TestVirtualModel> cursor = database.query(TestVirtualModel.class, query);
        try {
            int expectedCount = expectedResults == null ? 0 : expectedResults.length;
            assertEquals(expectedCount, cursor.getCount());
            if (expectedCount == 0) {
                return;
            }

            for (int i = 0; i < expectedCount; i++) {
                cursor.moveToPosition(i);
                assertEquals(expectedResults[i], new TestVirtualModel(cursor));
            }
        } finally {
            cursor.close();
        }
    }
}
