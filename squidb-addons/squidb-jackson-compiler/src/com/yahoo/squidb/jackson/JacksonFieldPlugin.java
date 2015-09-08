/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.jackson;

import com.yahoo.aptutils.model.DeclaredTypeName;
import com.yahoo.squidb.processor.data.ModelSpec;
import com.yahoo.squidb.processor.plugins.PluginEnvironment;
import com.yahoo.squidb.processor.plugins.defaults.properties.TableModelSpecFieldPlugin;
import com.yahoo.squidb.processor.plugins.defaults.properties.generators.PropertyGenerator;

import javax.lang.model.element.VariableElement;

public class JacksonFieldPlugin extends TableModelSpecFieldPlugin {

    public JacksonFieldPlugin(ModelSpec<?> modelSpec, PluginEnvironment pluginEnv) {
        super(modelSpec, pluginEnv);
    }

    @Override
    public boolean processVariableElement(VariableElement field, DeclaredTypeName fieldType) {
        if (field.getAnnotation(JacksonProperty.class) == null) {
            return false;
        }
        return super.processVariableElement(field, fieldType);
    }

    @Override
    protected boolean hasPropertyGeneratorForField(VariableElement field, DeclaredTypeName fieldType) {
        if (fieldType.equals(JacksonTypeConstants.MAP)) {
            return fieldType.getTypeArgs().get(0) instanceof DeclaredTypeName &&
                    fieldType.getTypeArgs().get(1) instanceof DeclaredTypeName;
        } else if (fieldType.equals(JacksonTypeConstants.LIST)) {
            return fieldType.getTypeArgs().get(0) instanceof DeclaredTypeName;
        }
        return false;
    }

    @Override
    protected PropertyGenerator getPropertyGenerator(VariableElement field, DeclaredTypeName fieldType) {
        if (fieldType.equals(JacksonTypeConstants.MAP)) {
            return new JacksonMapPropertyGenerator(modelSpec, field, fieldType, utils);
        } else if (fieldType.equals(JacksonTypeConstants.LIST)) {
            return new JacksonListPropertyGenerator(modelSpec, field, fieldType, utils);
        }
        return null;
    }

}
