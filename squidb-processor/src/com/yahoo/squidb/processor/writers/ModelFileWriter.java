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
import com.yahoo.squidb.annotations.Ignore;
import com.yahoo.squidb.annotations.ModelMethod;
import com.yahoo.squidb.processor.TypeConstants;
import com.yahoo.squidb.processor.plugins.PluginManager;
import com.yahoo.squidb.processor.plugins.PluginWriter;
import com.yahoo.squidb.processor.properties.factory.PropertyGeneratorFactory;
import com.yahoo.squidb.processor.properties.generators.PropertyGenerator;

import java.io.IOException;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.tools.Diagnostic.Kind;
import javax.tools.JavaFileObject;

public abstract class ModelFileWriter<T extends Annotation> {

    protected T modelSpec;
    protected JavaFileWriter writer;
    protected PropertyGeneratorFactory propertyGeneratorFactory;
    protected List<PluginWriter> pluginWriters;
    protected DeclaredTypeName generatedClassName;
    protected DeclaredTypeName sourceElementName;
    protected TypeElement modelSpecElement;
    protected AptUtils utils;

    protected Set<DeclaredTypeName> imports = new HashSet<DeclaredTypeName>();
    protected List<VariableElement> constantElements = new ArrayList<VariableElement>();
    protected List<PropertyGenerator> propertyGenerators = new ArrayList<PropertyGenerator>();
    protected List<PropertyGenerator> deprecatedPropertyGenerators = new ArrayList<PropertyGenerator>();

    protected static final String PROPERTIES_ARRAY_NAME = "PROPERTIES";
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

    public ModelFileWriter(TypeElement modelSpecElement, Class<T> modelSpecClass, PluginManager pluginManager,
            PropertyGeneratorFactory propertyGeneratorFactory, AptUtils utils) {
        this.modelSpecElement = modelSpecElement;
        this.propertyGeneratorFactory = propertyGeneratorFactory;
        this.utils = utils;
        this.sourceElementName = new DeclaredTypeName(modelSpecElement.getQualifiedName().toString());
        this.modelSpec = modelSpecElement.getAnnotation(modelSpecClass);
        this.generatedClassName = new DeclaredTypeName(sourceElementName.getPackageName(), getGeneratedClassName());
        this.pluginWriters = pluginManager.getWritersForElement(modelSpecElement, sourceElementName, generatedClassName);
    }

    protected abstract String getGeneratedClassName();

    public final void writeJava(Filer filer) throws IOException {
        initFileWriter(filer);
        writeJavaFile();
        writer.close();
    }

    private void initFileWriter(Filer filer) throws IOException {
        if (this.writer == null) {
            JavaFileObject jfo = filer.createSourceFile(generatedClassName.toString(), modelSpecElement);
            Writer writer = jfo.openWriter();
            this.writer = new JavaFileWriter(writer);
        } else {
            throw new IllegalStateException("JavaFileWriter already initialized");
        }
    }

    private void writeJavaFile() throws IOException {
        processVariableElements();

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

        for (PluginWriter pluginWriter : pluginWriters) {
            pluginWriter.writeMethods(writer);
        }

        emitCreator();
        emitModelSpecificHelpers();
        writer.finishTypeDefinition();
    }

    protected void processVariableElements() {
        List<? extends Element> enclosedElements = modelSpecElement.getEnclosedElements();
        for (Element e : enclosedElements) {
            if (e instanceof VariableElement && e.getAnnotation(Ignore.class) == null) {
                TypeName typeName = utils.getTypeNameFromTypeMirror(e.asType());
                if (!(typeName instanceof DeclaredTypeName)) {
                    utils.getMessager().printMessage(Kind.WARNING,
                            "Element type " + typeName + " is not a concrete type, will be ignored", e);
                } else {
                    processVariableElement((VariableElement) e, (DeclaredTypeName) typeName);
                }
            }
        }
    }

    protected void initializePropertyGenerator(VariableElement e) {
        PropertyGenerator generator = propertyGeneratorForElement(e);
        if (generator != null) {
            if (generator.isDeprecated()) {
                deprecatedPropertyGenerators.add(generator);
            } else {
                propertyGenerators.add(generator);
            }
        } else {
            utils.getMessager()
                    .printMessage(Kind.WARNING, "No PropertyGenerator found to handle this modelSpecElement", e);
        }
    }

    protected PropertyGenerator propertyGeneratorForElement(VariableElement e) {
        return propertyGeneratorFactory
                .getPropertyGeneratorForVariableElement(e, generatedClassName, modelSpecElement);
    }

    protected abstract void processVariableElement(VariableElement e, DeclaredTypeName elementType);

    protected abstract DeclaredTypeName getModelSuperclass();

    protected void emitModelSpecificFields() throws IOException {
        // Subclasses can override
    }

    protected void emitPackage() throws IOException {
        writer.writePackage(generatedClassName.getPackageName());
    }

    protected void emitImports() throws IOException {
        addCommonImports();
        writer.writeImports(imports);
        writer.registerOtherKnownNames(TypeConstants.CREATOR, TypeConstants.MODEL_CREATOR,
                TypeConstants.TABLE_MAPPING_VISITORS, sourceElementName);
    }

    private void addCommonImports() {
        imports.add(TypeConstants.CONTENT_VALUES);
        imports.add(TypeConstants.PROPERTY);
        imports.add(TypeConstants.ABSTRACT_MODEL);
        imports.add(TypeConstants.SQUID_CURSOR);
        imports.add(getModelSuperclass());
        for (PropertyGenerator generator : propertyGenerators) {
            generator.registerRequiredImports(imports);
        }
        Collection<DeclaredTypeName> modelSpecificImports = getModelSpecificImports();
        if (modelSpecificImports != null) {
            imports.addAll(modelSpecificImports);
        }
        utils.accumulateImportsFromElements(imports, constantElements);

        for (PluginWriter writer : pluginWriters) {
            writer.addRequiredImports(imports);
        }
    }

    protected abstract Collection<DeclaredTypeName> getModelSpecificImports();

    protected void beginClassDeclaration() throws IOException {
        writer.writeComment("Generated code -- do not modify!");
        writer.writeComment("This class was generated from the model spec at " + sourceElementName);
        if (modelSpecElement.getAnnotation(Deprecated.class) != null) {
            writer.writeAnnotation(CoreTypes.DEPRECATED);
        }
        TypeDeclarationParameters params = new TypeDeclarationParameters()
                .setName(generatedClassName)
                .setSuperclass(getModelSuperclass())
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

    protected void emitConstantElements() throws IOException {
        if (constantElements.size() > 0) {
            writer.writeComment("--- constants");
            for (VariableElement constant : constantElements) {
                writer.writeFieldDeclaration(
                        utils.getTypeNameFromTypeMirror(constant.asType()),
                        constant.getSimpleName().toString(),
                        Expressions.staticReference(sourceElementName, constant.getSimpleName().toString()),
                        TypeConstants.PUBLIC_STATIC_FINAL);
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
        return propertyGenerators.size();
    }

    protected void emitPropertyDeclarations() throws IOException {
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

    protected void emitConstructors() throws IOException {
        writer.writeComment("--- constructors");
        MethodDeclarationParameters params = new MethodDeclarationParameters()
                .setModifiers(Modifier.PUBLIC)
                .setConstructorName(generatedClassName);
        writer.beginConstructorDeclaration(params)
                .writeStringStatement("super()")
                .finishMethodDefinition();

        DeclaredTypeName squidCursorType = TypeConstants.SQUID_CURSOR.clone();
        squidCursorType.setTypeArgs(Collections.singletonList(getSquidCursorTypeArg()));
        params.setArgumentTypes(squidCursorType).setArgumentNames("cursor");
        writer.beginConstructorDeclaration(params)
                .writeStringStatement("this()")
                .writeStringStatement("readPropertiesFromCursor(cursor)")
                .finishMethodDefinition();

        params.setArgumentTypes(Collections.singletonList(TypeConstants.CONTENT_VALUES))
                .setArgumentNames("contentValues");
        writer.beginConstructorDeclaration(params)
                .writeStatement(Expressions.callMethod("this", "contentValues", PROPERTIES_ARRAY_NAME))
                .finishMethodDefinition();

        params.setArgumentTypes(Arrays.asList(TypeConstants.CONTENT_VALUES, TypeConstants.PROPERTY_VARARGS))
                .setArgumentNames("contentValues", "withProperties");
        writer.beginConstructorDeclaration(params)
                .writeStringStatement("this()")
                .writeStringStatement("readPropertiesFromContentValues(contentValues, withProperties)")
                .finishMethodDefinition();
    }

    protected void emitClone() throws IOException {
        MethodDeclarationParameters params = new MethodDeclarationParameters()
                .setModifiers(Modifier.PUBLIC)
                .setMethodName("clone")
                .setReturnType(generatedClassName);

        Expression cloneBody = Expressions.callMethodOn("super", "clone")
                .cast(generatedClassName).returnExpr();

        writer.writeAnnotation(CoreTypes.OVERRIDE);
        writer.beginMethodDefinition(params)
                .writeStatement(cloneBody)
                .finishMethodDefinition();
    }

    protected TypeName getSquidCursorTypeArg() {
        return generatedClassName;
    }

    protected void emitGettersAndSetters() throws IOException {
        writer.writeComment("--- getters and setters");
        for (PropertyGenerator generator : propertyGenerators) {
            emitGetter(writer, generator);
            emitSetter(writer, generator);
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

    private void emitModelMethod(ExecutableElement e, Modifier... modifiers) throws IOException {
        MethodDeclarationParameters params = utils.methodDeclarationParamsFromExecutableElement(e, modifiers);

        ModelMethod methodAnnotation = e.getAnnotation(ModelMethod.class);
        List<Object> arguments = new ArrayList<Object>();
        if (methodAnnotation != null) {
            String name = methodAnnotation.name();
            if (!AptUtils.isEmpty(name)) {
                params.setMethodName(name);
            }
            params.getArgumentTypes().remove(0);
            params.getArgumentNames().remove(0);
            arguments.add(0, "this");
        }
        arguments.addAll(params.getArgumentNames());
        Expression methodCall = Expressions.staticMethod(sourceElementName,
                e.getSimpleName().toString(), arguments);
        if (!CoreTypes.VOID.equals(params.getReturnType())) {
            methodCall = methodCall.returnExpr();
        }
        writer.beginMethodDefinition(params)
                .writeStatement(methodCall)
                .finishMethodDefinition();
    }

    protected void emitDefaultValues() throws IOException {
        writer.writeComment("--- default values");
        writer.writeFieldDeclaration(TypeConstants.CONTENT_VALUES, "defaultValues",
                Expressions.callConstructor(TypeConstants.CONTENT_VALUES),
                Modifier.PROTECTED, Modifier.STATIC, Modifier.FINAL);

        writer.beginInitializerBlock(true, true)
                .writeComment("--- put property defaults");

        emitDefaultValuesInitializationBlock();

        writer.finishInitializerBlock(false, true).writeNewline();

        writer.writeAnnotation(CoreTypes.OVERRIDE)
                .beginMethodDefinition(GET_DEFAULT_VALUES_PARAMS)
                .writeStringStatement("return defaultValues")
                .finishMethodDefinition();
    }

    protected abstract void emitDefaultValuesInitializationBlock() throws IOException;

    protected void emitCreator() throws IOException {
        writer.writeComment("--- parcelable helpers");
        List<DeclaredTypeName> genericList = Collections.singletonList(generatedClassName);
        DeclaredTypeName creatorType = TypeConstants.CREATOR.clone();
        DeclaredTypeName modelCreatorType = TypeConstants.MODEL_CREATOR.clone();
        creatorType.setTypeArgs(genericList);
        modelCreatorType.setTypeArgs(genericList);

        writer.writeFieldDeclaration(creatorType,
                "CREATOR", Expressions.callConstructor(modelCreatorType, Expressions.classObject(generatedClassName)),
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
}
