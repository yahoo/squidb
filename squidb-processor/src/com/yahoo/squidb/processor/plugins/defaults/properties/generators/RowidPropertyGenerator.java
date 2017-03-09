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

    public static final String DEFAULT_ROWID_GETTER_NAME = "getRowId";
    public static final String DEFAULT_ROWID_SETTER_NAME = "setRowId";

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
    protected MethodSpec.Builder getterMethodParams() {
        MethodSpec.Builder params = super.getterMethodParams();
        addAccessorDocumentationForRowids(params, true);
        return params;
    }

    @Override
    protected MethodSpec.Builder setterMethodParams(String argName) {
        MethodSpec.Builder params = super.setterMethodParams(argName);
        addAccessorDocumentationForRowids(params, false);
        return params;
    }

    private void addAccessorDocumentationForRowids(MethodSpec.Builder params, boolean getter) {
        if (isUnaliasedRowid()) {
            params.addAnnotation(Override.class);
        } else {
            params.addJavadoc("This " + (getter ? "getter" : "setter") + " is an alias for " +
                    (getter ? "get" : "set") + "RowId(), as the underlying column is an INTEGER PRIMARY KEY\n");
        }
    }

    @Override
    public String getterMethodName() {
        // Camel case translation doesn't quite work in this case, so override
        if (isUnaliasedRowid()) {
            return DEFAULT_ROWID_GETTER_NAME;
        }
        return super.getterMethodName();
    }

    @Override
    public String setterMethodName() {
        // Camel case translation doesn't quite work in this case, so override
        if (isUnaliasedRowid()) {
            return DEFAULT_ROWID_SETTER_NAME;
        }
        return super.setterMethodName();
    }

    private boolean isUnaliasedRowid() {
        return TableModelSpecFieldPlugin.DEFAULT_ROWID_PROPERTY_NAME.equals(propertyName);
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
