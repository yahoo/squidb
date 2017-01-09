/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.json;

import com.squareup.javapoet.TypeName;
import com.yahoo.squidb.processor.data.ViewModelSpecWrapper;
import com.yahoo.squidb.processor.plugins.defaults.properties.generators.interfaces.ViewModelPropertyGenerator;

import javax.lang.model.element.VariableElement;

/**
 * Plugin which handles JSON fields in a ViewModelSpec file.
 */
public class JSONViewPropertyPlugin
        extends JSONPropertyReferencePlugin<ViewModelSpecWrapper, ViewModelPropertyGenerator> {

    @Override
    protected Class<ViewModelSpecWrapper> getHandledModelSpecClass() {
        return ViewModelSpecWrapper.class;
    }

    @Override
    protected ViewModelPropertyGenerator getTypedJsonPropertyGenerator(VariableElement field,
            TypeName propertyType) {
        return new JSONPropertyReferencePropertyGenerator(modelSpec, field, propertyType, pluginEnv);
    }
}
