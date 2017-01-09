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
import com.yahoo.squidb.processor.plugins.defaults.properties.generators.BasicStringPropertyGenerator;

import javax.lang.model.element.VariableElement;

/**
 * Property generator for @JSONColumn annotated fields in a table model spec.
 */
public class JSONPropertyGenerator extends BasicStringPropertyGenerator {

    private final TypeName fieldType;
    private final JSONPropertyGeneratorDelegate delegate;

    public JSONPropertyGenerator(ModelSpec<?, ?> modelSpec, VariableElement field, TypeName fieldType,
            PluginEnvironment pluginEnv) {
        super(modelSpec, field, pluginEnv);
        this.fieldType = fieldType;
        this.delegate = new JSONPropertyGeneratorDelegate(getPropertyName(), getTypeForAccessors());
    }

    @Override
    public TypeName getPropertyType() {
        return ParameterizedTypeName.get(JSONTypes.JSON_PROPERTY, fieldType);
    }

    @Override
    public TypeName getTypeForAccessors() {
        return fieldType;
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
