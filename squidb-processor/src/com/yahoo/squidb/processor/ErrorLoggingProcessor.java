/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor;

import com.squareup.javapoet.TypeName;
import com.yahoo.squidb.annotations.ModelGenErrors;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;

/**
 * An annotation processor whose sole purpose is to check for {@link ModelGenErrors} annotations and log errors using
 * their contents. This is a workaround to defer this error logging until after the first round of annotation
 * processing has completed and avoid "cannot find symbol" errors. See the ModelGenErrors javadoc for more info.
 */
public class ErrorLoggingProcessor extends AbstractProcessor {

    private Set<String> supportedAnnotationTypes = new HashSet<>();

    public ErrorLoggingProcessor() {
        supportedAnnotationTypes.add(ModelGenErrors.class.getName());
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return supportedAnnotationTypes;
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment env) {
        for (TypeElement annotationType : annotations) {
            if (supportedAnnotationTypes.contains(annotationType.getQualifiedName().toString())) {
                for (Element element : env.getElementsAnnotatedWith(annotationType)) {
                    logErrors(element);
                }
            }
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    private void logErrors(Element element) {
        AnnotationValue errorsArrayValue = getAnnotationValueFromElement(element, ModelGenErrors.class, "value");
        if (errorsArrayValue != null) {
            List<? extends AnnotationValue> errorsList = (List<? extends AnnotationValue>) errorsArrayValue.getValue();
            for (AnnotationValue error : errorsList) {
                logSingleError(error);
            }
        }
    }

    private AnnotationValue getAnnotationValueFromElement(Element element, Class<? extends Annotation> annotationClass,
            String annotationValueName) {
        List<? extends AnnotationMirror> annotationMirrors = element.getAnnotationMirrors();
        for (AnnotationMirror mirror : annotationMirrors) {
            if (TypeName.get(annotationClass).equals(TypeName.get(mirror.getAnnotationType()))) {
                return getAnnotationValueFromMirror(mirror, annotationValueName);
            }
        }
        return null;
    }

    private AnnotationValue getAnnotationValueFromMirror(AnnotationMirror mirror, String annotationValueName) {
        Map<? extends ExecutableElement, ? extends AnnotationValue> values = mirror.getElementValues();
        for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> value : values.entrySet()) {
            if (annotationValueName.equals(value.getKey().getSimpleName().toString())) {
                return value.getValue();
            }
        }
        return null;
    }

    private void logSingleError(AnnotationValue singleErrorAnnotation) {
        AnnotationMirror singleErrorMirror = (AnnotationMirror) singleErrorAnnotation.getValue();

        TypeMirror errorClass = null;
        AnnotationValue errorClassValue = getAnnotationValueFromMirror(singleErrorMirror, "specClass");
        if (errorClassValue != null) {
            errorClass = (TypeMirror) errorClassValue.getValue();
        }

        String errorMessage = null;
        AnnotationValue errorMessageValue = getAnnotationValueFromMirror(singleErrorMirror, "message");
        if (errorMessageValue != null) {
            errorMessage = (String) errorMessageValue.getValue();
        }

        String errorElementName = null;
        AnnotationValue errorElementNameValue = getAnnotationValueFromMirror(singleErrorMirror, "element");
        if (errorElementNameValue != null) {
            errorElementName = (String) errorElementNameValue.getValue();
        }

        if (errorClass != null) {
            Element errorElement = findErrorElement(errorClass, errorElementName);
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, errorMessage, errorElement);
        }
    }

    private Element findErrorElement(TypeMirror errorClass, String elementName) {
        Element errorClassElement = processingEnv.getTypeUtils().asElement(errorClass);
        if (!StringUtils.isEmpty(elementName)) {
            List<? extends Element> enclosedElements = errorClassElement.getEnclosedElements();
            for (Element e : enclosedElements) {
                if (e.getSimpleName().toString().equals(elementName)) {
                    return e;
                }
            }
        }
        return errorClassElement;
    }
}
