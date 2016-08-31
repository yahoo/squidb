/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.plugins.defaults.properties.generators;

import com.yahoo.aptutils.model.CoreTypes;
import com.yahoo.aptutils.model.DeclaredTypeName;
import com.yahoo.aptutils.utils.AptUtils;
import com.yahoo.aptutils.writer.JavaFileWriter;
import com.yahoo.aptutils.writer.parameters.MethodDeclarationParameters;
import com.yahoo.squidb.processor.data.ModelSpec;
import com.yahoo.squidb.processor.plugins.defaults.properties.TableModelSpecFieldPlugin;

import java.io.IOException;

import javax.lang.model.element.VariableElement;

/**
 * Special case of BasicLongPropertyGenerator specific to ROWID or INTEGER PRIMARY KEY properties
 */
public class RowidPropertyGenerator extends BasicLongPropertyGenerator {

    public RowidPropertyGenerator(ModelSpec<?> modelSpec, String columnName, AptUtils utils) {
        super(modelSpec, columnName, utils);
    }

    public RowidPropertyGenerator(ModelSpec<?> modelSpec, String columnName,
            String propertyName, AptUtils utils) {
        super(modelSpec, columnName, propertyName, utils);
    }

    public RowidPropertyGenerator(ModelSpec<?> modelSpec, VariableElement field, AptUtils utils) {
        super(modelSpec, field, utils);
    }

    @Override
    public DeclaredTypeName getTypeForAccessors() {
        return CoreTypes.PRIMITIVE_LONG;
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
    protected void writeGetterBody(JavaFileWriter writer, MethodDeclarationParameters params) throws IOException {
        writer.writeStringStatement("return super.getRowId()");
    }

    @Override
    protected void writeSetterBody(JavaFileWriter writer, MethodDeclarationParameters params) throws IOException {
        writer.writeStringStatement("super.setRowId(" + params.getArgumentNames().get(0) + ")");
        writer.writeStringStatement("return this");
    }
}
