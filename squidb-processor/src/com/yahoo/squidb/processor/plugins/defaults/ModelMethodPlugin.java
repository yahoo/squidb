/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.plugins.defaults;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import com.yahoo.squidb.annotations.ModelMethod;
import com.yahoo.squidb.processor.StringUtils;
import com.yahoo.squidb.processor.TypeConstants;
import com.yahoo.squidb.processor.data.InheritedModelSpecWrapper;
import com.yahoo.squidb.processor.data.ModelSpec;
import com.yahoo.squidb.processor.data.TableModelSpecWrapper;
import com.yahoo.squidb.processor.data.ViewModelSpecWrapper;
import com.yahoo.squidb.processor.plugins.AbstractPlugin;
import com.yahoo.squidb.processor.plugins.Plugin;
import com.yahoo.squidb.processor.plugins.PluginEnvironment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
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

    private static final Pattern FIRST_OBJ_C_ARG = Pattern.compile("With\\p{Alnum}+:");

    private final List<ExecutableElement> modelMethods = new ArrayList<>();
    private final List<ExecutableElement> staticModelMethods = new ArrayList<>();

    // -- parse and validate methods

    @Override
    public void afterProcessVariableElements() {
        parseModelMethods();
    }

    private void parseModelMethods() {
        List<? extends Element> enclosedElements = modelSpec.getModelSpecElement().getEnclosedElements();
        for (Element e : enclosedElements) {
            if (e instanceof ExecutableElement) {
                checkExecutableElement((ExecutableElement) e, modelMethods, staticModelMethods);
            }
        }
    }

    private void checkExecutableElement(ExecutableElement e, List<ExecutableElement> modelMethods,
            List<ExecutableElement> staticModelMethods) {
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
                if (!checkFirstArgType(paramType)) {
                    modelSpec.logError("@ModelMethod methods must have either this model class or an appropriate model "
                            + "superclass as their first argument", e);
                } else {
                    modelMethods.add(e);
                }
            }
        }
    }

    private boolean checkFirstArgType(TypeMirror type) {
        TypeName typeName = TypeName.get(type);
        if (!(typeName instanceof ClassName)) {
            return false;
        }
        ClassName firstArgClass = (ClassName) typeName;

        // Acceptable first arg types for model methods:
        // 1) AbstractModel
        // 2) When the type doesn't exist yet (e.g. is the class to be generated), APT represents it as ErrorType, which
        //    doesn't include package info. In this case, we just check the simple name of the type against the simple name
        //    of the generated class.
        // 3) If it's not an ErrorType, check the full generated class name
        // 4) Generated model superclass
        // 5) Model superclass appropriate to this model spec type
        return TypeConstants.ABSTRACT_MODEL.equals(firstArgClass) ||
                (type instanceof ErrorType && firstArgClass.simpleName()
                        .equals(modelSpec.getGeneratedClassName().simpleName())) ||
                modelSpec.getGeneratedClassName().equals(firstArgClass) ||
                modelSpec.getModelSuperclass().equals(firstArgClass) ||
                modelSpec.accept(modelMethodArgumentTypeVisitor, null).equals(firstArgClass);
    }

    private ModelSpec.ModelSpecVisitor<TypeName, Void> modelMethodArgumentTypeVisitor = new ModelSpec.ModelSpecVisitor<TypeName, Void>() {
        @Override
        public TypeName visitTableModel(TableModelSpecWrapper modelSpec, Void data) {
            return TypeConstants.TABLE_MODEL;
        }

        @Override
        public TypeName visitViewModel(ViewModelSpecWrapper modelSpec, Void data) {
            return TypeConstants.VIEW_MODEL;
        }

        @Override
        public TypeName visitInheritedModel(InheritedModelSpecWrapper modelSpec, Void data) {
            return modelSpec.getModelSuperclass();
        }
    };

    // -- declare methods

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
            if (paramNumber != 0 || isStatic) {
                ParameterSpec.Builder parameterSpecBuilder = ParameterSpec.get(parameter).toBuilder();
                List<? extends AnnotationMirror> parameterAnnotationMirrors = parameter.getAnnotationMirrors();
                for (AnnotationMirror annotation : parameterAnnotationMirrors) {
                    parameterSpecBuilder.addAnnotation(AnnotationSpec.get(annotation));
                }
                ParameterSpec parameterSpec = parameterSpecBuilder.build();
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

        List<? extends AnnotationMirror> methodAnnotations = e.getAnnotationMirrors();
        for (AnnotationMirror annotation : methodAnnotations) {
            TypeName annotationType = TypeName.get(annotation.getAnnotationType());
            if (!isStatic && TypeConstants.OBJECTIVE_C_NAME.equals(annotationType)) {
                AnnotationSpec adjustedObjectiveCName = getAdjustedObjCNameAnnotation(e, annotation);
                if (adjustedObjectiveCName != null) {
                    methodBuilder.addAnnotation(adjustedObjectiveCName);
                }
            } else if (!TypeName.get(ModelMethod.class).equals(annotationType)) {
                methodBuilder.addAnnotation(AnnotationSpec.get(annotation));
            }
        }

        methodBuilder.addStatement(methodCall, modelSpec.getModelSpecName(), originalMethodName);
        builder.addMethod(methodBuilder.build());
    }

    private AnnotationSpec getAdjustedObjCNameAnnotation(ExecutableElement modelMethod, AnnotationMirror objCName) {
        String name = null;
        for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> annotationValue :
                objCName.getElementValues().entrySet()) {
            if (annotationValue.getKey().getSimpleName().toString().equals("value")) {
                AnnotationValue value = annotationValue.getValue();
                Object nameValue = value.getValue();
                if (nameValue instanceof String) {
                    name = (String) nameValue;
                    break;
                }
            }
        }
        if (name == null) {
            pluginEnv.getMessager().printMessage(Diagnostic.Kind.WARNING,
                    "Unable to adjust ObjectiveCName for model method", modelMethod);
            return null;
        }
        return AnnotationSpec.builder(TypeConstants.OBJECTIVE_C_NAME)
                .addMember("value", "$S", getAdjustedObjCName(name)).build();
    }

    private String getAdjustedObjCName(String objCName) {
        Matcher matcher = FIRST_OBJ_C_ARG.matcher(objCName);
        if (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();
            StringBuilder builder = new StringBuilder(objCName.substring(0, start));
            if (end < objCName.length()) {
                builder.append(StringUtils.capitalize(objCName.substring(end)));
            }
            return builder.toString();
        }
        return objCName;
    }
}
