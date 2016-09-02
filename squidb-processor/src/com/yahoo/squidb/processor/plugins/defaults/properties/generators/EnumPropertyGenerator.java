package com.yahoo.squidb.processor.plugins.defaults.properties.generators;

import com.yahoo.aptutils.model.CoreTypes;
import com.yahoo.aptutils.model.DeclaredTypeName;
import com.yahoo.aptutils.utils.AptUtils;
import com.yahoo.aptutils.writer.JavaFileWriter;
import com.yahoo.aptutils.writer.expressions.Expression;
import com.yahoo.aptutils.writer.expressions.Expressions;
import com.yahoo.aptutils.writer.parameters.MethodDeclarationParameters;
import com.yahoo.squidb.processor.TypeConstants;
import com.yahoo.squidb.processor.data.ModelSpec;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;

import javax.lang.model.element.VariableElement;

/**
 * Property generator for Enum fields in a model spec.
 */
public class EnumPropertyGenerator extends BasicStringPropertyGenerator {

    private static final DeclaredTypeName ENUM_PROPERTY = new DeclaredTypeName(TypeConstants.PROPERTY.toString(),
            "EnumProperty");
    private final DeclaredTypeName enumType;

    public EnumPropertyGenerator(ModelSpec<?> modelSpec, VariableElement field, AptUtils utils,
            DeclaredTypeName enumType) {
        super(modelSpec, field, utils);
        this.enumType = enumType;
    }

    @Override
    protected void registerAdditionalImports(Set<DeclaredTypeName> imports) {
        imports.add(enumType);
    }

    @Override
    public DeclaredTypeName getPropertyType() {
        DeclaredTypeName enumProperty = ENUM_PROPERTY.clone();
        enumProperty.setTypeArgs(Collections.singletonList(enumType));
        return enumProperty;
    }

    @Override
    public DeclaredTypeName getTypeForAccessors() {
        return enumType;
    }

    @Override
    protected void writeGetterBody(JavaFileWriter writer, MethodDeclarationParameters params) throws IOException {
        final String value = "value";
        writer.writeFieldDeclaration(CoreTypes.JAVA_STRING, value,
                Expressions.callMethod("get", propertyName));
        Expression condition = Expressions.fromString(value + " == null");
        Expression ifTrue = Expressions.fromString("null");
        Expression ifFalse = Expressions.staticMethod(enumType, "valueOf", value);
        TernaryExpression ternary = new TernaryExpression(condition, ifTrue, ifFalse);
        writer.writeStatement(ternary.returnExpr());
    }

    @Override
    protected void writeSetterBody(JavaFileWriter writer, MethodDeclarationParameters params) throws IOException {
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
