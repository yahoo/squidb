/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.plugins.defaults;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.yahoo.squidb.annotations.Implements;
import com.yahoo.squidb.processor.data.ModelSpec;
import com.yahoo.squidb.processor.plugins.AbstractPlugin;
import com.yahoo.squidb.processor.plugins.Plugin;
import com.yahoo.squidb.processor.plugins.PluginEnvironment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

/**
 * A {@link Plugin} that controls declaring that model classes implement interfaces. This plugin looks for and parses
 * the value of any {@link Implements} annotations on the given model spec to determine which interfaces to add. It is
 * enabled by default but can be disabled by passing
 * {@link PluginEnvironment#OPTIONS_DISABLE_DEFAULT_IMPLEMENTS_HANDLING 'disableImplements'} as one of the
 * values for the 'squidbOptions' key.
 */
public class ImplementsPlugin extends AbstractPlugin {

    private final List<TypeName> interfaces = new ArrayList<>();

    @Override
    public boolean init(ModelSpec<?, ?> modelSpec, PluginEnvironment pluginEnv) {
        super.init(modelSpec, pluginEnv);
        parseInterfaces();
        return true;
    }

    @Override
    public void addInterfacesToImplement(Set<TypeName> interfaces) {
        interfaces.addAll(this.interfaces);
    }

    private void parseInterfaces() {
        TypeElement modelSpecElement = modelSpec.getModelSpecElement();
        if (modelSpecElement.getAnnotation(Implements.class) != null) {
            List<? extends AnnotationMirror> annotationMirrors = modelSpecElement.getAnnotationMirrors();
            for (AnnotationMirror annotation : annotationMirrors) {
                if (TypeName.get(Implements.class).equals(TypeName.get(annotation.getAnnotationType()))) {
                    processAnnotationMirror(annotation);
                }
            }
        }
    }

    private void processAnnotationMirror(AnnotationMirror implementsAnnotation) {
        for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> annotationValue :
                implementsAnnotation.getElementValues().entrySet()) {
            ExecutableElement method = annotationValue.getKey();
            AnnotationValue value = annotationValue.getValue();

            String methodName = method.getSimpleName().toString();
            if ("interfaceClasses".equals(methodName)) {
                processInterfaceClasses(value);
            } else if ("interfaceDefinitions".equals(methodName)) {
                processInterfaceDefinitions(value);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void processInterfaceClasses(AnnotationValue classes) {
        List<? extends AnnotationValue> classList = (List<? extends AnnotationValue>) classes.getValue();
        for (AnnotationValue value : classList) {
            TypeMirror classMirror = (TypeMirror) value.getValue();
            interfaces.add(TypeName.get(classMirror));
        }
    }

    @SuppressWarnings("unchecked")
    private void processInterfaceDefinitions(AnnotationValue definitions) {
        List<? extends AnnotationValue> classList = (List<? extends AnnotationValue>) definitions.getValue();
        for (AnnotationValue value : classList) {
            AnnotationMirror interfaceSpecMirror = (AnnotationMirror) value.getValue();
            processInterfaceSpecMirror(interfaceSpecMirror);
        }
    }

    @SuppressWarnings("unchecked")
    private void processInterfaceSpecMirror(AnnotationMirror interfaceSpec) {
        ClassName rootName = null;
        List<TypeName> classTypeArgs = new ArrayList<>();
        List<TypeName> namedTypeArgs = new ArrayList<>();
        for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> annotationValue :
                interfaceSpec.getElementValues().entrySet()) {
            ExecutableElement method = annotationValue.getKey();
            AnnotationValue value = annotationValue.getValue();

            String methodName = method.getSimpleName().toString();
            if ("interfaceClass".equals(methodName)) {
                TypeMirror interfaceClass = (TypeMirror) value.getValue();
                rootName = ClassName.get((TypeElement) pluginEnv.getProcessingEnvironment()
                        .getTypeUtils().asElement(interfaceClass));
            } else if ("interfaceTypeArgs".equals(methodName)) {
                List<? extends AnnotationValue> typeArgs = (List<? extends AnnotationValue>) value.getValue();
                for (AnnotationValue arg : typeArgs) {
                    TypeMirror classMirror = (TypeMirror) arg.getValue();
                    classTypeArgs.add(TypeName.get(classMirror));
                }
            } else if ("interfaceTypeArgNames".equals(methodName)) {
                List<? extends AnnotationValue> typeArgNames = (List<? extends AnnotationValue>) value.getValue();
                for (AnnotationValue arg : typeArgNames) {
                    String className = (String) arg.getValue();
                    namedTypeArgs.add(ClassName.bestGuess(className));
                }
            }

        }
        if (rootName != null) {
            if (classTypeArgs.isEmpty() && namedTypeArgs.isEmpty()) {
                interfaces.add(rootName);
            } else if (classTypeArgs.isEmpty()) {
                interfaces.add(ParameterizedTypeName.get(rootName, namedTypeArgs.toArray(new TypeName[namedTypeArgs.size()])));
            } else {
                interfaces.add(ParameterizedTypeName.get(rootName, classTypeArgs.toArray(new TypeName[classTypeArgs.size()])));
            }
        }
    }
}
