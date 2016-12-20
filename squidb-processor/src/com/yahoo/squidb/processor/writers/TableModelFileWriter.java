/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.writers;

import com.yahoo.aptutils.utils.AptUtils;
import com.yahoo.aptutils.writer.expressions.Expressions;
import com.yahoo.squidb.processor.TypeConstants;
import com.yahoo.squidb.processor.data.TableModelSpecWrapper;
import com.yahoo.squidb.processor.plugins.PluginEnvironment;
import com.yahoo.squidb.processor.plugins.defaults.properties.generators.interfaces.TableModelPropertyGenerator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.lang.model.element.TypeElement;

/**
 * Responsible for writing all aspects of the Java file for a generated model class.
 */
public class TableModelFileWriter extends ModelFileWriter<TableModelSpecWrapper> {

    public static final String TABLE_NAME = "TABLE";
    public static final String TABLE_MODEL_NAME = "TABLE_MODEL_NAME";

    public TableModelFileWriter(TypeElement element, PluginEnvironment pluginEnv, AptUtils utils) {
        super(new TableModelSpecWrapper(element, pluginEnv, utils), pluginEnv, utils);
    }

    @Override
    protected void emitModelSpecificFields() throws IOException {
        emitTableDeclaration();
    }

    private void emitTableDeclaration() throws IOException {
        writer.writeComment("--- table declaration");
        List<Object> arguments = new ArrayList<>();
        arguments.add(Expressions.classObject(modelSpec.getGeneratedClassName())); // modelClass
        arguments.add(PROPERTIES_ARRAY_NAME); // properties
        arguments.add("\"" + modelSpec.getSpecAnnotation().tableName().trim() + "\""); // name
        arguments.add(null); // database name, null by default
        if (modelSpec.isVirtualTable()) {
            if (AptUtils.isEmpty(modelSpec.getSpecAnnotation().virtualModule())) {
                modelSpec.logError("virtualModule should be non-empty for virtual table models",
                        modelSpec.getModelSpecElement());
            }
            arguments.add("\"" + modelSpec.getSpecAnnotation().virtualModule() + "\"");
        } else if (!AptUtils.isEmpty(modelSpec.getSpecAnnotation().tableConstraint())) {
            arguments.add("\"" + modelSpec.getSpecAnnotation().tableConstraint() + "\"");
        }
        writer.writeFieldDeclaration(modelSpec.getTableType(), TABLE_NAME,
                Expressions.callConstructor(modelSpec.getTableType(), arguments), TypeConstants.PUBLIC_STATIC_FINAL);
        writer.writeFieldDeclaration(TypeConstants.TABLE_MODEL_NAME, TABLE_MODEL_NAME,
                Expressions.callConstructor(TypeConstants.TABLE_MODEL_NAME,
                        Expressions.classObject(modelSpec.getGeneratedClassName()),
                        Expressions.callMethodOn(TableModelFileWriter.TABLE_NAME, "getName")),
                TypeConstants.PUBLIC_STATIC_FINAL);
        writer.writeNewline();
    }

    @Override
    protected void emitAllProperties() throws IOException {
        for (TableModelPropertyGenerator generator : modelSpec.getPropertyGenerators()) {
            modelSpec.getPluginBundle().beforeEmitPropertyDeclaration(writer, generator);
            generator.emitTablePropertyDeclaration(writer, TABLE_MODEL_NAME);
            modelSpec.getPluginBundle().afterEmitPropertyDeclaration(writer, generator);
            writer.writeNewline();
        }

        for (TableModelPropertyGenerator deprecatedProperty : modelSpec.getDeprecatedPropertyGenerators()) {
            modelSpec.getPluginBundle().beforeEmitPropertyDeclaration(writer, deprecatedProperty);
            deprecatedProperty.emitTablePropertyDeclaration(writer, TABLE_MODEL_NAME);
            modelSpec.getPluginBundle().afterEmitPropertyDeclaration(writer, deprecatedProperty);
            writer.writeNewline();
        }
    }

    @Override
    protected void emitDefaultValuesInitializationBlock() throws IOException {
        for (TableModelPropertyGenerator generator : modelSpec.getPropertyGenerators()) {
            generator.emitPutDefault(writer, DEFAULT_VALUES_NAME);
        }
    }
}
