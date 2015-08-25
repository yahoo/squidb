/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.writers;

import com.yahoo.aptutils.model.CoreTypes;
import com.yahoo.aptutils.model.DeclaredTypeName;
import com.yahoo.aptutils.model.GenericName;
import com.yahoo.aptutils.model.TypeName;
import com.yahoo.aptutils.utils.AptUtils;
import com.yahoo.aptutils.writer.JavaFileWriter;
import com.yahoo.aptutils.writer.JavaFileWriter.Type;
import com.yahoo.aptutils.writer.expressions.Expression;
import com.yahoo.aptutils.writer.expressions.Expressions;
import com.yahoo.aptutils.writer.parameters.MethodDeclarationParameters;
import com.yahoo.aptutils.writer.parameters.TypeDeclarationParameters;
import com.yahoo.squidb.processor.TypeConstants;
import com.yahoo.squidb.processor.data.ModelSpec;
import com.yahoo.squidb.processor.plugins.PluginContext;
import com.yahoo.squidb.processor.plugins.PluginWriter;
import com.yahoo.squidb.processor.plugins.properties.generators.PropertyGenerator;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;
import javax.tools.JavaFileObject;

public abstract class ModelFileWriter<T extends ModelSpec<?>> {

    protected final AptUtils utils;
    protected final PluginContext pluginContext;

    protected final T modelSpec;
    private final List<PluginWriter> pluginWriters;

    protected JavaFileWriter writer;

    public static final String PROPERTIES_ARRAY_NAME = "PROPERTIES";
    protected static final String DEFAULT_VALUES_NAME = "defaultValues";

    private static final MethodDeclarationParameters GET_CREATOR_PARAMS;
    private static final MethodDeclarationParameters GET_DEFAULT_VALUES_PARAMS;

    static {
        List<TypeName> extend = new ArrayList<TypeName>();
        extend.add(TypeConstants.ABSTRACT_MODEL);
        GenericName returnGeneric = new GenericName(GenericName.WILDCARD_CHAR, extend, null);
        DeclaredTypeName returnType = TypeConstants.CREATOR.clone();
        returnType.setTypeArgs(Collections.singletonList(returnGeneric));
        GET_CREATOR_PARAMS = new MethodDeclarationParameters()
                .setMethodName("getCreator")
                .setModifiers(Modifier.PROTECTED)
                .setReturnType(returnType);

        GET_DEFAULT_VALUES_PARAMS = new MethodDeclarationParameters()
                .setMethodName("getDefaultValues")
                .setModifiers(Modifier.PUBLIC)
                .setReturnType(TypeConstants.CONTENT_VALUES);
    }

    public ModelFileWriter(T modelSpec, PluginContext pluginContext, AptUtils utils) {
        this.pluginContext = pluginContext;
        this.utils = utils;
        this.modelSpec = modelSpec;
        this.pluginWriters = pluginContext.getWritersForElement(modelSpec);
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
        emitPackage();
        emitImports();
        beginClassDeclaration();
        emitConstantElements();

        emitPropertiesArray();

        emitModelSpecificFields();

        emitPropertyDeclarations();
        emitConstructors();
        emitClone();
        emitDefaultValues();
        emitGettersAndSetters();
        emitMethods();

        emitCreator();
        emitModelSpecificHelpers();
        emitAdditionalCodeFromPlugins();

        writer.finishTypeDefinition();
    }

    private void emitPackage() throws IOException {
        writer.writePackage(modelSpec.getGeneratedClassName().getPackageName());
    }

    private void emitImports() throws IOException {
        Set<DeclaredTypeName> imports = modelSpec.getRequiredImports();
        accumulateImports(imports);
        writer.writeImports(imports);
        writer.registerOtherKnownNames(TypeConstants.CREATOR, TypeConstants.MODEL_CREATOR,
                TypeConstants.TABLE_MAPPING_VISITORS, modelSpec.getModelSpecName());
    }

    private void accumulateImports(Set<DeclaredTypeName> imports) {
        for (PluginWriter writer : pluginWriters) {
            writer.addRequiredImports(imports);
        }
    }

    protected void emitModelSpecificFields() throws IOException {
        // Subclasses can override
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
        List<DeclaredTypeName> interfaces = new ArrayList<DeclaredTypeName>();
        for (PluginWriter writer : pluginWriters) {
            List<DeclaredTypeName> writerInterfaces = writer.getInterfacesToImplement();
            if (writerInterfaces != null) {
                interfaces.addAll(writerInterfaces);
            }
        }
        return interfaces;
    }

    private void emitConstantElements() throws IOException {
        if (modelSpec.getConstantElements().size() > 0) {
            writer.writeComment("--- constants");
            for (VariableElement constant : modelSpec.getConstantElements()) {
                writer.writeFieldDeclaration(
                        utils.getTypeNameFromTypeMirror(constant.asType()),
                        constant.getSimpleName().toString(),
                        Expressions.staticReference(modelSpec.getModelSpecName(), constant.getSimpleName().toString()),
                        TypeConstants.PUBLIC_STATIC_FINAL);
            }
            for (PluginWriter pluginWriter : pluginWriters) {
                pluginWriter.writeConstants(writer);
            }
            writer.writeNewline();
        }
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

    private void emitConstructors() throws IOException {
        writer.writeComment("--- constructors");
        for (PluginWriter pluginWriter : pluginWriters) {
            pluginWriter.writeConstructors(writer);
        }
    }

    private void emitClone() throws IOException {
        MethodDeclarationParameters params = new MethodDeclarationParameters()
                .setModifiers(Modifier.PUBLIC)
                .setMethodName("clone")
                .setReturnType(modelSpec.getGeneratedClassName());

        Expression cloneBody = Expressions.callMethodOn("super", "clone")
                .cast(modelSpec.getGeneratedClassName()).returnExpr();

        writer.writeAnnotation(CoreTypes.OVERRIDE);
        writer.beginMethodDefinition(params)
                .writeStatement(cloneBody)
                .finishMethodDefinition();
    }

    protected void emitGettersAndSetters() throws IOException {
        if (pluginContext.getFlag(PluginContext.OPTIONS_DISABLE_GETTERS_AND_SETTERS)) {
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

    private void emitMethods() throws IOException {
        writer.writeComment("--- other instance methods");
        for (PluginWriter pluginWriter : pluginWriters) {
            pluginWriter.writeMethods(writer);
        }
    }

    protected void emitDefaultValues() throws IOException {
        writer.writeComment("--- default values");
        writer.writeFieldDeclaration(TypeConstants.CONTENT_VALUES, DEFAULT_VALUES_NAME,
                Expressions.callConstructor(TypeConstants.CONTENT_VALUES),
                Modifier.PROTECTED, Modifier.STATIC, Modifier.FINAL);

        if (pluginContext.getFlag(PluginContext.OPTIONS_DISABLE_DEFAULT_CONTENT_VALUES)) {
            writer.writeComment("--- property defaults disabled by plugin flag");
        } else {
            writer.beginInitializerBlock(true, true)
                    .writeComment("--- put property defaults");

            emitDefaultValuesInitializationBlock();

            writer.finishInitializerBlock(false, true).writeNewline();

            writer.writeAnnotation(CoreTypes.OVERRIDE)
                    .beginMethodDefinition(GET_DEFAULT_VALUES_PARAMS)
                    .writeStringStatement("return " + DEFAULT_VALUES_NAME)
                    .finishMethodDefinition();
        }
    }

    protected abstract void emitDefaultValuesInitializationBlock() throws IOException;

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

        writer.writeAnnotation(CoreTypes.OVERRIDE)
                .beginMethodDefinition(GET_CREATOR_PARAMS)
                .writeStringStatement("return CREATOR")
                .finishMethodDefinition();
    }

    protected void emitModelSpecificHelpers() throws IOException {
        // Subclasses can override
    }

    private void emitAdditionalCodeFromPlugins() throws IOException {
        for (PluginWriter pluginWriter : pluginWriters) {
            pluginWriter.writeAdditionalCode(writer);
        }
    }
}
