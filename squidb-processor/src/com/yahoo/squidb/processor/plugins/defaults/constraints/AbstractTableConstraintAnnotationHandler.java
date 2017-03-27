/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.plugins.defaults.constraints;

import com.yahoo.squidb.processor.data.TableModelSpecWrapper;
import com.yahoo.squidb.processor.plugins.PluginEnvironment;

import java.lang.annotation.Annotation;

import javax.lang.model.element.TypeElement;

/**
 * Abstract base class for handling column constraint annotations like
 * {@link com.yahoo.squidb.annotations.tables.constraints.UniqueColumns},
 * {@link com.yahoo.squidb.annotations.tables.constraints.PrimaryKeyColumns}, etc.
 */
public abstract class AbstractTableConstraintAnnotationHandler<ANNOTATION extends Annotation>
        implements TableConstraintAnnotationHandler<ANNOTATION> {

    @Override
    public final void appendConstraintForTable(StringBuilder constraintBuilder, TableModelSpecWrapper modelSpec,
            PluginEnvironment pluginEnv) {
        ANNOTATION annotation = getAnnotation(modelSpec);
        if (annotation != null) {
            if (constraintBuilder.length() != 0) {
                constraintBuilder.append(", ");
            }
            appendConstraintStringForTable(constraintBuilder, annotation, modelSpec, pluginEnv);
        }
    }

    @Override
    public final void validateAnnotationForTable(TableModelSpecWrapper modelSpec, PluginEnvironment pluginEnvironment) {
        ANNOTATION annotation = getAnnotation(modelSpec);
        if (annotation != null) {
            if (modelSpec.isVirtualTable()) {
                modelSpec.logError("Virtual tables do not currently support table constraints; annotation @" +
                        getAnnotationClass().getSimpleName() + " should be removed", modelSpec.getModelSpecElement());
            }
            validateAnnotationForTable(annotation, modelSpec, pluginEnvironment);
        }
    }

    protected ANNOTATION getAnnotation(TableModelSpecWrapper modelSpec) {
        TypeElement modelSpecElement = modelSpec.getModelSpecElement();
        return modelSpecElement.getAnnotation(getAnnotationClass());
    }

    protected abstract void appendConstraintStringForTable(StringBuilder constraintBuilder, ANNOTATION annotation,
            TableModelSpecWrapper modelSpec, PluginEnvironment pluginEnvironment);

    protected abstract void validateAnnotationForTable(ANNOTATION annotation, TableModelSpecWrapper modelSpec,
            PluginEnvironment pluginEnvironment);
}
