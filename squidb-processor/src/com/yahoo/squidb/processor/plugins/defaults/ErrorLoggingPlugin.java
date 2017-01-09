/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.plugins.defaults;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeSpec;
import com.yahoo.squidb.processor.StringUtils;
import com.yahoo.squidb.processor.data.ErrorInfo;
import com.yahoo.squidb.processor.data.ModelSpec;
import com.yahoo.squidb.processor.plugins.AbstractPlugin;
import com.yahoo.squidb.processor.plugins.Plugin;
import com.yahoo.squidb.processor.plugins.PluginEnvironment;

import java.util.List;

import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;

/**
 * A {@link Plugin} that controls writing any errors logged using {@link ModelSpec#logError(String, Element)} to the
 * generated file using the {@link com.yahoo.squidb.annotations.ModelGenErrors} annotation, to be subsequently
 * processed by the {@link com.yahoo.squidb.processor.ErrorLoggingProcessor}. It is enabled by default but can be
 * disabled by passing {@link PluginEnvironment#OPTIONS_USE_STANDARD_ERROR_LOGGING 'standardErrorLogging'} as one
 * of the values for the 'squidbOptions' key.
 */
public class ErrorLoggingPlugin extends AbstractPlugin {

    private static final ClassName MODEL_GEN_ERRORS = ClassName.get("com.yahoo.squidb.annotations", "ModelGenErrors");
    private static final ClassName MODEL_GEN_ERROR_INNER =
            ClassName.get("com.yahoo.squidb.annotations", "ModelGenErrors", "ModelGenError");

    @Override
    public void declareAdditionalJava(TypeSpec.Builder builder) {
        List<ErrorInfo> errors = modelSpec.getLoggedErrors();
        if (errors.size() > 0) {
            TypeSpec.Builder dummyErrorClass = TypeSpec.classBuilder("LoggedErrors")
                    .addModifiers(Modifier.STATIC, Modifier.FINAL)
                    .addAnnotation(modelGenAnnotationSpec(errors))
                    .addJavadoc("Dummy class for holding logged error annotations");
            builder.addType(dummyErrorClass.build());
        }
    }

    private AnnotationSpec modelGenAnnotationSpec(List<ErrorInfo> errors) {
        AnnotationSpec.Builder rootAnnotationBulder = AnnotationSpec.builder(MODEL_GEN_ERRORS);
        for (ErrorInfo errorInfo : errors) {
            AnnotationSpec.Builder singleErrorBuilder = AnnotationSpec.builder(MODEL_GEN_ERROR_INNER)
                    .addMember("specClass", "$T.class", errorInfo.errorClass);
            if (!StringUtils.isEmpty(errorInfo.element)) {
                singleErrorBuilder.addMember("element", "$S", errorInfo.element);
            }
            singleErrorBuilder.addMember("message", "$S", errorInfo.message);
            rootAnnotationBulder.addMember("value", "$L", singleErrorBuilder.build());
        }
        return rootAnnotationBulder.build();
    }
}
