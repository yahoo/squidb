/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.plugins.defaults.constraints;

import com.yahoo.squidb.processor.data.ModelSpec;
import com.yahoo.squidb.processor.plugins.PluginEnvironment;
import com.yahoo.squidb.processor.plugins.defaults.properties.generators.interfaces.PropertyGenerator;

import javax.annotation.Nonnull;
import javax.tools.Diagnostic;

/**
 * Handler for {@link Nonnull} annotation. This is provided mostly as a concession because some users will probably
 * use this by accident when they really mean to use {@link com.yahoo.squidb.annotations.tables.constraints.NotNull}.
 * We will treat it as though they meant NotNull with no conflict resolution, but log a warning when it is used that
 * way.
 */
public class JavaxNonnullAnnotationHandler extends AbstractColumnConstraintAnnotationHandler<Nonnull> {

    @Override
    public Class<Nonnull> getAnnotationClass() {
        return Nonnull.class;
    }

    @Override
    protected void appendConstraintStringForColumn(StringBuilder constraintBuilder, Nonnull annotation,
            PropertyGenerator propertyGenerator, PluginEnvironment pluginEnvironment) {
        if (!constraintBuilder.toString().toUpperCase().contains("NOT NULL")) {
            constraintBuilder.append(" NOT NULL");
        }
    }

    @Override
    protected void validateAnnotationForColumn(Nonnull annotation, PropertyGenerator propertyGenerator,
            ModelSpec<?, ?> modelSpec, PluginEnvironment pluginEnvironment) {
        pluginEnvironment.getMessager().printMessage(Diagnostic.Kind.WARNING, "Use "
                + "com.yahoo.squidb.annotations.tables.constraints.NotNull to specify a NOT NULL constraint for this "
                + "column instead of javax.annotation.Nonnull.", propertyGenerator.getField());
    }
}
