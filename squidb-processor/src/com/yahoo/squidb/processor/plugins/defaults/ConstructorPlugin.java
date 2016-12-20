/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.plugins.defaults;

import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.yahoo.squidb.processor.TypeConstants;
import com.yahoo.squidb.processor.plugins.AbstractPlugin;
import com.yahoo.squidb.processor.plugins.Plugin;
import com.yahoo.squidb.processor.plugins.PluginEnvironment;
import com.yahoo.squidb.processor.writers.ModelFileWriter;

import javax.lang.model.element.Modifier;

/**
 * A {@link Plugin} that controls generating constructors in model classes. This plugin generates four distinct
 * constructors in each model class. It is enabled by default but can be disabled by passing
 * {@link PluginEnvironment#OPTIONS_DISABLE_DEFAULT_CONSTRUCTORS 'disableDefaultConstructors'} as one
 * of the values for the 'squidbOptions' key.
 */
public class ConstructorPlugin extends AbstractPlugin {

    @Override
    public void declareMethodsOrConstructors(TypeSpec.Builder builder) {
        MethodSpec.Builder params = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addStatement("super()");
        builder.addMethod(params.build());

        TypeName squidCursorType = ParameterizedTypeName.get(TypeConstants.SQUID_CURSOR,
                modelSpec.getGeneratedClassName());
        params = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(squidCursorType, "cursor")
                .addStatement("this()")
                .addStatement("readPropertiesFromCursor(cursor)");
        builder.addMethod(params.build());

        params = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(TypeConstants.MAP_VALUES, "values")
                .addStatement("this(values, $L)", ModelFileWriter.PROPERTIES_LIST_NAME);
        builder.addMethod(params.build());

        params = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(TypeConstants.MAP_VALUES, "values")
                .addParameter(TypeConstants.PROPERTY_ARRAY, "withProperties")
                .varargs()
                .addStatement("this()")
                .addStatement("readPropertiesFromMap(values, withProperties)");
        builder.addMethod(params.build());

        params = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(TypeConstants.MAP_VALUES, "values")
                .addParameter(TypeConstants.PROPERTY_LIST, "withProperties")
                .addStatement("this()")
                .addStatement("readPropertiesFromMap(values, withProperties)");
        builder.addMethod(params.build());


        params = MethodSpec.methodBuilder("clone")
                .addModifiers(Modifier.PUBLIC)
                .returns(modelSpec.getGeneratedClassName())
                .addStatement("return ($T) super.clone()", modelSpec.getGeneratedClassName())
                .addAnnotation(Override.class);
        builder.addMethod(params.build());
    }
}
