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
import com.yahoo.squidb.annotations.PrimaryKey;
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

        this.camelCasePropertyName = StringUtils.toCamelCase(propertyName).trim();
        this.propertyName = StringUtils.toUpperUnderscore(camelCasePropertyName);
        this.columnName = columnName == null ? null : columnName.trim();

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

    // TODO remove when SqlUtils reports an error for identifiers containing '$'
    private void validateColumnName() {
        if (columnName.indexOf('$') >= 0) {
            modelSpec.logError("Column names cannot contain the $ symbol", field);
        }
    }

    private String getColumnName(ColumnSpec columnDef) {
        if (columnDef != null && !AptUtils.isEmpty(columnDef.name().trim())) {
            return columnDef.name().trim();
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

    /**
     * @return the name of the underlying column in SQLite to use for this property
     */
    public String getColumnName() {
        return columnName;
    }

    @Override
    public void emitPropertyDeclaration(JavaFileWriter writer) throws IOException {
        if (isDeprecated) {
            writer.writeAnnotation(CoreTypes.DEPRECATED);
        }
        List<Object> constructorArgs = new ArrayList<>();
        constructorArgs.add(TableModelFileWriter.TABLE_MODEL_NAME);
        constructorArgs.add("\"" + columnName + "\"");
        String columnDef = getColumnDefinition();
        if (!AptUtils.isEmpty(columnDef)) {
            constructorArgs.add(columnDef);
        }

        writer.writeFieldDeclaration(getPropertyType(), propertyName,
                Expressions.callConstructor(getPropertyType(), constructorArgs), TypeConstants.PUBLIC_STATIC_FINAL);
    }

    /**
     * @return the default value given by any existing {@link ColumnSpec} definition, or {@link ColumnSpec#DEFAULT_NONE}
     * if none is set
     */
    protected String getColumnDefault() {
        return extras != null ? extras.defaultValue() : ColumnSpec.DEFAULT_NONE;
    }

    /**
     * @return the full column definition for this Property as a SQL string
     */
    protected String getColumnDefinition() {
        StringBuilder toReturn = new StringBuilder();
        String constraints = extras != null ? extras.constraints() : ColumnSpec.DEFAULT_NONE;
        if (!ColumnSpec.DEFAULT_NONE.equals(constraints)) {
            toReturn.append(constraints);
        }

        if (!ColumnSpec.DEFAULT_NONE.equals(getColumnDefault())) {
            String columnDefaultValue = getColumnDefinitionDefaultValue();

            if (!toReturn.toString().toUpperCase().contains("DEFAULT")) {
                toReturn.append(" DEFAULT ").append(columnDefaultValue);
            } else {
                utils.getMessager().printMessage(Kind.WARNING, "Duplicate default value definitions", field);
            }
        }

        if (field != null && field.getAnnotation(PrimaryKey.class) != null) {
            PrimaryKey primaryKeyAnnotation = field.getAnnotation(PrimaryKey.class);
            if (!toReturn.toString().toUpperCase().contains("PRIMARY KEY")) {
                toReturn.append(" PRIMARY KEY ");
                if (TypeConstants.isIntegerType(getTypeForAccessors()) && primaryKeyAnnotation.autoincrement()) {
                    toReturn.append("AUTOINCREMENT");
                }
            } else {
                utils.getMessager().printMessage(Kind.WARNING, "Duplicate primary key definition in column constraints."
                        + " Use the @PrimaryKey annotation instead of declaring the constraint in ColumnSpec.");
            }
        }

        String toReturnString = toReturn.toString().trim();
        if (!AptUtils.isEmpty(toReturnString)) {
            return "\"" + toReturnString + "\"";
        }
        return null;
    }

    /**
     * @return a string version of the column default to be used in the SQLite column definition
     */
    protected String getColumnDefinitionDefaultValue() {
        String columnDefault = getColumnDefault();
        return ColumnSpec.DEFAULT_NULL.equals(columnDefault) ? "NULL" : columnDefault;
    }

    @Override
    public final void emitGetter(JavaFileWriter writer) throws IOException {
        if (isDeprecated) {
            return;
        }
        MethodDeclarationParameters params = getterMethodParams();

        modelSpec.getPluginBundle().beforeEmitGetter(writer, this, params);
        writer.beginMethodDefinition(params);
        writeGetterBody(writer, params);
        writer.finishMethodDefinition();
        modelSpec.getPluginBundle().afterEmitGetter(writer, this, params);
    }

    @Override
    public String getterMethodName() {
        return "get" + StringUtils.capitalize(camelCasePropertyName);
    }

    /**
     * Constructs and returns a MethodDeclarationParameters object that defines the method signature for the property
     * getter method. Subclasses can override this hook to alter or return different parameters. Some contracts that
     * should be observed when overriding this hook and creating the MethodDeclarationParameters object:
     * <ul>
     * <li>The method name should be the value returned by {@link #getterMethodName()}</li>
     * <li>The method return type should be the value returned by {@link #getTypeForAccessors()}</li>
     * </ul>
     * The best way to keep these contracts when overriding this hook is to first call super.getterMethodParams()
     * and then modify the object returned from super before returning it.
     *
     * @see #writeGetterBody(JavaFileWriter, MethodDeclarationParameters)
     */
    protected MethodDeclarationParameters getterMethodParams() {
        return new MethodDeclarationParameters()
                .setMethodName(getterMethodName())
                .setModifiers(Modifier.PUBLIC)
                .setReturnType(getTypeForAccessors());
    }

    /**
     * Subclasses can override this hook to generate a custom method body for the property getter
     */
    protected void writeGetterBody(JavaFileWriter writer, MethodDeclarationParameters params) throws IOException {
        writeGetterBody(writer);
    }

    /**
     * Subclasses can override this hook to generate a custom method body for the property getter. This version of the
     * hook is deprecated, users should use {@link #writeGetterBody(JavaFileWriter, MethodDeclarationParameters)}
     * instead.
     */
    @Deprecated
    protected void writeGetterBody(JavaFileWriter writer) throws IOException {
        writer.writeStatement(Expressions.callMethod("get", propertyName).returnExpr());
    }

    @Override
    public final void emitSetter(JavaFileWriter writer) throws IOException {
        if (isDeprecated) {
            return;
        }
        MethodDeclarationParameters params = setterMethodParams();

        modelSpec.getPluginBundle().beforeEmitSetter(writer, this, params);
        writer.beginMethodDefinition(params);
        writeSetterBody(writer, params);
        writer.finishMethodDefinition();
        modelSpec.getPluginBundle().afterEmitSetter(writer, this, params);
    }

    @Override
    public String setterMethodName() {
        return "set" + StringUtils.capitalize(camelCasePropertyName);
    }

    /**
     * Constructs and returns a MethodDeclarationParameters object that defines the method signature for the property
     * setter method. Subclasses can override this hook to alter or return different parameters. Some contracts that
     * should be observed when overriding this hook and creating the MethodDeclarationParameters object:
     * <ul>
     * <li>The method name should be the value returned by {@link #setterMethodName()}</li>
     * <li>The method should typically accept as an argument an object of the type returned by
     * {@link #getTypeForAccessors()}. This argument would be the value to set</li>
     * </ul>
     * The best way to keep these contracts when overriding this hook is to first call super.setterMethodParams()
     * and then modify the object returned from super before returning it.
     *
     * @see #writeSetterBody(JavaFileWriter, MethodDeclarationParameters)
     */
    protected MethodDeclarationParameters setterMethodParams() {
        String argName = propertyName.equals(camelCasePropertyName) ? "_" + camelCasePropertyName
                : camelCasePropertyName;
        return new MethodDeclarationParameters()
                .setMethodName(setterMethodName())
                .setReturnType(modelSpec.getGeneratedClassName())
                .setModifiers(Modifier.PUBLIC)
                .setArgumentTypes(getTypeForAccessors())
                .setArgumentNames(argName);
    }

    /**
     * Subclasses can override this hook to generate a custom method body for the property setter
     */
    protected void writeSetterBody(JavaFileWriter writer, MethodDeclarationParameters params) throws IOException {
        writeSetterBody(writer, params.getArgumentNames().get(0));
    }

    /**
     * Subclasses can override this hook to generate a custom method body for the property setter. This version of the
     * hook is deprecated, users should use {@link #writeGetterBody(JavaFileWriter, MethodDeclarationParameters)}
     * instead.
     */
    @Deprecated
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
        List<Object> arguments = new ArrayList<>();
        arguments.add(Expressions.callMethodOn(propertyName, "getName"));
        if (ColumnSpec.DEFAULT_NULL.equals(defaultValue)) {
            methodToInvoke = "putNull";
        } else {
            methodToInvoke = "put";
            arguments.add(getContentValuesDefaultValue());
        }

        writer.writeStatement(Expressions.callMethodOn(contentValuesName, methodToInvoke, arguments));
    }

    /**
     * @return a string version of the column default to be used in the model's defaultValues ValuesStorage
     */
    protected String getContentValuesDefaultValue() {
        return getColumnDefault();
    }

}
