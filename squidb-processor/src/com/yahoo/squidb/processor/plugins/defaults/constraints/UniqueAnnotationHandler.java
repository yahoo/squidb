/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.plugins.defaults.constraints;

import com.yahoo.squidb.annotations.tables.constraints.Unique;
import com.yahoo.squidb.processor.plugins.PluginEnvironment;
import com.yahoo.squidb.processor.plugins.defaults.properties.generators.interfaces.PropertyGenerator;

/**
 * Handler for {@link Unique} annotation
 */
public class UniqueAnnotationHandler extends AbstractColumnConstraintAnnotationHandler<Unique> {

    @Override
    public Class<Unique> getAnnotationClass() {
        return Unique.class;
    }

    @Override
    protected void appendConstraintStringForColumn(StringBuilder constraintBuilder, Unique annotation,
            PropertyGenerator propertyGenerator, PluginEnvironment pluginEnvironment) {
        if (!constraintBuilder.toString().toUpperCase().contains("UNIQUE")) {
            constraintBuilder.append(" UNIQUE");
            appendOnConflict(constraintBuilder, annotation.onConflict());
        } else {
            logDuplicateConstraintWarning("UNIQUE", propertyGenerator, pluginEnvironment);
        }
    }
}
