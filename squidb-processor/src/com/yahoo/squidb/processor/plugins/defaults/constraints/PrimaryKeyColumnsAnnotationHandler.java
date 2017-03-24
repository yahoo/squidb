/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.plugins.defaults.constraints;

import com.yahoo.squidb.annotations.tables.ConflictAlgorithm;
import com.yahoo.squidb.annotations.tables.constraints.IndexedColumn;
import com.yahoo.squidb.annotations.tables.constraints.PrimaryKeyColumns;

/**
 * Handler for {@link PrimaryKeyColumns} annotation
 */
public class PrimaryKeyColumnsAnnotationHandler extends IndexedColumnsConstraintHandler<PrimaryKeyColumns> {

    @Override
    protected String[] getColumnNames(PrimaryKeyColumns annotation) {
        return annotation.columns();
    }

    @Override
    protected IndexedColumn[] getIndexedColumns(PrimaryKeyColumns annotation) {
        return annotation.indexedColumns();
    }

    @Override
    protected ConflictAlgorithm getOnConflict(PrimaryKeyColumns annotation) {
        return annotation.onConflict();
    }

    @Override
    protected String getConstraintName() {
        return "PRIMARY KEY";
    }

    @Override
    public Class<PrimaryKeyColumns> getAnnotationClass() {
        return PrimaryKeyColumns.class;
    }
}
