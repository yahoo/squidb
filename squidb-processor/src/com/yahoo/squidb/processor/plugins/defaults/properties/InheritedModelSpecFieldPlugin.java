/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.plugins.defaults.properties;

import com.squareup.javapoet.TypeName;
import com.yahoo.squidb.processor.data.InheritedModelSpecWrapper;
import com.yahoo.squidb.processor.plugins.defaults.properties.generators.PropertyReferencePropertyGenerator;
import com.yahoo.squidb.processor.plugins.defaults.properties.generators.interfaces.InheritedModelPropertyGenerator;

import javax.lang.model.element.VariableElement;

/**
 * This plugin controls generating property declarations, getters, and setters for fields in an inherited model. It can
 * create instances of {@link InheritedModelPropertyGenerator} for references to other Property subclasses
 * (StringProperty, LongProperty, etc.)
 */
public class InheritedModelSpecFieldPlugin extends
        PropertyReferencePlugin<InheritedModelSpecWrapper, InheritedModelPropertyGenerator> {

    @Override
    protected Class<InheritedModelSpecWrapper> getHandledModelSpecClass() {
        return InheritedModelSpecWrapper.class;
    }

    @Override
    protected InheritedModelPropertyGenerator getPropertyGenerator(VariableElement field, TypeName fieldType) {
        return new PropertyReferencePropertyGenerator(modelSpec, field, fieldType, pluginEnv);
    }
}
