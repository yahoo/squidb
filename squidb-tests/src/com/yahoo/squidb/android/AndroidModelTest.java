/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.android;

import android.content.ContentValues;
import android.os.Parcel;

import com.yahoo.squidb.data.ValuesStorage;
import com.yahoo.squidb.test.DatabaseTestCase;
import com.yahoo.squidb.test.TestModel;

import java.lang.reflect.Field;

public class AndroidModelTest extends DatabaseTestCase {

    public void testModelParcelable() {
        TestModel model = new TestModel()
                .setFirstName("A")
                .setLastName("B")
                .setLuckyNumber(2)
                .setBirthday(System.currentTimeMillis())
                .setIsHappy(true);

        Parcel parcel = Parcel.obtain();
        model.writeToParcel(parcel, 0);

        parcel.setDataPosition(0);

        TestModel createdFromParcel = TestModel.CREATOR.createFromParcel(parcel);
        assertFalse(model == createdFromParcel);
        assertEquals(model, createdFromParcel);
    }

    public void testTypesafeReadFromContentValues() {
        testContentValuesTypes(false);
    }

    public void testTypesafeSetFromContentValues() {
        testContentValuesTypes(true);
    }

    private void testContentValuesTypes(final boolean useSetValues) {
        final ContentValues values = new ContentValues();
        values.put(TestModel.FIRST_NAME.getName(), "A");
        values.put(TestModel.LAST_NAME.getName(), "B");
        values.put(TestModel.BIRTHDAY.getName(), 1); // Putting an int where long expected
        values.put(TestModel.IS_HAPPY.getName(), 1); // Putting an int where boolean expected
        values.put(TestModel.SOME_DOUBLE.getName(), 1); // Putting an int where double expected
        values.put(TestModel.$_123_ABC.getName(), "1"); // Putting a String where int expected

        TestModel fromValues;
        if (useSetValues) {
            fromValues = new TestModel();
            fromValues.setPropertiesFromContentValues(values, TestModel.PROPERTIES);
        } else {
            fromValues = new TestModel(values);
        }

        // Check the types stored in the values
        ValuesStorage checkTypesOn = useSetValues ? fromValues.getSetValues() : fromValues.getDatabaseValues();
        assertNotNull(checkTypesOn);
        assertTrue(checkTypesOn.get(TestModel.FIRST_NAME.getName()) instanceof String);
        assertTrue(checkTypesOn.get(TestModel.LAST_NAME.getName()) instanceof String);
        assertTrue(checkTypesOn.get(TestModel.BIRTHDAY.getName()) instanceof Long);
        assertTrue(checkTypesOn.get(TestModel.IS_HAPPY.getName()) instanceof Boolean);
        assertTrue(checkTypesOn.get(TestModel.SOME_DOUBLE.getName()) instanceof Double);
        assertTrue(checkTypesOn.get(TestModel.$_123_ABC.getName()) instanceof Integer);

        // Check the types using the model getters
        assertEquals("A", fromValues.getFirstName());
        assertEquals("B", fromValues.getLastName());
        assertEquals((Long) 1L, fromValues.getBirthday());
        assertNonNullAndTrue(fromValues.isHappy());
        assertEquals(1.0, fromValues.getSomeDouble());
        assertEquals((Integer) 1, fromValues.get$123abc());

        values.clear();
        values.put(TestModel.IS_HAPPY.getName(), "ABC");
        testThrowsException(new Runnable() {
            @Override
            public void run() {
                if (useSetValues) {
                    new TestModel().setPropertiesFromContentValues(values, TestModel.IS_HAPPY);
                } else {
                    new TestModel(values);
                }
            }
        }, ClassCastException.class);
    }

    public void testValueCoercionAppliesToAllValues() {
        // Make sure the model is initialized with values and setValues
        ContentValues values = new ContentValues();
        values.put(TestModel.FIRST_NAME.getName(), "A");
        TestModel model = new TestModel();
        model.readPropertiesFromContentValues(values, TestModel.FIRST_NAME);
        model.setFirstName("B");

        ValuesStorage defaultValuesInternal;
        try {
            // Access this internal field reflectively so we can put a new value for this test
            Field defaultValuesInternalField = TestModel.class.getDeclaredField("defaultValuesInternal");
            defaultValuesInternalField.setAccessible(true);
            defaultValuesInternal = (ValuesStorage) defaultValuesInternalField.get(null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        defaultValuesInternal.put(TestModel.IS_HAPPY.getName(), 1);
        assertNonNullAndTrue(model.isHappy()); // Test default values
        assertNotNull(model.getDatabaseValues());
        model.getDatabaseValues().put(TestModel.IS_HAPPY.getName(), 0);
        assertNonNullAndFalse(model.isHappy()); // Test database values
        assertNotNull(model.getSetValues());
        model.getSetValues().put(TestModel.IS_HAPPY.getName(), 1);
        assertNonNullAndTrue(model.isHappy()); // Test set values

        defaultValuesInternal.put(TestModel.IS_HAPPY.getName(), true); // Reset the static variable
    }
}
