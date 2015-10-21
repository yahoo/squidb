/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.test;

import com.yahoo.squidb.data.SquidDatabase;

import java.util.Calendar;

public class DatabaseTestCase extends SquidTestCase {

    protected SquidDatabase database;

    protected final long testDate;

    {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, 1988);
        cal.set(Calendar.MONTH, Calendar.MAY);
        cal.set(Calendar.DATE, 11);
        testDate = cal.getTimeInMillis();
    }

    protected TestModel insertBasicTestModel() {
        return insertBasicTestModel("Sam", "Bosley", testDate);
    }

    protected TestModel insertBasicTestModel(String firstName, String lastName, long birthday) {
        TestModel model = new TestModel();

        model.setFirstName(firstName);
        model.setLastName(lastName);
        model.setBirthday(birthday);
        model.setIsHappy(true);

        database.persist(model);
        assertTrue(model.isSaved());
        return model;
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        setupDatabase();
    }

    /**
     * Called during {@link #setUp()} to initialize the database. The base implementation creates a new
     * {@link TestDatabase}. Subclasses that want to insert test data should override and call super, then perform its
     * operations.
     */
    protected void setupDatabase() {
        database = new TestDatabase();
        database.clear();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        tearDownDatabase();
    }

    /**
     * Called during {@link #tearDown()} to clean up any databases. The base implementation tries to close the database
     * created in {@link #setupDatabase()}.
     */
    protected void tearDownDatabase() {
        if (database != null) {
            database.close();
        }
    }
}
