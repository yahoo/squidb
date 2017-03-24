/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.plugins.defaults.constraints;

import com.yahoo.squidb.annotations.tables.ConflictAlgorithm;
import com.yahoo.squidb.annotations.tables.IndexOrder;
import com.yahoo.squidb.annotations.tables.constraints.IndexedColumn;
import com.yahoo.squidb.processor.data.TableModelSpecWrapper;

import org.junit.Test;

import java.lang.annotation.Annotation;

import javax.lang.model.element.Element;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

abstract class IndexedColumnsConstraintHandlerTest<A extends Annotation> extends ConstraintAnnotationTest {

    @Test
    public void testConstraintIsAppendedToColumnDefinitionWithColumnNames() {
        for (int i = 1; i <= 5; i++) {
            for (ConflictAlgorithm onConflict : ConflictAlgorithm.values()) {
                testConstraintWithParams(i, 0, onConflict);
            }
        }
    }

    private void testConstraintWithParams(int colNameSize, int indexedColumnSize,
            ConflictAlgorithm onConflict) {
        TableConstraintAnnotationHandler<A> handler = getTableAnnotationHandler();
        String[] mockedColumns = getMockedColumnNames(colNameSize);
        IndexedColumn[] mockedIndexedColumns = getMockedIndexedColumns(indexedColumnSize);
        A mockedAnnotation = getMockedAnnotation(mockedColumns, mockedIndexedColumns, onConflict);
        String expectedConstraintString = getExpectedConstraintString(mockedColumns, mockedIndexedColumns, onConflict);

        StringBuilder builder = new StringBuilder();
        handler.appendConstraintForTable(builder,
                mockTableModelSpecWithAnnotation(getAnnotationClass(), mockedAnnotation), pluginEnv);
        assertEquals(expectedConstraintString, builder.toString());
    }

    @Test
    public void testConstraintIsAppendedToColumnDefinitionWithIndexedColumns() {
        for (int i = 1; i <= 5; i++) {
            for (ConflictAlgorithm onConflict : ConflictAlgorithm.values()) {
                testConstraintWithParams(0, i, onConflict);
            }
        }
    }

    @Test
    public void testNamesAndIndexedColumnsSpecifiedLogsValidationError() {
        TableConstraintAnnotationHandler<A> handler = getTableAnnotationHandler();
        A annotation = getMockedAnnotation(getMockedColumnNames(1), getMockedIndexedColumns(1), ConflictAlgorithm.NONE);
        TableModelSpecWrapper modelSpec = mockTableModelSpecWithAnnotation(getAnnotationClass(), annotation);
        handler.validateAnnotationForTable(modelSpec, pluginEnv);
        verify(modelSpec, atLeastOnce()).logError(anyString(), any(Element.class));
    }

    @Test
    public void testNoNamesOrIndexedColumnsSpecifiedLogsValidationError() {
        TableConstraintAnnotationHandler<A> handler = getTableAnnotationHandler();
        A annotation = getMockedAnnotation(getMockedColumnNames(0), getMockedIndexedColumns(0), ConflictAlgorithm.NONE);
        TableModelSpecWrapper modelSpec = mockTableModelSpecWithAnnotation(getAnnotationClass(), annotation);
        handler.validateAnnotationForTable(modelSpec, pluginEnv);
        verify(modelSpec, atLeastOnce()).logError(anyString(), any(Element.class));
    }

    private String[] getMockedColumnNames(int size) {
        String[] result = new String[size];
        for (int i = 0; i < result.length; i++) {
            result[i] = "col" + i;
        }
        return result;
    }

    private IndexedColumn[] getMockedIndexedColumns(int size) {
        IndexedColumn[] result = new IndexedColumn[size];
        for (int i = 0; i < result.length; i++) {
            IndexedColumn col = mock(IndexedColumn.class);
            when(col.name()).thenReturn("col" + i);
            when(col.collate()).thenReturn(size % 2 == 0 ? "" : "NOCASE");
            when(col.order()).thenReturn(IndexOrder.values()[i % IndexOrder.values().length]);
            result[i] = col;
        }
        return result;
    }

    private String columnNamesToString(String[] columnNames) {
        StringBuilder builder = new StringBuilder();
        boolean needsSeparator = false;
        for (String col : columnNames) {
            if (needsSeparator) {
                builder.append(", ");
            }
            builder.append(col);
            needsSeparator = true;
        }
        return builder.toString();
    }

    private String indexedColumnsToString(IndexedColumn[] indexedColumns) {
        StringBuilder builder = new StringBuilder();
        boolean needsSeparator = false;
        for (IndexedColumn col : indexedColumns) {
            if (needsSeparator) {
                builder.append(", ");
            }
            builder.append(col.name());
            if (!col.collate().isEmpty()) {
                builder.append(" COLLATE ").append(col.collate());
            }
            if (col.order() != IndexOrder.UNSPECIFIED) {
                builder.append(" ").append(col.order().name());
            }
            needsSeparator = true;
        }
        return builder.toString();
    }

    private String getExpectedConstraintString(String[] mockedColumnNames, IndexedColumn[] mockedIndexedColumns,
            ConflictAlgorithm onConflict) {
        StringBuilder expectedConstraint = new StringBuilder(getConstraintString()).append("(");
        if (mockedIndexedColumns.length > 0) {
            expectedConstraint.append(indexedColumnsToString(mockedIndexedColumns));
        } else if (mockedColumnNames.length > 0) {
            expectedConstraint.append(columnNamesToString(mockedColumnNames));
        }
        expectedConstraint.append(")");
        if (onConflict != ConflictAlgorithm.NONE) {
            expectedConstraint.append(" ON CONFLICT ").append(onConflict.name());
        }
        return expectedConstraint.toString();
    }

    protected abstract TableConstraintAnnotationHandler<A> getTableAnnotationHandler();

    protected abstract Class<A> getAnnotationClass();

    protected abstract A getMockedAnnotation(String[] columnNames, IndexedColumn[] indexedColumns,
            ConflictAlgorithm onConflict);

    protected abstract String getConstraintString();

}
