/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.plugins.defaults.constraints;

import com.yahoo.squidb.annotations.tables.constraints.ConstraintSql;
import com.yahoo.squidb.processor.StringUtils;
import com.yahoo.squidb.processor.data.ModelSpec;
import com.yahoo.squidb.processor.data.TableModelSpecWrapper;
import com.yahoo.squidb.processor.plugins.PluginEnvironment;
import com.yahoo.squidb.processor.plugins.defaults.properties.generators.interfaces.PropertyGenerator;

import javax.lang.model.element.Element;

/**
 * Handler for {@link ConstraintSql} annotation
 */
public class ConstraintSqlAnnotationHandler {

    private static void validateAnnotation(ConstraintSql annotation, Element element, ModelSpec<?, ?> modelSpec) {
        if (StringUtils.isEmpty(getConstraintSqlExpression(annotation))) {
            modelSpec.logError("@ConstraintSql annotation cannot have an empty expression", element);
        }
    }

    private static String getConstraintSqlExpression(ConstraintSql annotation) {
        return annotation.value().trim();
    }

    public static class ColumnConstraintSqlAnnotationHandler
            extends AbstractColumnConstraintAnnotationHandler<ConstraintSql> {
        @Override
        protected void appendConstraintStringForColumn(StringBuilder constraintBuilder, ConstraintSql annotation,
                PropertyGenerator propertyGenerator, PluginEnvironment pluginEnvironment) {
            constraintBuilder.append(getConstraintSqlExpression(annotation));
        }

        @Override
        protected void validateAnnotationForColumn(ConstraintSql annotation,
                PropertyGenerator propertyGenerator, ModelSpec<?, ?> modelSpec, PluginEnvironment pluginEnvironment) {
            validateAnnotation(annotation, propertyGenerator.getField(), modelSpec);
        }

        @Override
        public Class<ConstraintSql> getAnnotationClass() {
            return ConstraintSql.class;
        }
    }

    public static class TableConstraintSqlAnnotationHandler
            extends AbstractTableConstraintAnnotationHandler<ConstraintSql> {
        @Override
        protected void appendConstraintStringForTable(StringBuilder constraintBuilder, ConstraintSql annotation,
                TableModelSpecWrapper modelSpec, PluginEnvironment pluginEnvironment) {
            constraintBuilder.append(getConstraintSqlExpression(annotation));
        }

        @Override
        protected void validateAnnotationForTable(ConstraintSql annotation, TableModelSpecWrapper modelSpec,
                PluginEnvironment pluginEnvironment) {
            validateAnnotation(annotation, modelSpec.getModelSpecElement(), modelSpec);
        }

        @Override
        public Class<ConstraintSql> getAnnotationClass() {
            return ConstraintSql.class;
        }
    }
}
