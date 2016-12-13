/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.plugins.defaults.properties.generators;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;

/**
 * Helper class containing logic that is common to enum properties in all model types
 */
class EnumPropertyGeneratorDelegate {

    private final String propertyName;
    private final TypeName enumType;

    EnumPropertyGeneratorDelegate(String propertyName, TypeName enumType) {
        this.propertyName = propertyName;
        this.enumType = enumType;
    }

    void writeGetterBody(CodeBlock.Builder body) {
        body.addStatement("$T value = get($L)", String.class, propertyName);
        body.addStatement("return value == null ? null : $T.valueOf(value)", enumType);
    }

    void writeSetterBody(CodeBlock.Builder body, String argName) {
        String argAsString = argName + "AsString";
        body.addStatement("$T $L = ($L == null ? null : $L.name())", String.class, argAsString, argName, argName);
        body.addStatement("set($L, $L)", propertyName, argAsString);
        body.addStatement("return this");
    }
}
