/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.json;

import com.yahoo.aptutils.model.DeclaredTypeName;
import com.yahoo.squidb.processor.data.InheritedModelSpecWrapper;
import com.yahoo.squidb.processor.data.ModelSpec;
import com.yahoo.squidb.processor.plugins.PluginEnvironment;
import com.yahoo.squidb.processor.plugins.defaults.properties.generators.interfaces.InheritedModelPropertyGenerator;

import javax.lang.model.element.VariableElement;

/**
 * Plugin which handles JSON fields in an InheritedModelSpec file.
 */
public class JSONInheritedPropertyPlugin
        extends JSONPropertyReferencePlugin<InheritedModelSpecWrapper, InheritedModelPropertyGenerator> {

    public JSONInheritedPropertyPlugin(ModelSpec<?, ?> modelSpec, PluginEnvironment pluginEnv) {
        super(modelSpec, pluginEnv);
    }

    @Override
    protected Class<InheritedModelSpecWrapper> getHandledModelSpecClass() {
        return InheritedModelSpecWrapper.class;
    }

    @Override
    protected InheritedModelPropertyGenerator getTypedJsonPropertyGenerator(VariableElement field,
            DeclaredTypeName propertyType) {
        return new JSONPropertyReferencePropertyGenerator(modelSpec, field, propertyType, utils);
    }
}
