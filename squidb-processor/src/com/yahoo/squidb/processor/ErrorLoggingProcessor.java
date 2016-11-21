/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor;

import com.yahoo.aptutils.utils.AptUtils;
import com.yahoo.squidb.annotations.ModelGenErrors;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
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
    private AptUtils utils;

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
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.utils = new AptUtils(processingEnv);
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
        AnnotationValue errorsArrayValue = utils.getAnnotationValue(element, ModelGenErrors.class, "value");
        List<? extends AnnotationValue> errorsList = (List<? extends AnnotationValue>) errorsArrayValue.getValue();
        for (AnnotationValue error : errorsList) {
            logSingleError(error);
        }
    }

    private void logSingleError(AnnotationValue singleErrorAnnotation) {
        AnnotationMirror singleErrorMirror = (AnnotationMirror) singleErrorAnnotation.getValue();

        TypeMirror errorClass = utils.getTypeMirrorsFromAnnotationValue(
                utils.getAnnotationValueFromMirror(singleErrorMirror, "specClass")).get(0);
        String errorMessage = utils.getValuesFromAnnotationValue(
                utils.getAnnotationValueFromMirror(singleErrorMirror, "message"), String.class).get(0);
        List<String> errorElementValues = utils.getValuesFromAnnotationValue(
                utils.getAnnotationValueFromMirror(singleErrorMirror, "element"), String.class);
        String errorElementName = AptUtils.isEmpty(errorElementValues) ? null : errorElementValues.get(0);

        Element errorElement = findErrorElement(errorClass, errorElementName);
        utils.getMessager().printMessage(Diagnostic.Kind.ERROR, errorMessage, errorElement);
    }

    private Element findErrorElement(TypeMirror errorClass, String elementName) {
        Element errorClassElement = utils.getTypes().asElement(errorClass);
        List<? extends Element> enclosedElements = errorClassElement.getEnclosedElements();
        for (Element e : enclosedElements) {
            if (e.getSimpleName().toString().equals(elementName)) {
                return e;
            }
        }
        return errorClassElement;
    }
}
