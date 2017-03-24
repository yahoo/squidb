/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.plugins.defaults.constraints;

import com.yahoo.squidb.annotations.tables.constraints.Check;
import com.yahoo.squidb.annotations.tables.constraints.ConstraintSql;
import com.yahoo.squidb.annotations.tables.constraints.TableConstraints;
import com.yahoo.squidb.annotations.tables.constraints.UniqueColumns;
import com.yahoo.squidb.processor.data.TableModelSpecWrapper;
import com.yahoo.squidb.processor.plugins.PluginEnvironment;

/**
 * Handler for the {@link TableConstraints} annotation
 */
public class TableConstraintsContainerAnnotationHandler
        extends AbstractTableConstraintAnnotationHandler<TableConstraints> {

    @Override
    protected void appendConstraintStringForTable(StringBuilder constraintBuilder, TableConstraints annotation,
            TableModelSpecWrapper modelSpec, PluginEnvironment pluginEnvironment) {
        for (final UniqueColumns uniqueColumns : annotation.uniques()) {
            getUniqueHandler(uniqueColumns).appendConstraintForTable(constraintBuilder, modelSpec, pluginEnvironment);
        }
        for (Check check : annotation.checks()) {
            getCheckHandler(check).appendConstraintForTable(constraintBuilder, modelSpec, pluginEnvironment);
        }
        for (ConstraintSql constraintSql : annotation.constraintSqls()) {
            getConstraintSqlHandler(constraintSql).appendConstraintForTable(constraintBuilder, modelSpec, pluginEnvironment);
        }
    }

    @Override
    protected void validateAnnotationForTable(TableConstraints annotation, TableModelSpecWrapper modelSpec,
            PluginEnvironment pluginEnvironment) {
        for (UniqueColumns uniqueColumns : annotation.uniques()) {
            getUniqueHandler(uniqueColumns).validateAnnotationForTable(modelSpec, pluginEnvironment);
        }
        for (Check check : annotation.checks()) {
            getCheckHandler(check).validateAnnotationForTable(modelSpec, pluginEnvironment);
        }
        for (ConstraintSql constraintSql : annotation.constraintSqls()) {
            getConstraintSqlHandler(constraintSql).validateAnnotationForTable(modelSpec, pluginEnvironment);
        }
    }

    private UniqueColumnsAnnotationHandler getUniqueHandler(final UniqueColumns uniqueColumns) {
        return new UniqueColumnsAnnotationHandler() {
            @Override
            protected UniqueColumns getAnnotation(TableModelSpecWrapper modelSpec) {
                return uniqueColumns;
            }
        };
    }

    private CheckAnnotationHandler.TableCheckAnnotationHandler getCheckHandler(final Check check) {
        return new CheckAnnotationHandler.TableCheckAnnotationHandler() {
            @Override
            protected Check getAnnotation(TableModelSpecWrapper modelSpec) {
                return check;
            }
        };
    }

    private ConstraintSqlAnnotationHandler.TableConstraintSqlAnnotationHandler
        getConstraintSqlHandler(final ConstraintSql constraintSql) {
        return new ConstraintSqlAnnotationHandler.TableConstraintSqlAnnotationHandler() {
            @Override
            protected ConstraintSql getAnnotation(TableModelSpecWrapper modelSpec) {
                return constraintSql;
            }
        };
    }

    @Override
    public Class<TableConstraints> getAnnotationClass() {
        return TableConstraints.class;
    }
}
