/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.plugins.defaults.constraints;

import com.yahoo.squidb.annotations.tables.ConflictAlgorithm;
import com.yahoo.squidb.annotations.tables.constraints.Unique;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UniqueAnnotationHandlerTest extends SimpleColumnConstraintAnnotationTest<Unique> {

    @Override
    protected ColumnConstraintAnnotationHandler<Unique> getColumnAnnotationHandler() {
        return new UniqueAnnotationHandler();
    }

    @Override
    protected Class<Unique> getAnnotationClass() {
        return Unique.class;
    }

    @Override
    protected List<Unique> getMockedAnnotationMatrix() {
        List<Unique> result = new ArrayList<>();
        for (ConflictAlgorithm onConflict : ConflictAlgorithm.values()) {
            Unique unique = mock(Unique.class);
            when(unique.onConflict()).thenReturn(onConflict);
            result.add(unique);
        }
        return result;
    }

    @Override
    protected List<String> getExpectedConstraintStrings() {
        List<String> result = new ArrayList<>();
        for (ConflictAlgorithm onConflict : ConflictAlgorithm.values()) {
            String expected = " UNIQUE";
            if (onConflict != ConflictAlgorithm.NONE) {
                expected += " ON CONFLICT " + onConflict.name();
            }
            result.add(expected);
        }
        return result;
    }
}
