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
import com.yahoo.squidb.processor.plugins.defaults.properties.generators.interfaces.PropertyGenerator;

import javax.lang.model.element.VariableElement;

/**
 * Base abstract class for plugins that processes all variable elements in a model spec class to create
 * {@link PropertyGenerator} instances for the model class. Each subclass of this base class will only handle fields
 * in a single type of model spec.
 */
public abstract class BaseFieldPlugin<T extends ModelSpec<?, P>, P extends PropertyGenerator> extends Plugin {

    public BaseFieldPlugin(ModelSpec<?, ?> modelSpec, PluginEnvironment pluginEnv) {
        super(modelSpec, pluginEnv);
    }

    protected abstract Class<T> getHandledModelSpecClass();

    @Override
    public final boolean hasChangesForModelSpec() {
        return getHandledModelSpecClass() != null && getHandledModelSpecClass().isAssignableFrom(modelSpec.getClass());
    }

    @Override
    public boolean processVariableElement(VariableElement field, DeclaredTypeName fieldType) {
        return tryCreatePropertyGenerator(field, fieldType);
    }

    @SuppressWarnings("unchecked")
    private boolean tryCreatePropertyGenerator(VariableElement field, DeclaredTypeName fieldType) {
        if (hasPropertyGeneratorForField(field, fieldType)) {
            P generator = getPropertyGenerator(field, fieldType);
            if (generator != null) {
                if (generator.isDeprecated()) {
                    ((ModelSpec<?, P>) modelSpec).addDeprecatedPropertyGenerator(generator);
                } else {
                    ((ModelSpec<?, P>) modelSpec).addPropertyGenerator(generator);
                }
                return true;
            }
        }
        return false;
    }

    protected abstract boolean hasPropertyGeneratorForField(VariableElement field, DeclaredTypeName fieldType);

    protected abstract P getPropertyGenerator(VariableElement field, DeclaredTypeName fieldType);
}
