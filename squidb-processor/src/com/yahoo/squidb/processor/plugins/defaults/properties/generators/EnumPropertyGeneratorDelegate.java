/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.plugins.defaults.properties.generators;

import com.yahoo.aptutils.model.CoreTypes;
import com.yahoo.aptutils.model.DeclaredTypeName;
import com.yahoo.aptutils.writer.JavaFileWriter;
import com.yahoo.aptutils.writer.expressions.Expression;
import com.yahoo.aptutils.writer.expressions.Expressions;
import com.yahoo.aptutils.writer.parameters.MethodDeclarationParameters;

import java.io.IOException;
import java.util.Set;

/**
 * Helper class containing logic that is common to enum properties in all model types
 */
class EnumPropertyGeneratorDelegate {

    private final String propertyName;
    private final DeclaredTypeName enumType;

    EnumPropertyGeneratorDelegate(String propertyName, DeclaredTypeName enumType) {
        this.propertyName = propertyName;
        this.enumType = enumType;
    }

    void registerRequiredImports(Set<DeclaredTypeName> imports) {
        imports.add(enumType);
    }

    void writeGetterBody(JavaFileWriter writer, MethodDeclarationParameters params) throws IOException {
        final String value = "value";
        writer.writeFieldDeclaration(CoreTypes.JAVA_STRING, value,
                Expressions.callMethod("get", propertyName));
        Expression condition = Expressions.fromString(value + " == null");
        Expression ifTrue = Expressions.fromString("null");
        Expression ifFalse = Expressions.staticMethod(enumType, "valueOf", value);
        TernaryExpression ternary = new TernaryExpression(condition, ifTrue, ifFalse);
        writer.writeStatement(ternary.returnExpr());
    }

    void writeSetterBody(JavaFileWriter writer, MethodDeclarationParameters params) throws IOException {
        String argName = params.getArgumentNames().get(0);
        final String argAsString = argName + "AsString";
        Expression condition = Expressions.fromString(argName + " == null");
        Expression ifTrue = Expressions.fromString("null");
        Expression ifFalse = Expressions.callMethodOn(argName, "name");
        writer.writeFieldDeclaration(CoreTypes.JAVA_STRING, argAsString,
                new TernaryExpression(condition, ifTrue, ifFalse));
        writer.writeStatement(Expressions.callMethod("set", propertyName, argAsString));
        writer.writeStringStatement("return this");
    }

    private static class TernaryExpression extends Expression {

        private Expression condition;
        private Expression ifTrue;
        private Expression ifFalse;

        TernaryExpression(Expression condition, Expression ifTrue, Expression ifFalse) {
            this.condition = condition;
            this.ifTrue = ifTrue;
            this.ifFalse = ifFalse;
        }

        @Override
        public boolean writeExpression(JavaFileWriter writer) throws IOException {
            writer.appendExpression(condition)
                    .appendString(" ? ")
                    .appendExpression(ifTrue)
                    .appendString(" : ")
                    .appendExpression(ifFalse);

            return true;
        }
    }

}
