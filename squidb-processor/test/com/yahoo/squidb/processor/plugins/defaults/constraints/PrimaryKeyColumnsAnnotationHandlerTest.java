/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.plugins.defaults.constraints;

import com.yahoo.squidb.annotations.tables.ConflictAlgorithm;
import com.yahoo.squidb.annotations.tables.constraints.IndexedColumn;
import com.yahoo.squidb.annotations.tables.constraints.PrimaryKeyColumns;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PrimaryKeyColumnsAnnotationHandlerTest extends IndexedColumnsConstraintHandlerTest<PrimaryKeyColumns> {

    @Override
    protected TableConstraintAnnotationHandler<PrimaryKeyColumns> getTableAnnotationHandler() {
        return new PrimaryKeyColumnsAnnotationHandler();
    }

    @Override
    protected Class<PrimaryKeyColumns> getAnnotationClass() {
        return PrimaryKeyColumns.class;
    }

    @Override
    protected PrimaryKeyColumns getMockedAnnotation(String[] columnNames, IndexedColumn[] indexedColumns,
            ConflictAlgorithm onConflict) {
        PrimaryKeyColumns annotation = mock(PrimaryKeyColumns.class);
        when(annotation.columns()).thenReturn(columnNames);
        when(annotation.indexedColumns()).thenReturn(indexedColumns);
        when(annotation.onConflict()).thenReturn(onConflict);
        return annotation;
    }



    @Override
    protected String getConstraintString() {
        return "PRIMARY KEY";
    }
}
