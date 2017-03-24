/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.plugins.defaults.constraints;

import com.yahoo.squidb.annotations.tables.constraints.NotNull;
import com.yahoo.squidb.processor.plugins.PluginEnvironment;
import com.yahoo.squidb.processor.plugins.defaults.properties.generators.interfaces.PropertyGenerator;

/**
 * Handler for {@link NotNull} annotation
 */
public class NotNullAnnotationHandler extends AbstractColumnConstraintAnnotationHandler<NotNull> {

    @Override
    public Class<NotNull> getAnnotationClass() {
        return NotNull.class;
    }

    @Override
    protected void appendConstraintStringForColumn(StringBuilder constraintBuilder, NotNull annotation,
            PropertyGenerator propertyGenerator, PluginEnvironment pluginEnvironment) {
        if (!constraintBuilder.toString().toUpperCase().contains("NOT NULL")) {
            constraintBuilder.append(" NOT NULL");
            appendOnConflict(constraintBuilder, annotation.onConflict());
        } else {
            logDuplicateConstraintWarning("NOT NULL", propertyGenerator, pluginEnvironment);
        }
    }
}
