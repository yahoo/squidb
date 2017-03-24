/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.plugins.defaults.constraints;

import com.yahoo.squidb.annotations.tables.ConflictAlgorithm;
import com.yahoo.squidb.annotations.tables.constraints.IndexedColumn;
import com.yahoo.squidb.annotations.tables.constraints.UniqueColumns;

/**
 * Handler for {@link UniqueColumns} annotation
 */
public class UniqueColumnsAnnotationHandler extends IndexedColumnsConstraintHandler<UniqueColumns> {

    @Override
    protected String[] getColumnNames(UniqueColumns annotation) {
        return annotation.columns();
    }

    @Override
    protected IndexedColumn[] getIndexedColumns(UniqueColumns annotation) {
        return annotation.indexedColumns();
    }

    @Override
    protected ConflictAlgorithm getOnConflict(UniqueColumns annotation) {
        return annotation.onConflict();
    }

    @Override
    protected String getConstraintName() {
        return "UNIQUE";
    }

    @Override
    public Class<UniqueColumns> getAnnotationClass() {
        return UniqueColumns.class;
    }
}
