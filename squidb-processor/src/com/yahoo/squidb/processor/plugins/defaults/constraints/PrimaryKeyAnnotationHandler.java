/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.plugins.defaults.constraints;

import com.yahoo.squidb.annotations.tables.IndexOrder;
import com.yahoo.squidb.annotations.tables.constraints.PrimaryKey;
import com.yahoo.squidb.processor.TypeConstants;
import com.yahoo.squidb.processor.plugins.PluginEnvironment;
import com.yahoo.squidb.processor.plugins.defaults.properties.generators.interfaces.PropertyGenerator;

/**
 * Handler for {@link PrimaryKey} annotation
 */
public class PrimaryKeyAnnotationHandler extends AbstractColumnConstraintAnnotationHandler<PrimaryKey> {

    @Override
    public Class<PrimaryKey> getAnnotationClass() {
        return PrimaryKey.class;
    }

    @Override
    protected void appendConstraintStringForColumn(StringBuilder constraintBuilder, PrimaryKey annotation,
            PropertyGenerator propertyGenerator, PluginEnvironment pluginEnvironment) {
        if (!constraintBuilder.toString().toUpperCase().contains("PRIMARY KEY")) {
            constraintBuilder.append(" PRIMARY KEY");
            if (annotation.order() != IndexOrder.UNSPECIFIED) {
                constraintBuilder.append(" ").append(annotation.order().name());
            }
            appendOnConflict(constraintBuilder, annotation.onConflict());
            if (TypeConstants.isIntegerType(propertyGenerator.getTypeForAccessors()) &&
                    annotation.order() != IndexOrder.DESC &&
                    annotation.autoincrement()) {
                constraintBuilder.append(" AUTOINCREMENT");
            }
        } else {
            logDuplicateConstraintWarning("PRIMARY KEY", propertyGenerator, pluginEnvironment);
        }
    }
}
