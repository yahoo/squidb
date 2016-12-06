/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.json;

import com.yahoo.aptutils.model.DeclaredTypeName;
import com.yahoo.squidb.processor.data.ModelSpec;
import com.yahoo.squidb.processor.data.ViewModelSpecWrapper;
import com.yahoo.squidb.processor.plugins.PluginEnvironment;
import com.yahoo.squidb.processor.plugins.defaults.properties.generators.interfaces.ViewModelPropertyGenerator;

import javax.lang.model.element.VariableElement;

/**
 * Plugin which handles JSON fields in a ViewModelSpec file.
 */
public class JSONViewPropertyPlugin
        extends JSONPropertyReferencePlugin<ViewModelSpecWrapper, ViewModelPropertyGenerator> {

    public JSONViewPropertyPlugin(ModelSpec<?, ?> modelSpec, PluginEnvironment pluginEnv) {
        super(modelSpec, pluginEnv);
    }

    @Override
    protected Class<ViewModelSpecWrapper> getHandledModelSpecClass() {
        return ViewModelSpecWrapper.class;
    }

    @Override
    protected ViewModelPropertyGenerator getTypedJsonPropertyGenerator(VariableElement field,
            DeclaredTypeName propertyType) {
        return new JSONPropertyReferencePropertyGenerator(modelSpec, field, propertyType, utils);
    }
}
