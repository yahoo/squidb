/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.plugins.defaults.properties.generators;

import com.squareup.javapoet.TypeName;
import com.yahoo.squidb.annotations.tables.defaults.DefaultInt;
import com.yahoo.squidb.processor.TypeConstants;
import com.yahoo.squidb.processor.data.ModelSpec;
import com.yahoo.squidb.processor.plugins.PluginEnvironment;
import com.yahoo.squidb.processor.plugins.defaults.constraints.DefaultValueAnnotationHandler;

import java.util.Arrays;
import java.util.List;

import javax.lang.model.element.VariableElement;

/**
 * An implementation of
 * {@link com.yahoo.squidb.processor.plugins.defaults.properties.generators.interfaces.TableModelPropertyGenerator}
 * for handling int fields
 */
public class BasicIntegerPropertyGenerator extends BasicTableModelPropertyGenerator {

    public static List<TypeName> handledColumnTypes() {
        return Arrays.asList(TypeName.BYTE, TypeName.BYTE.box(), TypeName.SHORT, TypeName.SHORT.box(),
                TypeName.INT, TypeName.INT.box());
    }

    public BasicIntegerPropertyGenerator(ModelSpec<?, ?> modelSpec, String columnName, PluginEnvironment pluginEnv) {
        super(modelSpec, columnName, pluginEnv);
    }

    public BasicIntegerPropertyGenerator(ModelSpec<?, ?> modelSpec, String columnName,
            String propertyName, PluginEnvironment pluginEnv) {
        super(modelSpec, columnName, propertyName, pluginEnv);
    }

    public BasicIntegerPropertyGenerator(ModelSpec<?, ?> modelSpec, VariableElement field, PluginEnvironment pluginEnv) {
        super(modelSpec, field, pluginEnv);
    }

    @Override
    public TypeName getTypeForAccessors() {
        return TypeName.INT.box();
    }

    @Override
    public TypeName getPropertyType() {
        return TypeConstants.INTEGER_PROPERTY;
    }

    @Override
    protected DefaultValueAnnotationHandler<?, ?> getDefaultValueAnnotationHandler() {
        return new DefaultValueAnnotationHandler<DefaultInt, Integer>() {
            @Override
            public Class<DefaultInt> getAnnotationClass() {
                return DefaultInt.class;
            }

            @Override
            protected Integer getPrimitiveDefaultValueFromAnnotation(DefaultInt annotation) {
                return annotation.value();
            }
        };
    }
}
