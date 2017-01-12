/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.plugins.defaults;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import com.yahoo.squidb.processor.plugins.AbstractPlugin;
import com.yahoo.squidb.processor.plugins.Plugin;
import com.yahoo.squidb.processor.plugins.PluginEnvironment;
import com.yahoo.squidb.processor.plugins.defaults.properties.generators.RowidPropertyGenerator;
import com.yahoo.squidb.processor.plugins.defaults.properties.generators.interfaces.PropertyGenerator;

/**
 * A {@link Plugin} that alters the generated models to have iOS-specific features. It is disabled by default but
 * can be enabled by passing {@link PluginEnvironment#OPTIONS_GENERATE_IOS_MODELS 'iOSModels'} as one
 * of the values for the 'squidbOptions' key.
 */
public class IOSModelPlugin extends AbstractPlugin {

    private static final ClassName OBJECTIVE_C_NAME = ClassName.get("com.google.j2objc.annotations", "ObjectiveCName");

    @Override
    public void beforeDeclareGetter(TypeSpec.Builder builder, PropertyGenerator propertyGenerator,
            MethodSpec.Builder getterParams) {
        String methodName = getterParams.build().name;
        if (methodName.startsWith("get") && !RowidPropertyGenerator.DEFAULT_ROWID_GETTER_NAME.equals(methodName)) {
            String objectiveCName = methodName.substring(3);
            objectiveCName = Character.toLowerCase(objectiveCName.charAt(0)) + objectiveCName.substring(1);
            getterParams.addAnnotation(
                    AnnotationSpec.builder(OBJECTIVE_C_NAME)
                            .addMember("value", "$S", sanitizeObjectiveCName(objectiveCName))
                            .build());
        }
    }

    @Override
    public void beforeDeclareSetter(TypeSpec.Builder builder, PropertyGenerator propertyGenerator,
            MethodSpec.Builder setterParams) {
        String methodName = setterParams.build().name;
        if (methodName.startsWith("set") && !RowidPropertyGenerator.DEFAULT_ROWID_SETTER_NAME.equals(methodName)) {
            String objectiveCName = methodName + ":";
            setterParams.addAnnotation(
                    AnnotationSpec.builder(OBJECTIVE_C_NAME)
                            .addMember("value", "$S", sanitizeObjectiveCName(objectiveCName))
                            .build());
        }
    }

    private String sanitizeObjectiveCName(String objectiveCName) {
        // The @ObjectiveCName annotation rejects non-word characters, even though it seems like Obj-C allows a few others
        return objectiveCName.replaceAll("[^\\w:]", "_");
    }
}
