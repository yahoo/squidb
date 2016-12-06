/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.plugins.defaults.enums;

import com.yahoo.aptutils.model.DeclaredTypeName;
import com.yahoo.squidb.processor.data.ModelSpec;
import com.yahoo.squidb.processor.data.ViewModelSpecWrapper;
import com.yahoo.squidb.processor.plugins.PluginEnvironment;
import com.yahoo.squidb.processor.plugins.defaults.properties.generators.EnumPropertyReferencePropertyGenerator;
import com.yahoo.squidb.processor.plugins.defaults.properties.generators.interfaces.ViewModelPropertyGenerator;

import javax.lang.model.element.VariableElement;

/**
 * Plugin which handles Enum fields in a ViewModelSpec file.
 */
public class EnumViewPropertyPlugin
        extends EnumPropertyReferencePlugin<ViewModelSpecWrapper, ViewModelPropertyGenerator> {

    public EnumViewPropertyPlugin(ModelSpec<?, ?> modelSpec, PluginEnvironment pluginEnv) {
        super(modelSpec, pluginEnv);
    }

    @Override
    protected Class<ViewModelSpecWrapper> getHandledModelSpecClass() {
        return ViewModelSpecWrapper.class;
    }

    @Override
    protected ViewModelPropertyGenerator getTypedEnumPropertyGenerator(VariableElement field,
            DeclaredTypeName propertyType) {
        return new EnumPropertyReferencePropertyGenerator(modelSpec, field, propertyType, utils);
    }
}
