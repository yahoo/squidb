/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.plugins.defaults;

import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import com.yahoo.squidb.annotations.ModelMethod;
import com.yahoo.squidb.processor.StringUtils;
import com.yahoo.squidb.processor.TypeConstants;
import com.yahoo.squidb.processor.data.ModelSpec;
import com.yahoo.squidb.processor.plugins.AbstractPlugin;
import com.yahoo.squidb.processor.plugins.Plugin;
import com.yahoo.squidb.processor.plugins.PluginEnvironment;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ErrorType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.tools.Diagnostic;

/**
 * A {@link Plugin} that controls copying public static methods or methods annotated with {@link ModelMethod}
 * to the generated model. It is enabled by default but can be disabled by passing
 * {@link PluginEnvironment#OPTIONS_DISABLE_DEFAULT_METHOD_HANDLING 'disableModelMethod'} as one of the
 * values for the 'squidbOptions' key.
 */
public class ModelMethodPlugin extends AbstractPlugin {

    private final List<ExecutableElement> modelMethods = new ArrayList<>();
    private final List<ExecutableElement> staticModelMethods = new ArrayList<>();

    @Override
    public boolean init(ModelSpec<?, ?> modelSpec, PluginEnvironment pluginEnv) {
        super.init(modelSpec, pluginEnv);
        parseModelMethods();
        return true;
    }

    private void parseModelMethods() {
        List<? extends Element> enclosedElements = modelSpec.getModelSpecElement().getEnclosedElements();
        for (Element e : enclosedElements) {
            if (e instanceof ExecutableElement) {
                checkExecutableElement((ExecutableElement) e, modelMethods, staticModelMethods,
                        modelSpec.getGeneratedClassName());
            }
        }
    }

    @Override
    public void declareMethodsOrConstructors(TypeSpec.Builder builder) {
        for (ExecutableElement e : modelMethods) {
            declareModelMethod(builder, e, false, Modifier.PUBLIC);
        }
        for (ExecutableElement e : staticModelMethods) {
            declareModelMethod(builder, e, true, Modifier.PUBLIC, Modifier.STATIC);
        }
    }

    private void declareModelMethod(TypeSpec.Builder builder, ExecutableElement e, boolean isStatic,
            Modifier... modifiers) {
        String originalMethodName = e.getSimpleName().toString();
        ModelMethod methodAnnotation = e.getAnnotation(ModelMethod.class);
        String modelMethodName = methodAnnotation != null ? methodAnnotation.name() : null;
        String generatedMethodName = StringUtils.isEmpty(modelMethodName) ? originalMethodName : modelMethodName;

        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(generatedMethodName);
        methodBuilder.addModifiers(modifiers);

        for (TypeParameterElement typeParameterElement : e.getTypeParameters()) {
            TypeVariable var = (TypeVariable) typeParameterElement.asType();
            methodBuilder.addTypeVariable(TypeVariableName.get(var));
        }

        List<String> delegateArguments = new ArrayList<>();
        if (!isStatic) {
            delegateArguments.add("this");
        }
        TypeName returnType = TypeName.get(e.getReturnType());
        methodBuilder.returns(returnType);
        int paramNumber = 0;
        for (VariableElement parameter : e.getParameters()) {
            ParameterSpec parameterSpec = ParameterSpec.get(parameter);
            if (paramNumber != 0 || isStatic) {
                methodBuilder.addParameter(parameterSpec);
                delegateArguments.add(parameterSpec.name);
            }
            paramNumber++;
        }
        methodBuilder.varargs(e.isVarArgs());

        for (TypeMirror thrownType : e.getThrownTypes()) {
            methodBuilder.addException(TypeName.get(thrownType));
        }

        String methodCall = "$T.$L(" + StringUtils.join(delegateArguments, ", ") + ")";
        if (!TypeName.VOID.equals(returnType)) {
            methodCall = "return " + methodCall;
        }

        String javadoc = JavadocPlugin.getJavadocFromElement(pluginEnv, e);
        if (!StringUtils.isEmpty(javadoc)) {
            methodBuilder.addJavadoc(javadoc);
        }

        methodBuilder.addStatement(methodCall, modelSpec.getModelSpecName(), originalMethodName);
        builder.addMethod(methodBuilder.build());
    }

    private void checkExecutableElement(ExecutableElement e, List<ExecutableElement> modelMethods,
            List<ExecutableElement> staticModelMethods, TypeName modelClass) {
        Set<Modifier> modifiers = e.getModifiers();
        if (e.getKind() == ElementKind.CONSTRUCTOR) {
            // Don't copy constructors
            return;
        }
        if (!modifiers.contains(Modifier.STATIC)) {
            pluginEnv.getMessager().printMessage(Diagnostic.Kind.WARNING,
                    "Model spec objects should never be instantiated, so non-static methods are meaningless. " +
                            "Did you mean to make this a static method?", e);
            return;
        }
        ModelMethod methodAnnotation = e.getAnnotation(ModelMethod.class);
        // Private static methods may be unannotated if they are called by a public annotated method.
        // Don't assume error if method is private
        if (methodAnnotation == null) {
            if (modifiers.contains(Modifier.PUBLIC)) {
                staticModelMethods.add(e);
            } else if (!modifiers.contains(Modifier.PRIVATE)) {
                pluginEnv.getMessager().printMessage(Diagnostic.Kind.WARNING,
                        "This method will not be added to the model definition. " +
                                "Did you mean to annotate this method with @ModelMethod?", e);
            }
        } else {
            List<? extends VariableElement> params = e.getParameters();
            if (params.size() == 0) {
                modelSpec.logError("@ModelMethod methods must have an abstract model as their first argument", e);
            } else {
                VariableElement firstParam = params.get(0);
                TypeMirror paramType = firstParam.asType();
                if (!checkFirstArgType(paramType, modelClass)) {
                    modelSpec.logError("@ModelMethod methods must have an abstract model as their first argument", e);
                } else {
                    modelMethods.add(e);
                }
            }
        }
    }

    private boolean checkFirstArgType(TypeMirror type, TypeName generatedClassName) {
        if (type instanceof ErrorType) {
            return true;
        }
        if (!(type instanceof DeclaredType)) {
            return false;
        }

        TypeName typeName = TypeName.get(type);

        return typeName.equals(generatedClassName) || typeName.equals(TypeConstants.ABSTRACT_MODEL);
    }
}
