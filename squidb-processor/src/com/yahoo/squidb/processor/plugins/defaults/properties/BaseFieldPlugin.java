/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.plugins.defaults.properties;

import com.yahoo.aptutils.model.DeclaredTypeName;
import com.yahoo.squidb.processor.data.ModelSpec;
import com.yahoo.squidb.processor.plugins.Plugin;
import com.yahoo.squidb.processor.plugins.PluginEnvironment;
import com.yahoo.squidb.processor.plugins.defaults.properties.generators.PropertyGenerator;

import javax.lang.model.element.VariableElement;

public abstract class BaseFieldPlugin extends Plugin {

    public BaseFieldPlugin(ModelSpec<?> modelSpec, PluginEnvironment pluginEnv) {
        super(modelSpec, pluginEnv);
    }

    @Override
    public abstract boolean hasChangesForModelSpec();

    @Override
    public boolean processVariableElement(VariableElement field, DeclaredTypeName fieldType) {
        return tryCreatePropertyGenerator(field, fieldType);
    }

    private boolean tryCreatePropertyGenerator(VariableElement field, DeclaredTypeName fieldType) {
        if (hasPropertyGeneratorForField(field, fieldType)) {
            PropertyGenerator generator = getPropertyGenerator(field, fieldType);
            if (generator != null) {
                if (generator.isDeprecated()) {
                    modelSpec.addDeprecatedPropertyGenerator(generator);
                } else {
                    modelSpec.addPropertyGenerator(generator);
                }
                return true;
            }
        }
        return false;
    }

    protected abstract boolean hasPropertyGeneratorForField(VariableElement field, DeclaredTypeName fieldType);

    protected abstract PropertyGenerator getPropertyGenerator(VariableElement field, DeclaredTypeName fieldType);
}
