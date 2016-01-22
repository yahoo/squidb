/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.plugins.defaults.properties.generators;

import com.yahoo.aptutils.model.CoreTypes;
import com.yahoo.aptutils.model.DeclaredTypeName;
import com.yahoo.aptutils.utils.AptUtils;
import com.yahoo.aptutils.writer.JavaFileWriter;
import com.yahoo.aptutils.writer.expressions.Expressions;
import com.yahoo.aptutils.writer.parameters.MethodDeclarationParameters;
import com.yahoo.squidb.annotations.ColumnSpec;
import com.yahoo.squidb.processor.StringUtils;
import com.yahoo.squidb.processor.TypeConstants;
import com.yahoo.squidb.processor.data.ModelSpec;
import com.yahoo.squidb.processor.writers.TableModelFileWriter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;
import javax.tools.Diagnostic.Kind;

/**
 * A basic implementation of {@link PropertyGenerator} that handles the {@link ColumnSpec} annotation
 */
public abstract class BasicPropertyGenerator extends PropertyGenerator {

    protected final ColumnSpec extras;
    protected final String propertyName;
    protected final String camelCasePropertyName;
    protected final String columnName;

    public BasicPropertyGenerator(ModelSpec<?> modelSpec, String columnName, AptUtils utils) {
        this(modelSpec, columnName, columnName, utils);
    }

    public BasicPropertyGenerator(ModelSpec<?> modelSpec, String columnName, String propertyName, AptUtils utils) {
        super(modelSpec, null, utils);
        this.extras = null;

        this.camelCasePropertyName = StringUtils.toCamelCase(propertyName);
        this.propertyName = StringUtils.toUpperUnderscore(camelCasePropertyName);
        this.columnName = columnName;

        validateColumnName();
    }

    public BasicPropertyGenerator(ModelSpec<?> modelSpec, VariableElement field, AptUtils utils) {
        super(modelSpec, field, utils);
        this.extras = field.getAnnotation(ColumnSpec.class);
        String name = field.getSimpleName().toString();

        this.camelCasePropertyName = StringUtils.toCamelCase(name);
        this.propertyName = StringUtils.toUpperUnderscore(camelCasePropertyName);
        this.columnName = getColumnName(extras);

        validateColumnName();
    }

    private void validateColumnName() {
        if (columnName.indexOf('$') >= 0) {
            utils.getMessager().printMessage(Kind.ERROR, "Column names cannot contain the $ symbol", field);
        } else if (Character.isDigit(columnName.charAt(0))) {
            utils.getMessager().printMessage(Kind.ERROR, "Column names cannot begin with a digit", field);
        }
    }

    private String getColumnName(ColumnSpec columnDef) {
        if (columnDef != null && !"".equals(columnDef.name())) {
            return columnDef.name();
        }
        return camelCasePropertyName;
    }

    @Override
    protected void registerAdditionalImports(Set<DeclaredTypeName> imports) {
        // Nothing to do
    }

    @Override
    public String getPropertyName() {
        return propertyName;
    }

    @Override
    public void emitPropertyDeclaration(JavaFileWriter writer) throws IOException {
        if (isDeprecated) {
            writer.writeAnnotation(CoreTypes.DEPRECATED);
        }
        List<String> constructorArgs = new ArrayList<String>();
        constructorArgs.add(TableModelFileWriter.TABLE_NAME);
        constructorArgs.add("\"" + columnName + "\"");
        String columnDef = getColumnDefinition();
        if (!AptUtils.isEmpty(columnDef)) {
            constructorArgs.add(columnDef);
        }

        writer.writeFieldDeclaration(getPropertyType(), propertyName,
                Expressions.callConstructor(getPropertyType(), constructorArgs), TypeConstants.PUBLIC_STATIC_FINAL);
    }

    protected String getColumnDefault() {
        return extras != null ? extras.defaultValue() : ColumnSpec.DEFAULT_NONE;
    }

    protected String getColumnDefinition() {
        String toReturn = null;
        String constraints = extras != null ? extras.constraints() : ColumnSpec.DEFAULT_NONE;
        if (!ColumnSpec.DEFAULT_NONE.equals(constraints) || !ColumnSpec.DEFAULT_NONE.equals(getColumnDefault())) {
            toReturn = constraints;

            String columnDefaultValue = getColumnDefinitionDefaultValue();

            if (ColumnSpec.DEFAULT_NONE.equals(toReturn)) {
                toReturn = "DEFAULT " + columnDefaultValue;
            } else if (!ColumnSpec.DEFAULT_NONE.equals(columnDefaultValue)) {
                if (!toReturn.toUpperCase().contains("DEFAULT")) {
                    toReturn += " DEFAULT " + columnDefaultValue;
                } else {
                    utils.getMessager().printMessage(Kind.WARNING, "Duplicate default value definitions", field);
                }
            }
            toReturn = "\"" + toReturn + "\"";
        }
        return toReturn;
    }

    protected String getColumnDefinitionDefaultValue() {
        String columnDefault = getColumnDefault();
        return ColumnSpec.DEFAULT_NULL.equals(columnDefault) ? "NULL" : columnDefault;
    }

    @Override
    public final void emitGetter(JavaFileWriter writer) throws IOException {
        if (isDeprecated) {
            return;
        }
        MethodDeclarationParameters params = new MethodDeclarationParameters()
                .setMethodName(getterMethodName())
                .setModifiers(Modifier.PUBLIC)
                .setReturnType(getTypeForAccessors());

        modelSpec.getPluginBundle().beforeEmitGetter(writer, params);
        writer.beginMethodDefinition(params);
        writeGetterBody(writer);
        writer.finishMethodDefinition();
        modelSpec.getPluginBundle().afterEmitGetter(writer, params);
    }

    @Override
    public String getterMethodName() {
        return "get" + StringUtils.capitalize(camelCasePropertyName);
    }

    protected void writeGetterBody(JavaFileWriter writer) throws IOException {
        writer.writeStatement(Expressions.callMethod("get", propertyName).returnExpr());
    }

    @Override
    public final void emitSetter(JavaFileWriter writer) throws IOException {
        if (isDeprecated) {
            return;
        }
        String argName = propertyName.equals(camelCasePropertyName) ? "_" + camelCasePropertyName
                : camelCasePropertyName;
        MethodDeclarationParameters params = new MethodDeclarationParameters()
                .setMethodName(setterMethodName())
                .setReturnType(modelSpec.getGeneratedClassName())
                .setModifiers(Modifier.PUBLIC)
                .setArgumentTypes(getTypeForAccessors())
                .setArgumentNames(argName);

        modelSpec.getPluginBundle().beforeEmitSetter(writer, params);
        writer.beginMethodDefinition(params);
        writeSetterBody(writer, argName);
        writer.finishMethodDefinition();
        modelSpec.getPluginBundle().afterEmitSetter(writer, params);
    }

    @Override
    public String setterMethodName() {
        return "set" + StringUtils.capitalize(camelCasePropertyName);
    }

    protected void writeSetterBody(JavaFileWriter writer, String argName) throws IOException {
        writer.writeStatement(Expressions.callMethod("set", propertyName, argName));
        writer.writeStringStatement("return this");
    }

    @Override
    public void emitPutDefault(JavaFileWriter writer, String contentValuesName) throws IOException {
        String defaultValue = getColumnDefault();
        if (ColumnSpec.DEFAULT_NONE.equals(getColumnDefault())) {
            return;
        }

        String methodToInvoke;
        List<Object> arguments = new ArrayList<Object>();
        arguments.add(Expressions.callMethodOn(propertyName, "getName"));
        if (ColumnSpec.DEFAULT_NULL.equals(defaultValue)) {
            methodToInvoke = "putNull";
        } else {
            methodToInvoke = "put";
            arguments.add(getContentValuesDefaultValue());
        }

        writer.writeStatement(Expressions.callMethodOn(contentValuesName, methodToInvoke, arguments));
    }

    protected String getContentValuesDefaultValue() {
        return getColumnDefault();
    }

}
