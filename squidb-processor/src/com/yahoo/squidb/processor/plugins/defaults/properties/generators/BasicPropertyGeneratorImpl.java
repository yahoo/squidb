package com.yahoo.squidb.processor.plugins.defaults.properties.generators;

import com.yahoo.aptutils.model.DeclaredTypeName;
import com.yahoo.aptutils.utils.AptUtils;
import com.yahoo.aptutils.writer.JavaFileWriter;
import com.yahoo.aptutils.writer.expressions.Expressions;
import com.yahoo.aptutils.writer.parameters.MethodDeclarationParameters;
import com.yahoo.squidb.processor.StringUtils;
import com.yahoo.squidb.processor.TypeConstants;
import com.yahoo.squidb.processor.data.ModelSpec;
import com.yahoo.squidb.processor.plugins.defaults.properties.generators.interfaces.PropertyGenerator;

import java.io.IOException;
import java.util.Set;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;

/**
 * Basic abstract implementation of {@link PropertyGenerator} interface that includes getter and setter generation
 */
public abstract class BasicPropertyGeneratorImpl implements PropertyGenerator {

    protected final ModelSpec<?, ?> modelSpec;
    protected final VariableElement field;
    protected final AptUtils utils;
    protected final boolean isDeprecated;
    protected final String camelCasePropertyName;
    protected final String propertyName;

    public BasicPropertyGeneratorImpl(ModelSpec<?, ?> modelSpec, VariableElement field, String propertyName,
            AptUtils utils) {
        this.modelSpec = modelSpec;
        this.field = field;
        this.utils = utils;
        this.isDeprecated = field != null && field.getAnnotation(Deprecated.class) != null;
        this.camelCasePropertyName = StringUtils.toCamelCase(propertyName).trim();
        this.propertyName = StringUtils.toUpperUnderscore(camelCasePropertyName);
    }

    @Override
    public void registerRequiredImports(Set<DeclaredTypeName> imports) {
        imports.add(getPropertyType());
        DeclaredTypeName accessorType = getTypeForAccessors();
        if (!TypeConstants.isPrimitiveType(accessorType)) {
            imports.add(accessorType);
        }
    }

    @Override
    public boolean isDeprecated() {
        return isDeprecated;
    }

    @Override
    public VariableElement getField() {
        return field;
    }

    @Override
    public String getPropertyName() {
        return propertyName;
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
        writer.writeStatement(Expressions.callMethod("get", getPropertyName()).returnExpr());
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
        String argName = getPropertyName().equals(camelCasePropertyName) ? "_" + camelCasePropertyName
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
        writer.writeStatement(Expressions.callMethod("set", getPropertyName(), params.getArgumentNames().get(0)));
        writer.writeStringStatement("return this");
    }
}
