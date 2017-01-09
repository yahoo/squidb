/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.json;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;

import java.util.List;

/**
 * Helper class containing logic that is common to JSON properties in all model types
 */
class JSONPropertyGeneratorDelegate {

    private final String propertyName;
    private final TypeName jsonType;

    JSONPropertyGeneratorDelegate(String propertyName, TypeName jsonType) {
        this.propertyName = propertyName;
        this.jsonType = jsonType;
    }

    void writeGetterBody(CodeBlock.Builder body) {
        body.addStatement("return $T.getValueFromJSON(this, $L, $L)", JSONTypes.JSON_PROPERTY_SUPPORT,
                propertyName, getTypeExpression(jsonType));
    }

    void writeSetterBody(CodeBlock.Builder body, String argName) {
        body.addStatement("$T.setValueAsJSON(this, $L, $L, $L)", JSONTypes.JSON_PROPERTY_SUPPORT, propertyName,
                argName, getTypeExpression(jsonType));
        body.addStatement("return this");
    }

    private CodeBlock getTypeExpression(TypeName fieldType) {
        if (fieldType instanceof ParameterizedTypeName) {
            CodeBlock.Builder block = CodeBlock.builder();
            block.add("$T.build($T.class", JSONTypes.PARAMETERIZED_TYPE_BUILDER,
                    ((ParameterizedTypeName) fieldType).rawType);
            List<TypeName> typeArgs = ((ParameterizedTypeName) fieldType).typeArguments;
            for (TypeName typeArg : typeArgs) {
                // The cast to DeclaredTypeName is safe because we recursively check all type args before constructing
                // an instance of this property generator
                block.add(", $L", getTypeExpression(typeArg));
            }
            block.add(")");
            return block.build();
        } else {
            return CodeBlock.of("$T.class", fieldType);
        }
    }
}
