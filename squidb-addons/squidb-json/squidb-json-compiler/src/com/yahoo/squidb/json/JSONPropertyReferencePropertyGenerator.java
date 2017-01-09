/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.json;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.yahoo.squidb.processor.data.ModelSpec;
import com.yahoo.squidb.processor.plugins.PluginEnvironment;
import com.yahoo.squidb.processor.plugins.defaults.properties.generators.PropertyReferencePropertyGenerator;

import javax.lang.model.element.VariableElement;

/**
 * Extension of {@link PropertyReferencePropertyGenerator} that handles JSON property references
 */
public class JSONPropertyReferencePropertyGenerator extends PropertyReferencePropertyGenerator {

    private final JSONPropertyGeneratorDelegate delegate;

    public JSONPropertyReferencePropertyGenerator(ModelSpec<?, ?> modelSpec, VariableElement field,
            TypeName propertyType, PluginEnvironment pluginEnv) {
        super(modelSpec, field, propertyType, pluginEnv);
        this.delegate = new JSONPropertyGeneratorDelegate(getPropertyName(), getTypeForAccessors());
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
