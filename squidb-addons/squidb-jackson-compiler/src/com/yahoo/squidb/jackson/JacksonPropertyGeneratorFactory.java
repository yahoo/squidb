/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.jackson;

import com.yahoo.aptutils.model.DeclaredTypeName;
import com.yahoo.aptutils.utils.AptUtils;
import com.yahoo.squidb.processor.data.ModelSpec;
import com.yahoo.squidb.processor.plugins.Plugin;
import com.yahoo.squidb.processor.plugins.properties.generators.PropertyGenerator;

import javax.lang.model.element.VariableElement;

public class JacksonPropertyGeneratorFactory extends Plugin {

    public JacksonPropertyGeneratorFactory(AptUtils utils) {
        super(utils);
    }

    @Override
    public boolean hasPropertyGeneratorForField(ModelSpec<?> modelSpec, VariableElement field, DeclaredTypeName fieldType) {
        if (field.getAnnotation(JacksonProperty.class) == null) {
            return false;
        }
        if (fieldType.equals(JacksonTypeConstants.MAP)) {
            return fieldType.getTypeArgs().get(0) instanceof DeclaredTypeName &&
                    fieldType.getTypeArgs().get(1) instanceof DeclaredTypeName;
        } else if (fieldType.equals(JacksonTypeConstants.LIST)) {
            return fieldType.getTypeArgs().get(0) instanceof DeclaredTypeName;
        }
        return false;
    }

    @Override
    public PropertyGenerator getPropertyGenerator(ModelSpec<?> modelSpec, VariableElement field, DeclaredTypeName fieldType) {
        if (fieldType.equals(JacksonTypeConstants.MAP)) {
            return new JacksonMapPropertyGenerator(modelSpec, field, fieldType, utils);
        } else if (fieldType.equals(JacksonTypeConstants.LIST)) {
            return new JacksonListPropertyGenerator(modelSpec, field, fieldType, utils);
        }
        return null;
    }

}
