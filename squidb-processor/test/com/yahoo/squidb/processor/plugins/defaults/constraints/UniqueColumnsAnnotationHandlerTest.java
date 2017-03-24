/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.plugins.defaults.constraints;

import com.yahoo.squidb.annotations.tables.ConflictAlgorithm;
import com.yahoo.squidb.annotations.tables.constraints.IndexedColumn;
import com.yahoo.squidb.annotations.tables.constraints.UniqueColumns;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UniqueColumnsAnnotationHandlerTest extends IndexedColumnsConstraintHandlerTest<UniqueColumns> {

    @Override
    protected TableConstraintAnnotationHandler<UniqueColumns> getTableAnnotationHandler() {
        return new UniqueColumnsAnnotationHandler();
    }

    @Override
    protected Class<UniqueColumns> getAnnotationClass() {
        return UniqueColumns.class;
    }

    @Override
    protected UniqueColumns getMockedAnnotation(String[] columnNames, IndexedColumn[] indexedColumns,
            ConflictAlgorithm onConflict) {
        UniqueColumns annotation = mock(UniqueColumns.class);
        when(annotation.columns()).thenReturn(columnNames);
        when(annotation.indexedColumns()).thenReturn(indexedColumns);
        when(annotation.onConflict()).thenReturn(onConflict);
        return annotation;
    }



    @Override
    protected String getConstraintString() {
        return "UNIQUE";
    }
}
