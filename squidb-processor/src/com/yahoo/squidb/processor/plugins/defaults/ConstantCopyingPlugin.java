/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.plugins.defaults;

import com.yahoo.aptutils.model.DeclaredTypeName;
import com.yahoo.aptutils.model.TypeName;
import com.yahoo.aptutils.writer.JavaFileWriter;
import com.yahoo.aptutils.writer.expressions.Expressions;
import com.yahoo.squidb.annotations.Constants;
import com.yahoo.squidb.annotations.Ignore;
import com.yahoo.squidb.processor.TypeConstants;
import com.yahoo.squidb.processor.data.ModelSpec;
import com.yahoo.squidb.processor.plugins.Plugin;
import com.yahoo.squidb.processor.plugins.PluginEnvironment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.tools.Diagnostic;

/**
 * A plugin that controls the copying of otherwise unhandled public static final fields in model specs as constants in
 * the generated model. It is enabled by default but can be disabled by passing
 * {@link PluginEnvironment#OPTIONS_DISABLE_DEFAULT_CONSTANT_COPYING 'disableConstantCopying'} as one
 * of the values for the 'squidbOptions' key.
 */
public class ConstantCopyingPlugin extends Plugin {

    private final List<VariableElement> constantElements = new ArrayList<>();
    private final Map<String, List<VariableElement>> innerClassConstants = new HashMap<>();

    public ConstantCopyingPlugin(ModelSpec<?> modelSpec, PluginEnvironment pluginEnv) {
        super(modelSpec, pluginEnv);
    }

    @Override
    public boolean processVariableElement(VariableElement field, DeclaredTypeName fieldType) {
        return processVariableElement(field, constantElements);
    }

    private boolean processVariableElement(VariableElement field, List<VariableElement> constantList) {
        if (field.getAnnotation(Deprecated.class) != null) {
            return false;
        }
        if (TypeConstants.isVisibleConstant(field)) {
            constantList.add(field);
            return true;
        }
        return false;
    }

    @Override
    public void afterProcessVariableElements() {
        // Look for additional constants in @Constant annotated inner classes
        List<? extends Element> elements = modelSpec.getModelSpecElement().getEnclosedElements();
        for (Element element : elements) {
            if (element instanceof TypeElement && element.getAnnotation(Constants.class) != null) {
                if (!element.getModifiers().containsAll(Arrays.asList(Modifier.PUBLIC, Modifier.STATIC))) {
                    utils.getMessager().printMessage(Diagnostic.Kind.WARNING, "@Constants annotated class is not " +
                            "public static, will be ignored", element);
                    continue;
                }

                TypeElement constantClass = (TypeElement) element;
                List<VariableElement> constantList = new ArrayList<>();
                innerClassConstants.put(constantClass.getSimpleName().toString(), constantList);

                for (Element e : constantClass.getEnclosedElements()) {
                    if (e instanceof VariableElement && e.getAnnotation(Ignore.class) == null) {
                        TypeName typeName = utils.getTypeNameFromTypeMirror(e.asType());
                        if (!(typeName instanceof DeclaredTypeName)) {
                            utils.getMessager().printMessage(Diagnostic.Kind.WARNING,
                                    "Element type " + typeName + " is not a concrete type, will be ignored", e);
                        } else {
                            processVariableElement((VariableElement) e, constantList);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void addRequiredImports(Set<DeclaredTypeName> imports) {
        utils.accumulateImportsFromElements(imports, constantElements);
        for (List<VariableElement> innerClassConstant : innerClassConstants.values()) {
            utils.accumulateImportsFromElements(imports, innerClassConstant);
        }
    }

    @Override
    public void afterEmitSchema(JavaFileWriter writer) throws IOException {
        writer.writeComment("--- constants");
        for (VariableElement constant : constantElements) {
            writeConstantField(writer, modelSpec.getModelSpecName(), constant);
        }
        for (Map.Entry<String, List<VariableElement>> innerClassConstant : innerClassConstants.entrySet()) {
            String classNameString = innerClassConstant.getKey();
            DeclaredTypeName constClassName = new DeclaredTypeName(null,
                    modelSpec.getModelSpecName().getSimpleName() + "." + classNameString);
            for (VariableElement element : innerClassConstant.getValue()) {
                writeConstantField(writer, constClassName, element);
            }
        }
        writer.writeNewline();
    }

    private void writeConstantField(JavaFileWriter writer, DeclaredTypeName containingClassName,
            VariableElement constant) throws IOException {
        JavadocPlugin.writeJavadocFromElement(pluginEnv, writer, constant);
        writer.writeFieldDeclaration(
                utils.getTypeNameFromTypeMirror(constant.asType()),
                constant.getSimpleName().toString(),
                Expressions.staticReference(containingClassName, constant.getSimpleName().toString()),
                TypeConstants.PUBLIC_STATIC_FINAL);
    }
}
