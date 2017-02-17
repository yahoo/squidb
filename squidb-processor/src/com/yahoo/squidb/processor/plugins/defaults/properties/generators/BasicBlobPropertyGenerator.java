/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.plugins.defaults.properties.generators;

import com.squareup.javapoet.TypeName;
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
 * for handling blob (byte[]) fields
 */
public class BasicBlobPropertyGenerator extends BasicTableModelPropertyGenerator {

    public static List<TypeName> handledColumnTypes() {
        return Collections.singletonList(TypeConstants.BYTE_ARRAY);
    }

    public BasicBlobPropertyGenerator(ModelSpec<?, ?> modelSpec, String columnName, PluginEnvironment pluginEnv) {
        super(modelSpec, columnName, pluginEnv);
    }

    public BasicBlobPropertyGenerator(ModelSpec<?, ?> modelSpec, String columnName,
            String propertyName, PluginEnvironment pluginEnv) {
        super(modelSpec, columnName, propertyName, pluginEnv);
    }

    public BasicBlobPropertyGenerator(ModelSpec<?, ?> modelSpec, VariableElement field, PluginEnvironment pluginEnv) {
        super(modelSpec, field, pluginEnv);
    }

    @Override
    public TypeName getTypeForAccessors() {
        return TypeConstants.BYTE_ARRAY;
    }

    @Override
    public TypeName getPropertyType() {
        return TypeConstants.BLOB_PROPERTY;
    }

    @Override
    protected Class<? extends Annotation> getDefaultAnnotationType() {
        return null;
    }

    @Override
    protected Object getPrimitiveDefaultValueFromAnnotation() {
        return null;
    }
}
