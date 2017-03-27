/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.plugins.defaults.constraints;

import com.yahoo.squidb.processor.data.ModelSpec;
import com.yahoo.squidb.processor.plugins.PluginEnvironment;
import com.yahoo.squidb.processor.plugins.defaults.properties.generators.interfaces.PropertyGenerator;

import java.lang.annotation.Annotation;

/**
 * Interface for classes that handle column constraint annotations
 * (e.g. {@link com.yahoo.squidb.annotations.tables.constraints.Unique} or
 * {@link com.yahoo.squidb.annotations.tables.constraints.NotNull})
 */
public interface ColumnConstraintAnnotationHandler<ANNOTATION extends Annotation> {

    /**
     * @return the annotation class handled by this handler
     */
    Class<ANNOTATION> getAnnotationClass();

    /**
     * Builds and appends the SQL constraint for the handler's annotation for the given column (represented by a
     * {@link PropertyGenerator}).
     */
    void appendConstraintForColumn(StringBuilder constraintBuilder, PropertyGenerator propertyGenerator,
            PluginEnvironment pluginEnvironment);

    /**
     * Validates the annotation parameters for the handler's annotation for the given column and model spec
     */
    void validateAnnotationForColumn(PropertyGenerator propertyGenerator, ModelSpec<?, ?> modelSpec,
            PluginEnvironment pluginEnvironment);

}
