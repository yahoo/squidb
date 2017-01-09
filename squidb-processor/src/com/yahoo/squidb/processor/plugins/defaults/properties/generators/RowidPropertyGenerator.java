/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.plugins.defaults.properties.generators;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.yahoo.squidb.processor.data.ModelSpec;
import com.yahoo.squidb.processor.plugins.PluginEnvironment;
import com.yahoo.squidb.processor.plugins.defaults.properties.TableModelSpecFieldPlugin;

import javax.lang.model.element.VariableElement;

/**
 * Special case of BasicLongPropertyGenerator specific to ROWID or INTEGER PRIMARY KEY properties
 */
public class RowidPropertyGenerator extends BasicLongPropertyGenerator {

    public RowidPropertyGenerator(ModelSpec<?, ?> modelSpec, String columnName, PluginEnvironment pluginEnv) {
        super(modelSpec, columnName, pluginEnv);
    }

    public RowidPropertyGenerator(ModelSpec<?, ?> modelSpec, String columnName,
            String propertyName, PluginEnvironment pluginEnv) {
        super(modelSpec, columnName, propertyName, pluginEnv);
    }

    public RowidPropertyGenerator(ModelSpec<?, ?> modelSpec, VariableElement field, PluginEnvironment pluginEnv) {
        super(modelSpec, field, pluginEnv);
    }

    @Override
    public TypeName getTypeForAccessors() {
        return TypeName.LONG;
    }

    @Override
    public String getterMethodName() {
        // Camel case translation doesn't quite work in this case, so override
        if (TableModelSpecFieldPlugin.DEFAULT_ROWID_PROPERTY_NAME.equals(propertyName)) {
            return "getRowId";
        }
        return super.getterMethodName();
    }

    @Override
    public String setterMethodName() {
        // Camel case translation doesn't quite work in this case, so override
        if (TableModelSpecFieldPlugin.DEFAULT_ROWID_PROPERTY_NAME.equals(propertyName)) {
            return "setRowId";
        }
        return super.setterMethodName();
    }

    @Override
    protected void writeGetterBody(CodeBlock.Builder body, MethodSpec methodParams) {
        body.addStatement("return super.getRowId()");
    }

    @Override
    protected void writeSetterBody(CodeBlock.Builder body, MethodSpec methodParams) {
        body.addStatement("super.setRowId($L)", methodParams.parameters.get(0).name);
        body.addStatement("return this");
    }
}
