/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.writers;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.yahoo.squidb.processor.TypeConstants;
import com.yahoo.squidb.processor.data.ModelSpec;
import com.yahoo.squidb.processor.plugins.PluginBundle;
import com.yahoo.squidb.processor.plugins.PluginEnvironment;
import com.yahoo.squidb.processor.plugins.defaults.properties.generators.interfaces.PropertyGenerator;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.Modifier;

public abstract class ModelFileWriter<T extends ModelSpec<?, ?>> {

    protected final PluginEnvironment pluginEnv;

    protected final T modelSpec;

    protected TypeSpec.Builder builder;

    public static final String PROPERTIES_ARRAY_NAME = "PROPERTIES";
    protected static final String DEFAULT_VALUES_NAME = "defaultValues";

    public ModelFileWriter(T modelSpec, PluginEnvironment pluginEnv) {
        this.modelSpec = modelSpec;
        this.pluginEnv = pluginEnv;
    }

    public final void writeJava() throws IOException {
        initTypeSpecBuilder();
        buildJavaFile();
        JavaFile.builder(modelSpec.getModelSpecName().packageName(), builder.build())
                .addFileComment("Generated code -- do not modify!")
                .indent("\t").build()
                .writeTo(pluginEnv.getProcessingEnvironment().getFiler());
    }

    private void initTypeSpecBuilder() {
        if (this.builder == null) {
            this.builder = TypeSpec.classBuilder(modelSpec.getGeneratedClassName())
                    .addOriginatingElement(modelSpec.getModelSpecElement())
                    .superclass(modelSpec.getModelSuperclass())
                    .addSuperinterfaces(accumulateInterfacesFromPlugins())
                    .addModifiers(Modifier.PUBLIC);
            if (modelSpec.getModelSpecElement().getAnnotation(Deprecated.class) != null) {
                builder.addAnnotation(Deprecated.class);
            }
        } else {
            throw new IllegalStateException("JavaFileWriter already initialized");
        }
    }

    private void buildJavaFile() {
        PluginBundle plugins = modelSpec.getPluginBundle();

        plugins.beforeBeginClassDeclaration(builder);

        plugins.beforeDeclareSchema(builder);
        declarePropertiesArray();
        declareModelSpecificFields();
        declarePropertyDeclarations();
        declareDefaultValues();
        declareModelSpecificHelpers();
        plugins.afterDeclareSchema(builder);

        declareGettersAndSetters();
        plugins.declareMethodsOrConstructors(builder);

        plugins.declareAdditionalJava(builder);
    }

    private List<TypeName> accumulateInterfacesFromPlugins() {
        Set<TypeName> interfaces = new LinkedHashSet<>();
        modelSpec.getPluginBundle().addInterfacesToImplement(interfaces);
        return Arrays.asList(interfaces.toArray(new TypeName[interfaces.size()]));
    }

    protected void declarePropertiesArray() {
        FieldSpec propertiesArray = FieldSpec.builder(TypeConstants.PROPERTY_ARRAY, PROPERTIES_ARRAY_NAME,
                Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                .initializer("new $T[$L]", TypeConstants.PROPERTY, getPropertiesArrayLength()).build();
        builder.addField(propertiesArray);
    }

    protected int getPropertiesArrayLength() {
        return modelSpec.getPropertyGenerators().size();
    }

    protected void declareModelSpecificFields() {
        // Subclasses can override
    }

    private void declarePropertyDeclarations() {
        declareAllProperties();
        declarePropertyArrayInitialization();
    }

    protected abstract void declareAllProperties();

    protected void declarePropertyArrayInitialization() {
        CodeBlock.Builder propertiesInitializationBlock = CodeBlock.builder();
        writePropertiesInitializationBlock(propertiesInitializationBlock);
        CodeBlock block = propertiesInitializationBlock.build();
        if (!block.isEmpty()) {
            builder.addStaticBlock(block);
        }
    }

    protected abstract void writePropertiesInitializationBlock(CodeBlock.Builder block);

    protected void declareDefaultValues() {
        FieldSpec.Builder defaultValuesField = FieldSpec.builder(TypeConstants.VALUES_STORAGE, DEFAULT_VALUES_NAME,
                Modifier.PROTECTED, Modifier.STATIC, Modifier.FINAL)
                .initializer("new $T().newValuesStorage()", modelSpec.getGeneratedClassName());
        builder.addField(defaultValuesField.build());

        if (!pluginEnv.hasSquidbOption(PluginEnvironment.OPTIONS_DISABLE_DEFAULT_VALUES)) {
            CodeBlock.Builder defaultValuesInitializationBlock = CodeBlock.builder();
            buildDefaultValuesInitializationBlock(defaultValuesInitializationBlock);
            CodeBlock block = defaultValuesInitializationBlock.build();
            if (!block.isEmpty()) {
                builder.addStaticBlock(block);
            }
        }

        MethodSpec.Builder getDefaultValues = MethodSpec.methodBuilder("getDefaultValues")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(TypeConstants.VALUES_STORAGE)
                .addStatement("return $L", DEFAULT_VALUES_NAME);
        builder.addMethod(getDefaultValues.build());
    }

    protected abstract void buildDefaultValuesInitializationBlock(CodeBlock.Builder block);

    protected void declareGettersAndSetters() {
        if (!pluginEnv.hasSquidbOption(PluginEnvironment.OPTIONS_DISABLE_DEFAULT_GETTERS_AND_SETTERS)) {
            for (PropertyGenerator generator : modelSpec.getPropertyGenerators()) {
                generator.declareGetter(builder);
                generator.declareSetter(builder);
            }
        }
    }

    protected void declareModelSpecificHelpers() {
        // Subclasses can override
    }
}
