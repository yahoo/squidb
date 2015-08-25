/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.writers;

import com.yahoo.aptutils.model.CoreTypes;
import com.yahoo.aptutils.model.DeclaredTypeName;
import com.yahoo.aptutils.utils.AptUtils;
import com.yahoo.aptutils.writer.expressions.Expression;
import com.yahoo.aptutils.writer.expressions.Expressions;
import com.yahoo.aptutils.writer.parameters.MethodDeclarationParameters;
import com.yahoo.squidb.processor.TypeConstants;
import com.yahoo.squidb.processor.data.TableModelSpecWrapper;
import com.yahoo.squidb.processor.plugins.PluginContext;
import com.yahoo.squidb.processor.plugins.properties.generators.PropertyGenerator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic.Kind;

/**
 * Responsible for writing all aspects of the Java file for a generated model class.
 */
public class TableModelFileWriter extends ModelFileWriter<TableModelSpecWrapper> {

    public static final String TABLE_NAME = "TABLE";

    public TableModelFileWriter(TypeElement element, PluginContext pluginContext, AptUtils utils) {
        super(new TableModelSpecWrapper(element, pluginContext, utils), pluginContext, utils);
    }

    @Override
    protected void emitModelSpecificFields() throws IOException {
        emitTableDeclaration();
    }

    private void emitTableDeclaration() throws IOException {
        writer.writeComment("--- table declaration");
        List<Object> arguments = new ArrayList<Object>();
        arguments.add(Expressions.classObject(modelSpec.getGeneratedClassName())); // modelClass
        arguments.add(PROPERTIES_ARRAY_NAME); // properties
        arguments.add("\"" + modelSpec.getSpecAnnotation().tableName() + "\""); // name
        arguments.add(null); // database name, null by default
        if (modelSpec.isVirtualTable()) {
            if (AptUtils.isEmpty(modelSpec.getSpecAnnotation().virtualModule())) {
                utils.getMessager()
                        .printMessage(Kind.ERROR, "virtualModule should be non-empty for virtual table models",
                                modelSpec.getModelSpecElement());
            }
            arguments.add("\"" + modelSpec.getSpecAnnotation().virtualModule() + "\"");
        } else if (!modelSpec.getSpecAnnotation().tableConstraint().isEmpty()) {
            arguments.add("\"" + modelSpec.getSpecAnnotation().tableConstraint() + "\"");
        }
        writer.writeFieldDeclaration(modelSpec.getTableType(), TABLE_NAME,
                Expressions.callConstructor(modelSpec.getTableType(), arguments), TypeConstants.PUBLIC_STATIC_FINAL)
                .writeNewline();
    }

    @Override
    protected int getPropertiesArrayLength() {
        return super.getPropertiesArrayLength() + 1;
    }

    @Override
    protected void emitAllProperties() throws IOException {
        emitIdPropertyDeclaration();
        for (PropertyGenerator generator : modelSpec.getPropertyGenerators()) {
            generator.beforeEmitPropertyDeclaration(writer);
            generator.emitPropertyDeclaration(writer);
            generator.afterEmitPropertyDeclaration(writer);
            writer.writeNewline();
        }

        for (PropertyGenerator deprecatedProperty : modelSpec.getDeprecatedPropertyGenerators()) {
            deprecatedProperty.beforeEmitPropertyDeclaration(writer);
            deprecatedProperty.emitPropertyDeclaration(writer);
            deprecatedProperty.afterEmitPropertyDeclaration(writer);
            writer.writeNewline();
        }

        emitGetIdPropertyMethod();
    }

    private void emitIdPropertyDeclaration() throws IOException {
        PropertyGenerator idPropertyGenerator = modelSpec.getIdPropertyGenerator();
        if (idPropertyGenerator != null) {
            idPropertyGenerator.beforeEmitPropertyDeclaration(writer);
            idPropertyGenerator.emitPropertyDeclaration(writer);
            idPropertyGenerator.afterEmitPropertyDeclaration(writer);
        } else {
            // Default ID property
            Expression constructor;
            if (modelSpec.isVirtualTable()) {
                constructor = Expressions.callConstructor(TypeConstants.LONG_PROPERTY, TABLE_NAME,
                        Expressions.staticReference(TypeConstants.TABLE_MODEL, "ROWID"), "null");
            } else {
                constructor = Expressions.callConstructor(TypeConstants.LONG_PROPERTY, TABLE_NAME,
                        Expressions.staticReference(TypeConstants.TABLE_MODEL, "DEFAULT_ID_COLUMN"),
                        "\"PRIMARY KEY AUTOINCREMENT\"");
            }

            writer.writeFieldDeclaration(TypeConstants.LONG_PROPERTY, "ID", constructor,
                    TypeConstants.PUBLIC_STATIC_FINAL);
        }
        writer.beginInitializerBlock(true, true);
        writer.writeStatement(Expressions.callMethodOn(TABLE_NAME, "setIdProperty",
                idPropertyGenerator == null ? "ID" : idPropertyGenerator.getPropertyName()));
        writer.finishInitializerBlock(true, true);
        writer.writeNewline();
    }

    private void emitGetIdPropertyMethod() throws IOException {
        writer.writeAnnotation(CoreTypes.OVERRIDE);
        MethodDeclarationParameters params = new MethodDeclarationParameters()
                .setModifiers(Modifier.PUBLIC)
                .setReturnType(TypeConstants.LONG_PROPERTY)
                .setMethodName("getIdProperty");
        writer.beginMethodDefinition(params);
        if (modelSpec.getIdPropertyGenerator() != null) {
            writer.writeStringStatement("return " + modelSpec.getIdPropertyGenerator().getPropertyName());
        } else {
            writer.writeStringStatement("return ID");
        }
        writer.finishMethodDefinition();
    }

    @Override
    protected void writePropertiesInitializationBlock() throws IOException {
        writer.writeStatement(Expressions
                .assign(Expressions.arrayReference(PROPERTIES_ARRAY_NAME, 0), Expressions.fromString("ID")));
        for (int i = 0; i < modelSpec.getPropertyGenerators().size(); i++) {
            writer.writeStatement(Expressions
                    .assign(Expressions.arrayReference(PROPERTIES_ARRAY_NAME, i + 1),
                            Expressions.fromString(modelSpec.getPropertyGenerators().get(i).getPropertyName())));
        }
    }

    @Override
    protected void emitDefaultValuesInitializationBlock() throws IOException {
        for (PropertyGenerator generator : modelSpec.getPropertyGenerators()) {
            generator.emitPutDefault(writer, DEFAULT_VALUES_NAME);
        }
    }

    @Override
    protected void emitGettersAndSetters() throws IOException {
        super.emitGettersAndSetters();
        MethodDeclarationParameters params = new MethodDeclarationParameters()
                .setModifiers(Modifier.PUBLIC)
                .setMethodName("setId")
                .setArgumentTypes(new DeclaredTypeName("long"))
                .setArgumentNames("id")
                .setReturnType(modelSpec.getGeneratedClassName());
        writer.writeAnnotation(CoreTypes.OVERRIDE)
                .beginMethodDefinition(params)
                .writeStringStatement("super.setId(id)")
                .writeStringStatement("return this")
                .finishMethodDefinition();
    }
}
