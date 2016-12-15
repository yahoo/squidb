/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.plugins.defaults.enums;

import com.squareup.javapoet.ParameterizedTypeName;
import com.yahoo.squidb.processor.data.ViewModelSpecWrapper;
import com.yahoo.squidb.processor.plugins.defaults.properties.generators.EnumPropertyReferencePropertyGenerator;
import com.yahoo.squidb.processor.plugins.defaults.properties.generators.interfaces.ViewModelPropertyGenerator;

import javax.lang.model.element.VariableElement;

/**
 * Plugin which handles Enum fields in a ViewModelSpec file.
 */
public class EnumViewPropertyPlugin
        extends EnumPropertyReferencePlugin<ViewModelSpecWrapper, ViewModelPropertyGenerator> {

    @Override
    protected Class<ViewModelSpecWrapper> getHandledModelSpecClass() {
        return ViewModelSpecWrapper.class;
    }

    @Override
    protected ViewModelPropertyGenerator getTypedEnumPropertyGenerator(VariableElement field,
            ParameterizedTypeName propertyType) {
        return new EnumPropertyReferencePropertyGenerator(modelSpec, field, propertyType, pluginEnv);
    }
}
