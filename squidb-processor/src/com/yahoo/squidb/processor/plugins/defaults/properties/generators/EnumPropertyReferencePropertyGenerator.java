/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.plugins.defaults.properties.generators;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.yahoo.squidb.processor.data.ModelSpec;
import com.yahoo.squidb.processor.plugins.PluginEnvironment;

import javax.lang.model.element.VariableElement;

/**
 * Extension of {@link PropertyReferencePropertyGenerator} that handles enum property references
 */
public class EnumPropertyReferencePropertyGenerator extends PropertyReferencePropertyGenerator {

    private final EnumPropertyGeneratorDelegate delegate;

    public EnumPropertyReferencePropertyGenerator(ModelSpec<?, ?> modelSpec, VariableElement field,
            ParameterizedTypeName propertyType, PluginEnvironment pluginEnv) {
        super(modelSpec, field, propertyType, pluginEnv);
        this.delegate = new EnumPropertyGeneratorDelegate(getPropertyName(), getTypeForAccessors());
    }

    @Override
    protected TypeName initAccessorsType() {
        return ((ParameterizedTypeName) propertyType).typeArguments.get(0);
    }

    @Override
    protected void writeGetterBody(CodeBlock.Builder body, MethodSpec methodParams) {
        delegate.writeGetterBody(body);
    }

    @Override
    protected void writeSetterBody(CodeBlock.Builder body, MethodSpec methodParams) {
        delegate.writeSetterBody(body, methodParams.parameters.get(0).name);
    }
}
