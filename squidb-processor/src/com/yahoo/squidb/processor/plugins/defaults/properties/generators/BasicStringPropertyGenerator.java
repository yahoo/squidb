/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.plugins.defaults.properties.generators;

import com.squareup.javapoet.TypeName;
import com.yahoo.squidb.annotations.ColumnSpec;
import com.yahoo.squidb.processor.TypeConstants;
import com.yahoo.squidb.processor.data.ModelSpec;
import com.yahoo.squidb.processor.plugins.PluginEnvironment;

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
    protected String columnSpecDefaultValueToSql() {
        String defaultValue = super.columnSpecDefaultValueToSql();
        if (!ColumnSpec.DEFAULT_NONE.equals(defaultValue) && !"NULL".equals(defaultValue)) {
            return "'" + defaultValue + "'";
        }
        return defaultValue;
    }

    @Override
    protected String getDefaultValueForContentValues() {
        return "\"" + super.getDefaultValueForContentValues() + "\"";
    }

}
