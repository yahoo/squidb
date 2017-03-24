/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.plugins.defaults.constraints;

import com.yahoo.squidb.annotations.tables.ConflictAlgorithm;
import com.yahoo.squidb.processor.data.ModelSpec;
import com.yahoo.squidb.processor.plugins.PluginEnvironment;
import com.yahoo.squidb.processor.plugins.defaults.properties.generators.interfaces.PropertyGenerator;

import java.lang.annotation.Annotation;

import javax.lang.model.element.VariableElement;
import javax.tools.Diagnostic;

/**
 * Abstract base class for handling column constraint annotations like
 * {@link com.yahoo.squidb.annotations.tables.constraints.Unique},
 * {@link com.yahoo.squidb.annotations.tables.constraints.PrimaryKey}, etc.
 */
public abstract class AbstractColumnConstraintAnnotationHandler<ANNOTATION extends Annotation>
        implements ColumnConstraintAnnotationHandler<ANNOTATION> {

    protected static final String DUPLICATE_CONSTRAINT_WARNING = "Duplicate %s constraint definition in column " +
            "constraints. Use the @%s annotation instead of declaring the constraint in @ConstraintSql.";

    @Override
    public final void appendConstraintForColumn(StringBuilder constraintBuilder, PropertyGenerator propertyGenerator,
            PluginEnvironment pluginEnv) {
        VariableElement field = propertyGenerator.getField();
        if (field != null) {
            ANNOTATION annotation = field.getAnnotation(getAnnotationClass());
            if (annotation != null) {
                appendConstraintStringForColumn(constraintBuilder, annotation, propertyGenerator, pluginEnv);
            }
        }
    }

    @Override
    public final void validateAnnotationForColumn(PropertyGenerator propertyGenerator, ModelSpec<?, ?> modelSpec,
            PluginEnvironment pluginEnvironment) {
        VariableElement field = propertyGenerator.getField();
        if (field != null) {
            ANNOTATION annotation = field.getAnnotation(getAnnotationClass());
            if (annotation != null) {
                validateAnnotationForColumn(annotation, propertyGenerator, modelSpec, pluginEnvironment);
            }
        }
    }

    protected abstract void appendConstraintStringForColumn(StringBuilder constraintBuilder, ANNOTATION annotation,
            PropertyGenerator propertyGenerator, PluginEnvironment pluginEnvironment);

    protected void validateAnnotationForColumn(ANNOTATION annotation, PropertyGenerator propertyGenerator,
            ModelSpec<?, ?> modelSpec, PluginEnvironment pluginEnvironment) {
        // Subclasses can override
    }

    protected void appendOnConflict(StringBuilder constraintBuilder, ConflictAlgorithm onConflict) {
        if (onConflict != null && onConflict != ConflictAlgorithm.NONE) {
            constraintBuilder.append(" ON CONFLICT ").append(onConflict.name());
        }
    }

    protected void logDuplicateConstraintWarning(String constraintName, PropertyGenerator propertyGenerator,
            PluginEnvironment pluginEnv) {
        pluginEnv.getMessager().printMessage(Diagnostic.Kind.WARNING,
                String.format(DUPLICATE_CONSTRAINT_WARNING, constraintName, getAnnotationClass().getSimpleName()),
                propertyGenerator.getField());
    }
}
