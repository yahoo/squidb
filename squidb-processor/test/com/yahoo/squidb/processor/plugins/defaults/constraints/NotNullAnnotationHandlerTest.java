/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.plugins.defaults.constraints;

import com.yahoo.squidb.annotations.tables.ConflictAlgorithm;
import com.yahoo.squidb.annotations.tables.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class NotNullAnnotationHandlerTest extends SimpleColumnConstraintAnnotationTest<NotNull> {

    @Override
    protected ColumnConstraintAnnotationHandler<NotNull> getColumnAnnotationHandler() {
        return new NotNullAnnotationHandler();
    }

    @Override
    protected Class<NotNull> getAnnotationClass() {
        return NotNull.class;
    }

    @Override
    protected List<NotNull> getMockedAnnotationMatrix() {
        List<NotNull> result = new ArrayList<>();
        for (ConflictAlgorithm onConflict : ConflictAlgorithm.values()) {
            NotNull notNull = mock(NotNull.class);
            when(notNull.onConflict()).thenReturn(onConflict);
            result.add(notNull);
        }
        return result;
    }

    @Override
    protected List<String> getExpectedConstraintStrings() {
        List<String> result = new ArrayList<>();
        for (ConflictAlgorithm onConflict : ConflictAlgorithm.values()) {
            String expected = " NOT NULL";
            if (onConflict != ConflictAlgorithm.NONE) {
                expected += " ON CONFLICT " + onConflict.name();
            }
            result.add(expected);
        }
        return result;
    }
}
