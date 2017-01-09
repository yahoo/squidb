/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.plugins.defaults;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.yahoo.squidb.annotations.Constants;
import com.yahoo.squidb.annotations.Ignore;
import com.yahoo.squidb.processor.StringUtils;
import com.yahoo.squidb.processor.TypeConstants;
import com.yahoo.squidb.processor.data.ModelSpec;
import com.yahoo.squidb.processor.plugins.Plugin;
import com.yahoo.squidb.processor.plugins.PluginEnvironment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    public ConstantCopyingPlugin(ModelSpec<?, ?> modelSpec, PluginEnvironment pluginEnv) {
        super(modelSpec, pluginEnv);
    }

    @Override
    public boolean processVariableElement(VariableElement field, TypeName fieldType) {
        return processVariableElement(field, constantElements);
    }

    private boolean processVariableElement(VariableElement field, List<VariableElement> constantList) {
        if (field.getAnnotation(Deprecated.class) != null) {
            return false;
        }
        TypeName typeName = TypeName.get(field.asType());
        if (TypeConstants.isGenericType(typeName)) {
            pluginEnv.getMessager().printMessage(Diagnostic.Kind.WARNING,
                    "Element type " + typeName + " is not a concrete type, will be ignored", field);
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
                    pluginEnv.getMessager().printMessage(Diagnostic.Kind.WARNING, "@Constants annotated class is not " +
                            "public static, will be ignored", element);
                    continue;
                }

                TypeElement constantClass = (TypeElement) element;
                List<VariableElement> constantList = new ArrayList<>();
                innerClassConstants.put(constantClass.getSimpleName().toString(), constantList);

                for (Element e : constantClass.getEnclosedElements()) {
                    if (e instanceof VariableElement && e.getAnnotation(Ignore.class) == null) {
                        processVariableElement((VariableElement) e, constantList);
                    }
                }
            }
        }
    }

    @Override
    public void afterDeclareSchema(TypeSpec.Builder builder) {
        CodeBlock.Builder initializerBlock = CodeBlock.builder();
        for (VariableElement constant : constantElements) {
            writeConstantField(builder, initializerBlock, null, constant);
        }
        for (Map.Entry<String, List<VariableElement>> innerClassConstant : innerClassConstants.entrySet()) {
            String classNameString = innerClassConstant.getKey();
            for (VariableElement constant : innerClassConstant.getValue()) {
                writeConstantField(builder, initializerBlock, classNameString, constant);
            }
        }
        CodeBlock initializer = initializerBlock.build();
        if (!initializer.isEmpty()) {
            builder.addStaticBlock(initializer);
        }
    }

    private void writeConstantField(TypeSpec.Builder builder, CodeBlock.Builder initializerBlock,
            String containingClassName, VariableElement constant) {
        String constantName = constant.getSimpleName().toString();
        FieldSpec.Builder constantField = FieldSpec.builder(TypeName.get(constant.asType()),
                constantName, Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL);

        if (StringUtils.isEmpty(containingClassName)) {
            initializerBlock.add("$L = $T.$L;\n", constantName, modelSpec.getModelSpecName(), constantName);
        } else {
            initializerBlock.add("$L = $T.$L.$L;\n", constantName, modelSpec.getModelSpecName(),
                    containingClassName, constantName);
        }
        String javadoc = JavadocPlugin.getJavadocFromElement(pluginEnv, constant);
        if (!StringUtils.isEmpty(javadoc)) {
            constantField.addJavadoc(javadoc);
        }

        builder.addField(constantField.build());
    }
}
