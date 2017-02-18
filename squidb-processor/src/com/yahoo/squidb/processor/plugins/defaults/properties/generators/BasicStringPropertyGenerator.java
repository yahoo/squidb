/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.plugins.defaults.properties.generators;

import com.squareup.javapoet.TypeName;
import com.yahoo.squidb.annotations.defaults.DefaultString;
import com.yahoo.squidb.processor.TypeConstants;
import com.yahoo.squidb.processor.data.ModelSpec;
import com.yahoo.squidb.processor.plugins.PluginEnvironment;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.List;

import javax.lang.model.element.VariableElement;

/**
 * An implementation of
 * {@link com.yahoo.squidb.processor.plugins.defaults.properties.generators.interfaces.TableModelPropertyGenerator}
 * for handling String fields
 */
public class BasicStringPropertyGenerator extends BasicTableModelPropertyGenerator {

    public static List<TypeName> handledColumnTypes() {
        return Collections.singletonList(TypeName.get(String.class));
    }

    public BasicStringPropertyGenerator(ModelSpec<?, ?> modelSpec, String columnName, PluginEnvironment pluginEnv) {
        super(modelSpec, columnName, pluginEnv);
    }

    public BasicStringPropertyGenerator(ModelSpec<?, ?> modelSpec, String columnName,
            String propertyName, PluginEnvironment pluginEnv) {
        super(modelSpec, columnName, propertyName, pluginEnv);
    }

    public BasicStringPropertyGenerator(ModelSpec<?, ?> modelSpec, VariableElement field, PluginEnvironment pluginEnv) {
        super(modelSpec, field, pluginEnv);
    }

    @Override
    public TypeName getTypeForAccessors() {
        return TypeName.get(String.class);
    }

    @Override
    public TypeName getPropertyType() {
        return TypeConstants.STRING_PROPERTY;
    }

    @Override
    protected Class<? extends Annotation> getDefaultAnnotationType() {
        return DefaultString.class;
    }

    @Override
    protected String getPrimitiveDefaultValueFromAnnotation() {
        DefaultString defaultString = field.getAnnotation(DefaultString.class);
        if (defaultString != null) {
            return defaultString.value();
        }
        return null;
    }

    @Override
    protected String getPrimitiveDefaultValueAsSql() {
        String primitiveDefault = getPrimitiveDefaultValueFromAnnotation();
        return primitiveDefault != null ? "'" + primitiveDefault.replace("'", "''") + "'" : null;
    }

}
