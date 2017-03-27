/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.plugins.defaults.constraints;

import com.yahoo.squidb.processor.data.TableModelSpecWrapper;
import com.yahoo.squidb.processor.plugins.PluginEnvironment;

import java.lang.annotation.Annotation;

/**
 * Interface for classes that handle constraint annotations
 * (e.g. {@link com.yahoo.squidb.annotations.tables.constraints.Unique} or
 * {@link com.yahoo.squidb.annotations.tables.constraints.NotNull})
 */
public interface TableConstraintAnnotationHandler<ANNOTATION extends Annotation> {

    /**
     * @return the annotation class handled by this handler
     */
    Class<ANNOTATION> getAnnotationClass();

    /**
     * Builds and appends the SQL constraint for the handler's annotation for the given table (represented by a
     * {@link TableModelSpecWrapper}).
     */
    void appendConstraintForTable(StringBuilder constraintBuilder, TableModelSpecWrapper modelSpec,
            PluginEnvironment pluginEnvironment);

    /**
     * Validates the annotation parameters for the handler's annotation for the given model spec
     */
    void validateAnnotationForTable(TableModelSpecWrapper modelSpec, PluginEnvironment pluginEnvironment);

}
