/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.plugins.defaults.constraints;

import com.yahoo.squidb.annotations.tables.constraints.Check;
import com.yahoo.squidb.processor.StringUtils;
import com.yahoo.squidb.processor.data.ModelSpec;
import com.yahoo.squidb.processor.data.TableModelSpecWrapper;
import com.yahoo.squidb.processor.plugins.PluginEnvironment;
import com.yahoo.squidb.processor.plugins.defaults.properties.generators.interfaces.PropertyGenerator;

import javax.lang.model.element.Element;

/**
 * Handler for {@link Check} annotation
 */
public class CheckAnnotationHandler {

    private static void validateAnnotation(Check annotation, Element element, ModelSpec<?, ?> modelSpec) {
        if (StringUtils.isEmpty(annotation.value().trim())) {
            modelSpec.logError("@Check annotation cannot have an empty expression", element);
        }
    }

    private static String getCheckExpression(Check annotation) {
        String checkExpression = annotation.value().trim();
        if (!checkExpression.startsWith("(") || !checkExpression.endsWith(")")) {
            checkExpression = "(" + checkExpression + ")";
        }
        return "CHECK" + checkExpression;
    }

    public static class ColumnCheckAnnotationHandler extends AbstractColumnConstraintAnnotationHandler<Check> {
        @Override
        protected void appendConstraintStringForColumn(StringBuilder constraintBuilder, Check annotation,
                PropertyGenerator propertyGenerator, PluginEnvironment pluginEnvironment) {
            constraintBuilder.append(" ").append(getCheckExpression(annotation));
        }

        @Override
        protected void validateAnnotationForColumn(Check annotation, PropertyGenerator propertyGenerator,
                ModelSpec<?, ?> modelSpec, PluginEnvironment pluginEnvironment) {
            validateAnnotation(annotation, propertyGenerator.getField(), modelSpec);
        }

        @Override
        public Class<Check> getAnnotationClass() {
            return Check.class;
        }
    }

    public static class TableCheckAnnotationHandler extends AbstractTableConstraintAnnotationHandler<Check> {
        @Override
        protected void appendConstraintStringForTable(StringBuilder constraintBuilder, Check annotation,
                TableModelSpecWrapper modelSpec, PluginEnvironment pluginEnvironment) {
            constraintBuilder.append(getCheckExpression(annotation));
        }

        @Override
        protected void validateAnnotationForTable(Check annotation, TableModelSpecWrapper modelSpec,
                PluginEnvironment pluginEnvironment) {
            validateAnnotation(annotation, modelSpec.getModelSpecElement(), modelSpec);
        }

        @Override
        public Class<Check> getAnnotationClass() {
            return Check.class;
        }
    }
}
