/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.plugins.defaults.properties.generators;

import com.squareup.javapoet.TypeName;
import com.yahoo.squidb.annotations.tables.defaults.DefaultBoolean;
import com.yahoo.squidb.processor.StringUtils;
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
 * for handling boolean fields
 */
public class BasicBooleanPropertyGenerator extends BasicTableModelPropertyGenerator {

    public static List<TypeName> handledColumnTypes() {
        return Arrays.asList(TypeName.BOOLEAN, TypeName.BOOLEAN.box());
    }

    public BasicBooleanPropertyGenerator(ModelSpec<?, ?> modelSpec, String columnName, PluginEnvironment pluginEnv) {
        super(modelSpec, columnName, pluginEnv);
    }

    public BasicBooleanPropertyGenerator(ModelSpec<?, ?> modelSpec, String columnName,
            String propertyName, PluginEnvironment pluginEnv) {
        super(modelSpec, columnName, propertyName, pluginEnv);
    }

    public BasicBooleanPropertyGenerator(ModelSpec<?, ?> modelSpec, VariableElement field, PluginEnvironment pluginEnv) {
        super(modelSpec, field, pluginEnv);
    }

    @Override
    public TypeName getTypeForAccessors() {
        return TypeName.BOOLEAN.box();
    }

    @Override
    public TypeName getPropertyType() {
        return TypeConstants.BOOLEAN_PROPERTY;
    }

    @Override
    protected DefaultValueAnnotationHandler<?, ?> getDefaultValueAnnotationHandler() {
        return new DefaultValueAnnotationHandler<DefaultBoolean, Boolean>() {
            @Override
            public Class<DefaultBoolean> getAnnotationClass() {
                return DefaultBoolean.class;
            }

            @Override
            protected Boolean getPrimitiveDefaultValueFromAnnotation(DefaultBoolean annotation) {
                return annotation.value();
            }

            @Override
            protected String getPrimitiveDefaultValueAsSql(DefaultBoolean annotation) {
                return annotation.value() ? "1" : "0";
            }
        };
    }

    @Override
    public String getterMethodName() {
        if (camelCasePropertyName.startsWith("is") || camelCasePropertyName.startsWith("has")) {
            return camelCasePropertyName;
        } else {
            return "is" + StringUtils.capitalize(camelCasePropertyName);
        }
    }

    @Override
    public String setterMethodName() {
        if (!camelCasePropertyName.startsWith("is") && !camelCasePropertyName.startsWith("has")) {
            return "setIs" + StringUtils.capitalize(camelCasePropertyName);
        }
        return super.setterMethodName();
    }

}
