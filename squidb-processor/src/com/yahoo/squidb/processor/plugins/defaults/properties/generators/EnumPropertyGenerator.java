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
import com.yahoo.squidb.processor.TypeConstants;
import com.yahoo.squidb.processor.data.ModelSpec;
import com.yahoo.squidb.processor.plugins.PluginEnvironment;

import javax.lang.model.element.VariableElement;

/**
 * Property generator for Enum fields in a table model spec.
 */
public class EnumPropertyGenerator extends BasicStringPropertyGenerator {

    private final TypeName enumType;
    private final EnumPropertyGeneratorDelegate delegate;

    public EnumPropertyGenerator(ModelSpec<?, ?> modelSpec, VariableElement field, PluginEnvironment pluginEnv,
            TypeName enumType) {
        super(modelSpec, field, pluginEnv);
        this.enumType = enumType;
        this.delegate = new EnumPropertyGeneratorDelegate(getPropertyName(), getTypeForAccessors());
    }

    @Override
    public TypeName getPropertyType() {
        return ParameterizedTypeName.get(TypeConstants.ENUM_PROPERTY, enumType);
    }

    @Override
    public TypeName getTypeForAccessors() {
        return enumType;
    }

    @Override
    protected void writeGetterBody(CodeBlock.Builder body, MethodSpec methodParams) {
        delegate.writeGetterBody(body);
    }

    @Override
    protected void writeSetterBody(CodeBlock.Builder params, MethodSpec methodParams) {
        delegate.writeSetterBody(params, methodParams.parameters.get(0).name);
    }
}
