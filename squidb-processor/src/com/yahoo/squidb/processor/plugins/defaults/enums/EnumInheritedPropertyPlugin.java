/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.plugins.defaults.enums;

import com.squareup.javapoet.ParameterizedTypeName;
import com.yahoo.squidb.processor.data.InheritedModelSpecWrapper;
import com.yahoo.squidb.processor.plugins.defaults.properties.generators.EnumPropertyReferencePropertyGenerator;
import com.yahoo.squidb.processor.plugins.defaults.properties.generators.interfaces.InheritedModelPropertyGenerator;

import javax.lang.model.element.VariableElement;

/**
 * Plugin which handles Enum fields in an InheritedModelSpec file.
 */
public class EnumInheritedPropertyPlugin
        extends EnumPropertyReferencePlugin<InheritedModelSpecWrapper, InheritedModelPropertyGenerator> {

    @Override
    protected Class<InheritedModelSpecWrapper> getHandledModelSpecClass() {
        return InheritedModelSpecWrapper.class;
    }

    @Override
    protected InheritedModelPropertyGenerator getTypedEnumPropertyGenerator(VariableElement field,
            ParameterizedTypeName propertyType) {
        return new EnumPropertyReferencePropertyGenerator(modelSpec, field, propertyType, pluginEnv);
    }

}
