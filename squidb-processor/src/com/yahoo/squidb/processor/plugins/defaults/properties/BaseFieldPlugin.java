/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.plugins.defaults.properties;

import com.squareup.javapoet.TypeName;
import com.yahoo.squidb.processor.data.ModelSpec;
import com.yahoo.squidb.processor.plugins.AbstractPlugin;
import com.yahoo.squidb.processor.plugins.PluginEnvironment;
import com.yahoo.squidb.processor.plugins.defaults.properties.generators.interfaces.PropertyGenerator;

import javax.lang.model.element.VariableElement;

/**
 * Base abstract class for plugins that processes all variable elements in a model spec class to create
 * {@link PropertyGenerator} instances for the model class. Each subclass of this base class will only handle fields
 * in a single type of model spec.
 */
public abstract class BaseFieldPlugin<T extends ModelSpec<?, P>, P extends PropertyGenerator> extends AbstractPlugin {

    protected abstract Class<T> getHandledModelSpecClass();

    protected T modelSpec;

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(ModelSpec<?, ?> modelSpec, PluginEnvironment pluginEnv) {
        if (getHandledModelSpecClass() != null && getHandledModelSpecClass().isAssignableFrom(modelSpec.getClass())) {
            this.modelSpec = (T) modelSpec;
            return super.init(modelSpec, pluginEnv);
        }
        return false;
    }

    @Override
    public boolean processVariableElement(VariableElement field, TypeName fieldType) {
        return tryCreatePropertyGenerator(field, fieldType);
    }

    private boolean tryCreatePropertyGenerator(VariableElement field, TypeName fieldType) {
        if (hasPropertyGeneratorForField(field, fieldType)) {
            P generator = getPropertyGenerator(field, fieldType);
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

    protected abstract boolean hasPropertyGeneratorForField(VariableElement field, TypeName fieldType);

    protected abstract P getPropertyGenerator(VariableElement field, TypeName fieldType);
}
