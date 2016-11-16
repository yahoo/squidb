/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor;

import com.yahoo.squidb.annotations.ModelGenErrors;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

public class ErrorLoggingProcessor extends AbstractProcessor {

    private Set<String> supportedAnnotationTypes = new HashSet<>();
    private Messager messager;

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
        this.messager = processingEnv.getMessager();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment env) {
        for (TypeElement annotationType : annotations) {
            if (supportedAnnotationTypes.contains(annotationType.getQualifiedName().toString())) {
                for (Element element : env.getElementsAnnotatedWith(annotationType)) {
                    ModelGenErrors errorsToLog = element.getAnnotation(ModelGenErrors.class);
                    logErrors(errorsToLog);
                }
            }
        }
        return true;
    }

    private void logErrors(ModelGenErrors errors) {
        // TODO: Implement error logging
    }
}
