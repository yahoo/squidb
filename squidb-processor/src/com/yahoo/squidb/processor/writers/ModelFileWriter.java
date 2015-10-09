/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.writers;

import com.yahoo.aptutils.model.CoreTypes;
import com.yahoo.aptutils.model.DeclaredTypeName;
import com.yahoo.aptutils.utils.AptUtils;
import com.yahoo.aptutils.writer.JavaFileWriter;
import com.yahoo.aptutils.writer.JavaFileWriter.Type;
import com.yahoo.aptutils.writer.expressions.Expressions;
import com.yahoo.aptutils.writer.parameters.MethodDeclarationParameters;
import com.yahoo.aptutils.writer.parameters.TypeDeclarationParameters;
import com.yahoo.squidb.processor.TypeConstants;
import com.yahoo.squidb.processor.data.ModelSpec;
import com.yahoo.squidb.processor.plugins.PluginBundle;
import com.yahoo.squidb.processor.plugins.PluginEnvironment;
import com.yahoo.squidb.processor.plugins.defaults.properties.generators.PropertyGenerator;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Modifier;
import javax.tools.JavaFileObject;

public abstract class ModelFileWriter<T extends ModelSpec<?>> {

    protected final AptUtils utils;
    protected final PluginEnvironment pluginEnv;

    protected final T modelSpec;

    protected JavaFileWriter writer;

    public static final String PROPERTIES_ARRAY_NAME = "PROPERTIES";
    protected static final String DEFAULT_VALUES_NAME = "defaultValues";
    private final boolean generateIosModels;

    private static final MethodDeclarationParameters GET_DEFAULT_VALUES_PARAMS;

    static {

        GET_DEFAULT_VALUES_PARAMS = new MethodDeclarationParameters()
                .setMethodName("getDefaultValues")
                .setModifiers(Modifier.PUBLIC)
                .setReturnType(TypeConstants.VALUES_STORAGE);
    }

    public ModelFileWriter(T modelSpec, PluginEnvironment pluginEnv, AptUtils utils) {
        this.modelSpec = modelSpec;
        this.pluginEnv = pluginEnv;
        this.utils = utils;
        this.generateIosModels = pluginEnv.hasOption(PluginEnvironment.OPTIONS_GENERATE_IOS_MODELS);
    }

    public final void writeJava(Filer filer) throws IOException {
        initFileWriter(filer);
        writeJavaFile();
        writer.close();
    }

    private void initFileWriter(Filer filer) throws IOException {
        if (this.writer == null) {
            JavaFileObject jfo = filer.createSourceFile(modelSpec.getGeneratedClassName().toString(),
                    modelSpec.getModelSpecElement());
            Writer writer = jfo.openWriter();
            this.writer = new JavaFileWriter(writer);
        } else {
            throw new IllegalStateException("JavaFileWriter already initialized");
        }
    }

    private void writeJavaFile() throws IOException {
        PluginBundle plugins = modelSpec.getPluginBundle();

        emitPackage();
        emitImports();
        beginClassDeclaration();

        plugins.beforeEmitSchema(writer);
        emitPropertiesArray();
        emitModelSpecificFields();
        emitPropertyDeclarations();
        emitDefaultValues();
        plugins.afterEmitSchema(writer);

        plugins.emitConstructors(writer);

        plugins.beforeEmitMethods(writer);
        emitGettersAndSetters();
        plugins.emitMethods(writer);
        plugins.afterEmitMethods(writer);

        if (!generateIosModels) {
            emitCreator();
        }
        emitModelSpecificHelpers();
        plugins.emitAdditionalJava(writer);

        writer.finishTypeDefinition();
    }

    private void emitPackage() throws IOException {
        writer.writePackage(modelSpec.getGeneratedClassName().getPackageName());
    }

    private void emitImports() throws IOException {
        Set<DeclaredTypeName> imports = new HashSet<DeclaredTypeName>();
        modelSpec.addRequiredImports(imports);
        writer.writeImports(imports);
        writer.registerOtherKnownNames(TypeConstants.CREATOR,
                TypeConstants.TABLE_MAPPING_VISITORS, modelSpec.getModelSpecName());
    }

    private void beginClassDeclaration() throws IOException {
        writer.writeComment("Generated code -- do not modify!");
        writer.writeComment("This class was generated from the model spec at " + modelSpec.getModelSpecName());
        if (modelSpec.getModelSpecElement().getAnnotation(Deprecated.class) != null) {
            writer.writeAnnotation(CoreTypes.DEPRECATED);
        }
        TypeDeclarationParameters params = new TypeDeclarationParameters()
                .setName(modelSpec.getGeneratedClassName())
                .setSuperclass(modelSpec.getModelSuperclass())
                .setInterfaces(accumulateInterfacesFromPlugins())
                .setKind(Type.CLASS)
                .setModifiers(Modifier.PUBLIC);
        writer.beginTypeDefinition(params);
    }

    private List<DeclaredTypeName> accumulateInterfacesFromPlugins() {
        Set<DeclaredTypeName> interfaces = new LinkedHashSet<DeclaredTypeName>();
        modelSpec.getPluginBundle().addInterfacesToImplement(interfaces);
        return Arrays.asList(interfaces.toArray(new DeclaredTypeName[interfaces.size()]));
    }

    protected void emitPropertiesArray() throws IOException {
        writer.writeComment("--- allocate properties array");
        writer.writeFieldDeclaration(TypeConstants.PROPERTY_ARRAY, PROPERTIES_ARRAY_NAME,
                Expressions.arrayAllocation(TypeConstants.PROPERTY, 1, getPropertiesArrayLength()),
                TypeConstants.PUBLIC_STATIC_FINAL);
        writer.writeNewline();
    }

    protected int getPropertiesArrayLength() {
        return modelSpec.getPropertyGenerators().size();
    }

    protected void emitModelSpecificFields() throws IOException {
        // Subclasses can override
    }

    private void emitPropertyDeclarations() throws IOException {
        writer.writeComment("--- property declarations");
        emitAllProperties();
        emitPropertyArrayInitialization();
        writer.writeNewline();
    }

    protected abstract void emitAllProperties() throws IOException;

    protected void emitPropertyArrayInitialization() throws IOException {
        writer.beginInitializerBlock(true, true);
        writePropertiesInitializationBlock();
        writer.finishInitializerBlock(false, true);
    }

    protected abstract void writePropertiesInitializationBlock() throws IOException;

    protected void emitDefaultValues() throws IOException {
        writer.writeComment("--- default values");
        DeclaredTypeName valuesStorageType = generateIosModels ?
                TypeConstants.MAP_VALUES_STORAGE : TypeConstants.CONTENT_VALUES_STORAGE;
        writer.writeFieldDeclaration(TypeConstants.VALUES_STORAGE, DEFAULT_VALUES_NAME,
                Expressions.callConstructor(valuesStorageType),
                Modifier.PROTECTED, Modifier.STATIC, Modifier.FINAL);

        if (pluginEnv.hasOption(PluginEnvironment.OPTIONS_DISABLE_DEFAULT_CONTENT_VALUES)) {
            writer.writeComment("--- property defaults disabled by plugin flag");
        } else {
            writer.beginInitializerBlock(true, true)
                    .writeComment("--- put property defaults");

            emitDefaultValuesInitializationBlock();

            writer.finishInitializerBlock(false, true).writeNewline();
        }
        writer.writeAnnotation(CoreTypes.OVERRIDE)
                .beginMethodDefinition(GET_DEFAULT_VALUES_PARAMS)
                .writeStringStatement("return " + DEFAULT_VALUES_NAME)
                .finishMethodDefinition();
    }

    protected abstract void emitDefaultValuesInitializationBlock() throws IOException;

    protected void emitGettersAndSetters() throws IOException {
        if (pluginEnv.hasOption(PluginEnvironment.OPTIONS_DISABLE_DEFAULT_GETTERS_AND_SETTERS)) {
            writer.writeComment("--- getters and setters disabled by plugin flag");
        } else {
            writer.writeComment("--- getters and setters");
            for (PropertyGenerator generator : modelSpec.getPropertyGenerators()) {
                emitGetter(writer, generator);
                emitSetter(writer, generator);
            }
        }
    }

    private void emitGetter(JavaFileWriter writer, PropertyGenerator generator) throws IOException {
        generator.beforeEmitGetter(writer);
        generator.emitGetter(writer);
        generator.afterEmitGetter(writer);
    }

    private void emitSetter(JavaFileWriter writer, PropertyGenerator generator) throws IOException {
        generator.beforeEmitSetter(writer);
        generator.emitSetter(writer);
        generator.afterEmitSetter(writer);
    }

    private void emitCreator() throws IOException {
        writer.writeComment("--- parcelable helpers");
        List<DeclaredTypeName> genericList = Collections.singletonList(modelSpec.getGeneratedClassName());
        DeclaredTypeName creatorType = TypeConstants.CREATOR.clone();
        DeclaredTypeName modelCreatorType = TypeConstants.MODEL_CREATOR.clone();
        creatorType.setTypeArgs(genericList);
        modelCreatorType.setTypeArgs(genericList);

        writer.writeFieldDeclaration(creatorType,
                "CREATOR", Expressions.callConstructor(modelCreatorType,
                        Expressions.classObject(modelSpec.getGeneratedClassName())),
                TypeConstants.PUBLIC_STATIC_FINAL)
                .writeNewline();
    }

    protected void emitModelSpecificHelpers() throws IOException {
        // Subclasses can override
    }
}
