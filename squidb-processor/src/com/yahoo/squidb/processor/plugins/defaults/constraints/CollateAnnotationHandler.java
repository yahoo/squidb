/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.plugins.defaults.constraints;

import com.yahoo.squidb.annotations.tables.constraints.Collate;
import com.yahoo.squidb.processor.StringUtils;
import com.yahoo.squidb.processor.data.ModelSpec;
import com.yahoo.squidb.processor.plugins.PluginEnvironment;
import com.yahoo.squidb.processor.plugins.defaults.properties.generators.interfaces.PropertyGenerator;

/**
 * Handler for {@link Collate} annotation
 */
public class CollateAnnotationHandler extends AbstractColumnConstraintAnnotationHandler<Collate> {

    @Override
    public Class<Collate> getAnnotationClass() {
        return Collate.class;
    }

    @Override
    protected void appendConstraintStringForColumn(StringBuilder constraintBuilder, Collate annotation,
            PropertyGenerator propertyGenerator, PluginEnvironment pluginEnv) {
        if (!constraintBuilder.toString().toUpperCase().contains("COLLATE")) {
            constraintBuilder.append(" COLLATE ").append(annotation.value().trim());
        } else {
            logDuplicateConstraintWarning("COLLATE", propertyGenerator, pluginEnv);
        }
    }

    @Override
    protected void validateAnnotationForColumn(Collate annotation, PropertyGenerator propertyGenerator,
            ModelSpec<?, ?> modelSpec, PluginEnvironment pluginEnvironment) {
        if (StringUtils.isEmpty(annotation.value().trim())) {
            modelSpec.logError("@Collate annotation cannot have an empty collation sequence name",
                    propertyGenerator.getField());
        }
    }
}
