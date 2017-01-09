package com.yahoo.squidb.processor.plugins.defaults.properties.generators;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import com.yahoo.squidb.processor.StringUtils;
import com.yahoo.squidb.processor.data.ModelSpec;
import com.yahoo.squidb.processor.plugins.PluginEnvironment;
import com.yahoo.squidb.processor.plugins.defaults.properties.generators.interfaces.PropertyGenerator;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;

/**
 * Basic abstract implementation of {@link PropertyGenerator} interface that includes getter and setter generation
 */
public abstract class BasicPropertyGeneratorImpl implements PropertyGenerator {

    protected final ModelSpec<?, ?> modelSpec;
    protected final VariableElement field;
    protected final PluginEnvironment pluginEnv;
    protected final boolean isDeprecated;
    protected final String camelCasePropertyName;
    protected final String propertyName;

    public BasicPropertyGeneratorImpl(ModelSpec<?, ?> modelSpec, VariableElement field, String propertyName,
            PluginEnvironment pluginEnv) {
        this.modelSpec = modelSpec;
        this.field = field;
        this.pluginEnv = pluginEnv;
        this.isDeprecated = field != null && field.getAnnotation(Deprecated.class) != null;
        this.camelCasePropertyName = StringUtils.toCamelCase(propertyName).trim();
        this.propertyName = StringUtils.toUpperUnderscore(camelCasePropertyName);
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
    public final void declareGetter(TypeSpec.Builder builder) {
        if (isDeprecated) {
            return;
        }
        MethodSpec.Builder params = getterMethodParams();

        modelSpec.getPluginBundle().willDeclareGetter(builder, this, params);
        CodeBlock.Builder getterBody = CodeBlock.builder();
        writeGetterBody(getterBody, params.build());
        params.addCode(getterBody.build());
        MethodSpec spec = params.build();
        builder.addMethod(params.build());
        modelSpec.getPluginBundle().didDeclareGetter(builder, this, spec);
    }

    @Override
    public String getterMethodName() {
        return "get" + StringUtils.capitalize(camelCasePropertyName);
    }

    /**
     * Constructs and returns a MethodSpec.Builder object that defines the method signature for the property
     * getter method. Subclasses can override this hook to alter or return different parameters. Some contracts that
     * should be observed when overriding this hook and creating the MethodSpec.Builder object:
     * <ul>
     * <li>The method name should be the value returned by {@link #getterMethodName()}</li>
     * <li>The method return type should be the value returned by {@link #getTypeForAccessors()}</li>
     * </ul>
     * The best way to keep these contracts when overriding this hook is to first call super.getterMethodParams()
     * and then modify the object returned from super before returning it.
     *
     * @see #writeGetterBody(CodeBlock.Builder, MethodSpec)
     */
    protected MethodSpec.Builder getterMethodParams() {
        return MethodSpec.methodBuilder(getterMethodName())
                .addModifiers(Modifier.PUBLIC)
                .returns(getTypeForAccessors());
    }

    /**
     * Subclasses can override this hook to generate a custom method body for the property getter
     */
    protected void writeGetterBody(CodeBlock.Builder params, MethodSpec methodParams) {
        params.addStatement("return get($L)", getPropertyName());
    }

    @Override
    public final void declareSetter(TypeSpec.Builder builder) {
        if (isDeprecated) {
            return;
        }
        String argName = getPropertyName().equals(camelCasePropertyName) ? "_" + camelCasePropertyName
                : camelCasePropertyName;
        MethodSpec.Builder params = setterMethodParams(argName);
        modelSpec.getPluginBundle().willDeclareSetter(builder, this, params);
        CodeBlock.Builder setterBody = CodeBlock.builder();
        writeSetterBody(setterBody, params.build());
        params.addCode(setterBody.build());
        MethodSpec spec = params.build();
        builder.addMethod(spec);
        modelSpec.getPluginBundle().didDeclareSetter(builder, this, spec);
    }

    @Override
    public String setterMethodName() {
        return "set" + StringUtils.capitalize(camelCasePropertyName);
    }

    /**
     * Constructs and returns a MethodSpec.Builder object that defines the method signature for the property
     * setter method. Subclasses can override this hook to alter or return different parameters. Some contracts that
     * should be observed when overriding this hook and creating the MethodSpec.Builder object:
     * <ul>
     * <li>The method name should be the value returned by {@link #setterMethodName()}</li>
     * <li>The method should typically accept as an argument an object of the type returned by
     * {@link #getTypeForAccessors()}. This argument would be the value to set</li>
     * </ul>
     * The best way to keep these contracts when overriding this hook is to first call super.setterMethodParams()
     * and then modify the object returned from super before returning it.
     *
     * @see #writeSetterBody(CodeBlock.Builder, MethodSpec)
     */
    protected MethodSpec.Builder setterMethodParams(String argName) {
        return  MethodSpec.methodBuilder(setterMethodName())
                .returns(modelSpec.getGeneratedClassName())
                .addModifiers(Modifier.PUBLIC)
                .addParameter(getTypeForAccessors(), argName);
    }

    /**
     * Subclasses can override this hook to generate a custom method body for the property setter
     */
    protected void writeSetterBody(CodeBlock.Builder body, MethodSpec methodParams) {
        body.addStatement("set($L, $L)", getPropertyName(), methodParams.parameters.get(0).name);
        body.addStatement("return this");
    }
}
