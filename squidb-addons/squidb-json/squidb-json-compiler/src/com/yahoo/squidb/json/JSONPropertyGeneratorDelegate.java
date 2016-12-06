/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.json;

import com.yahoo.aptutils.model.DeclaredTypeName;
import com.yahoo.aptutils.model.TypeName;
import com.yahoo.aptutils.utils.AptUtils;
import com.yahoo.aptutils.visitors.ImportGatheringTypeNameVisitor;
import com.yahoo.aptutils.writer.JavaFileWriter;
import com.yahoo.aptutils.writer.expressions.Expression;
import com.yahoo.aptutils.writer.expressions.Expressions;
import com.yahoo.aptutils.writer.parameters.MethodDeclarationParameters;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Helper class containing logic that is common to JSON properties in all model types
 */
class JSONPropertyGeneratorDelegate {

    private final String propertyName;
    private final DeclaredTypeName jsonType;

    JSONPropertyGeneratorDelegate(String propertyName, DeclaredTypeName jsonType) {
        this.propertyName = propertyName;
        this.jsonType = jsonType;
    }

    void registerRequiredImports(Set<DeclaredTypeName> imports) {
        imports.add(JSONTypes.JSON_PROPERTY_SUPPORT);
        imports.add(JSONTypes.JSON_PROPERTY);
        if (!AptUtils.isEmpty(jsonType.getTypeArgs())) {
            imports.add(JSONTypes.PARAMETERIZED_TYPE_BUILDER);
        }
        jsonType.accept(new ImportGatheringTypeNameVisitor(), imports);
    }

    void writeGetterBody(JavaFileWriter writer, MethodDeclarationParameters params) throws IOException {
        Expression typeExpression = getTypeExpression(jsonType);

        writer.writeStatement(Expressions.staticMethod(JSONTypes.JSON_PROPERTY_SUPPORT, "getValueFromJSON",
                "this", propertyName, typeExpression).returnExpr());
    }

    void writeSetterBody(JavaFileWriter writer, MethodDeclarationParameters params) throws IOException {
        Expression typeExpression = getTypeExpression(jsonType);

        writer.writeStatement(Expressions.staticMethod(JSONTypes.JSON_PROPERTY_SUPPORT, "setValueAsJSON",
                "this", propertyName, params.getArgumentNames().get(0), typeExpression));
        writer.writeStringStatement("return this");
    }

    private Expression getTypeExpression(DeclaredTypeName fieldType) {
        List<? extends TypeName> typeArgs = fieldType.getTypeArgs();
        if (AptUtils.isEmpty(typeArgs)) {
            return Expressions.classObject(fieldType);
        } else {
            List<Expression> parameterizedTypeBuilderArgs = new ArrayList<>();
            parameterizedTypeBuilderArgs.add(Expressions.classObject(fieldType));
            for (TypeName typeArg : typeArgs) {
                // The cast to DeclaredTypeName is safe because we recursively check all type args before constructing
                // an instance of this property generator
                parameterizedTypeBuilderArgs.add(getTypeExpression((DeclaredTypeName) typeArg));
            }
            return Expressions.staticMethod(JSONTypes.PARAMETERIZED_TYPE_BUILDER, "build",
                    parameterizedTypeBuilderArgs);
        }
    }
}
