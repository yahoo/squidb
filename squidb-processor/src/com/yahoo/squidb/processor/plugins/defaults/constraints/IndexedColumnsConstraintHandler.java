/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.plugins.defaults.constraints;

import com.yahoo.squidb.annotations.tables.ConflictAlgorithm;
import com.yahoo.squidb.annotations.tables.IndexOrder;
import com.yahoo.squidb.annotations.tables.constraints.IndexedColumn;
import com.yahoo.squidb.processor.StringUtils;
import com.yahoo.squidb.processor.data.TableModelSpecWrapper;
import com.yahoo.squidb.processor.plugins.PluginEnvironment;
import com.yahoo.squidb.processor.plugins.defaults.properties.generators.interfaces.TableModelPropertyGenerator;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Base class for handling {@link com.yahoo.squidb.annotations.tables.constraints.UniqueColumns} and
 * {@link com.yahoo.squidb.annotations.tables.constraints.PrimaryKeyColumns} annotations.
 */
abstract class IndexedColumnsConstraintHandler<ANNOTATION extends Annotation>
        extends AbstractTableConstraintAnnotationHandler<ANNOTATION> {

    @Override
    protected void appendConstraintStringForTable(StringBuilder constraintBuilder, ANNOTATION annotation,
            TableModelSpecWrapper modelSpec, PluginEnvironment pluginEnvironment) {
        constraintBuilder.append(getConstraintName()).append("(");
        IndexedColumn[] indexedColumns = getIndexedColumns(annotation);
        String[] columnNames = getColumnNames(annotation);
        if (indexedColumns.length > 0) {
            appendIndexedColumns(constraintBuilder, indexedColumns);
        } else {
            appendColumnNames(constraintBuilder, columnNames);
        }
        constraintBuilder.append(")");
        ConflictAlgorithm onConflict = getOnConflict(annotation);
        if (onConflict != ConflictAlgorithm.NONE) {
            constraintBuilder.append(" ON CONFLICT ").append(onConflict.name());
        }
    }

    private void appendIndexedColumns(StringBuilder constraintBuilder, IndexedColumn[] indexedColumns) {
        boolean needsSeparator = false;
        for (IndexedColumn indexedCol : indexedColumns) {
            if (needsSeparator) {
                constraintBuilder.append(", ");
            }
            appendIndexedColumn(constraintBuilder, indexedCol);
            needsSeparator = true;
        }
    }

    private void appendIndexedColumn(StringBuilder constraintBuilder, IndexedColumn indexedColumn) {
        constraintBuilder.append(indexedColumn.name());
        String collate = indexedColumn.collate().trim();
        IndexOrder order = indexedColumn.order();
        if (!StringUtils.isEmpty(collate)) {
            constraintBuilder.append(" COLLATE ").append(collate);
        }
        if (order != IndexOrder.UNSPECIFIED) {
            constraintBuilder.append(" ").append(order.name());
        }
    }

    private void appendColumnNames(StringBuilder constraintBuilder, String[] columnNames) {
        boolean needsSeparator = false;
        for (String name : columnNames) {
            if (needsSeparator) {
                constraintBuilder.append(", ");
            }
            constraintBuilder.append(name);
            needsSeparator = true;
        }
    }

    @Override
    protected void validateAnnotationForTable(ANNOTATION annotation, TableModelSpecWrapper modelSpec,
            PluginEnvironment pluginEnvironment) {
        IndexedColumn[] indexedColumns = getIndexedColumns(annotation);
        String[] columnNames = getColumnNames(annotation);
        if (indexedColumns.length > 0 && columnNames.length > 0) {
            modelSpec.logError("Only one of indexedColumns or columnNames can be specified in the @" +
                    getAnnotationClass().getSimpleName() + " annotation", modelSpec.getModelSpecElement());
        } else if (indexedColumns.length == 0 && columnNames.length == 0) {
            modelSpec.logError("At least one of indexedColumns or columnNames must be a non-empty list in the @" +
                    getAnnotationClass().getSimpleName() + " annotation", modelSpec.getModelSpecElement());
        }

        List<String> columnNamesToValidate = new ArrayList<>();
        if (indexedColumns.length > 0) {
            for (IndexedColumn indexedCol : indexedColumns) {
                columnNamesToValidate.add(indexedCol.name());
            }
        } else {
            Collections.addAll(columnNamesToValidate, columnNames);
        }

        Set<String> availableColumnNames = new HashSet<>();
        for (TableModelPropertyGenerator propertyGenerator : modelSpec.getPropertyGenerators()) {
            availableColumnNames.add(propertyGenerator.getColumnName());
        }
        for (TableModelPropertyGenerator propertyGenerator : modelSpec.getDeprecatedPropertyGenerators()) {
            availableColumnNames.add(propertyGenerator.getColumnName());
        }
        for (String name : columnNamesToValidate) {
            if (!availableColumnNames.contains(name)) {
                modelSpec.logError("Annotation @" + getAnnotationClass().getSimpleName() + " references column name '" +
                        name + "' that does not exist in this table", modelSpec.getModelSpecElement());
            }
        }
    }

    protected abstract String[] getColumnNames(ANNOTATION annotation);

    protected abstract IndexedColumn[] getIndexedColumns(ANNOTATION annotation);

    protected abstract ConflictAlgorithm getOnConflict(ANNOTATION annotation);

    protected abstract String getConstraintName();
}
