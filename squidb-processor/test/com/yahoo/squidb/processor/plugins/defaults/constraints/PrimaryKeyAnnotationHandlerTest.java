/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.plugins.defaults.constraints;

import com.yahoo.squidb.annotations.tables.ConflictAlgorithm;
import com.yahoo.squidb.annotations.tables.IndexOrder;
import com.yahoo.squidb.annotations.tables.constraints.PrimaryKey;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PrimaryKeyAnnotationHandlerTest extends SimpleColumnConstraintAnnotationTest<PrimaryKey> {

    @Override
    protected ColumnConstraintAnnotationHandler<PrimaryKey> getColumnAnnotationHandler() {
        return new PrimaryKeyAnnotationHandler();
    }

    @Override
    protected Class<PrimaryKey> getAnnotationClass() {
        return PrimaryKey.class;
    }

    @Override
    protected List<PrimaryKey> getMockedAnnotationMatrix() {
        List<PrimaryKey> result = new ArrayList<>();
        for (IndexOrder order : IndexOrder.values()) {
            for (ConflictAlgorithm onConflict : ConflictAlgorithm.values()) {
                PrimaryKey primaryKey = mock(PrimaryKey.class);
                when(primaryKey.order()).thenReturn(order);
                when(primaryKey.onConflict()).thenReturn(onConflict);
                result.add(primaryKey);
            }
        }
        return result;
    }

    @Override
    protected List<String> getExpectedConstraintStrings() {
        List<String> result = new ArrayList<>();
        for (IndexOrder order : IndexOrder.values()) {
            for (ConflictAlgorithm onConflict : ConflictAlgorithm.values()) {
                String expected = " PRIMARY KEY";
                if (order != IndexOrder.UNSPECIFIED) {
                    expected += " " + order.name();
                }
                if (onConflict != ConflictAlgorithm.NONE) {
                    expected += " ON CONFLICT " + onConflict.name();
                }
                result.add(expected);
            }
        }
        return result;
    }
}
