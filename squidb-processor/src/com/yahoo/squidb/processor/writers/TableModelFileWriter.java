/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.writers;

import com.yahoo.aptutils.model.CoreTypes;
import com.yahoo.aptutils.utils.AptUtils;
import com.yahoo.aptutils.writer.expressions.Expression;
import com.yahoo.aptutils.writer.expressions.Expressions;
import com.yahoo.aptutils.writer.parameters.MethodDeclarationParameters;
import com.yahoo.squidb.processor.TypeConstants;
import com.yahoo.squidb.processor.data.TableModelSpecWrapper;
import com.yahoo.squidb.processor.plugins.PluginEnvironment;
import com.yahoo.squidb.processor.plugins.defaults.properties.generators.PropertyGenerator;

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
        arguments.add("\"" + modelSpec.getSpecAnnotation().tableName() + "\""); // name
        arguments.add(null); // database name, null by default
        if (modelSpec.isVirtualTable()) {
            if (AptUtils.isEmpty(modelSpec.getSpecAnnotation().virtualModule())) {
                utils.getMessager()
                        .printMessage(Kind.ERROR, "virtualModule should be non-empty for virtual table models",
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
    protected int getPropertiesArrayLength() {
        return super.getPropertiesArrayLength() + 1;
    }

    @Override
    protected void emitAllProperties() throws IOException {
        emitRowIdPropertyDeclaration();
        for (PropertyGenerator generator : modelSpec.getPropertyGenerators()) {
            modelSpec.getPluginBundle().beforeEmitPropertyDeclaration(writer, generator);
            generator.emitPropertyDeclaration(writer);
            modelSpec.getPluginBundle().afterEmitPropertyDeclaration(writer, generator);
            writer.writeNewline();
        }

        for (PropertyGenerator deprecatedProperty : modelSpec.getDeprecatedPropertyGenerators()) {
            modelSpec.getPluginBundle().beforeEmitPropertyDeclaration(writer, deprecatedProperty);
            deprecatedProperty.emitPropertyDeclaration(writer);
            modelSpec.getPluginBundle().afterEmitPropertyDeclaration(writer, deprecatedProperty);
            writer.writeNewline();
        }

        emitGetRowIdPropertyMethod();
    }

    private void emitRowIdPropertyDeclaration() throws IOException {
        PropertyGenerator rowidPropertyGenerator = modelSpec.getRowIdAliasPropertyGenerator();
        String propertyName = modelSpec.getRowIdAliasPropertyName();
        if (rowidPropertyGenerator != null) {
            modelSpec.getPluginBundle().beforeEmitPropertyDeclaration(writer, rowidPropertyGenerator);
            rowidPropertyGenerator.emitPropertyDeclaration(writer);
            modelSpec.getPluginBundle().afterEmitPropertyDeclaration(writer, rowidPropertyGenerator);
        } else {
            // Default ID property
            Expression constructor;
            if (modelSpec.shouldGenerateROWIDProperty()) {
                constructor = Expressions.callConstructor(TypeConstants.LONG_PROPERTY,
                        TABLE_MODEL_NAME, Expressions.staticReference(TypeConstants.TABLE_MODEL, "ROWID"));
            } else {
                constructor = Expressions.callConstructor(TypeConstants.LONG_PROPERTY,
                        TABLE_MODEL_NAME, Expressions.staticReference(TypeConstants.TABLE_MODEL, "DEFAULT_ID_COLUMN"),
                        "\"PRIMARY KEY AUTOINCREMENT\"");
                utils.getMessager().printMessage(Kind.WARNING, "Model class " + modelSpec.getGeneratedClassName() +
                        " is currently generating an integer primary key ID property to act as an alias to the table's "
                        + "rowid. Future versions of SquiDB will remove this default property for the sake of better "
                        + "support for arbitrary primary keys. If you are using the ID property, you should update "
                        + "your model spec by explicitly declaring a field, named id with column name '_id' and "
                        + "annotated with @PrimaryKey", modelSpec.getModelSpecElement());
            }

            writer.writeFieldDeclaration(TypeConstants.LONG_PROPERTY, propertyName, constructor,
                    TypeConstants.PUBLIC_STATIC_FINAL);
            if (modelSpec.isVirtualTable()) {
                writer.writeAnnotation(CoreTypes.DEPRECATED);
                writer.writeFieldDeclaration(TypeConstants.LONG_PROPERTY,
                        TableModelSpecWrapper.DEFAULT_ID_PROPERTY_NAME,
                        Expressions.fromString(TableModelSpecWrapper.DEFAULT_ROWID_PROPERTY_NAME),
                        TypeConstants.PUBLIC_STATIC_FINAL);
            }
        }
        writer.beginInitializerBlock(true, true);
        writer.writeStatement(Expressions.callMethodOn(TABLE_NAME, "setRowIdProperty", propertyName));
        writer.finishInitializerBlock(true, true);
        writer.writeNewline();
    }

    private void emitGetRowIdPropertyMethod() throws IOException {
        writer.writeAnnotation(CoreTypes.OVERRIDE);
        MethodDeclarationParameters params = new MethodDeclarationParameters()
                .setModifiers(Modifier.PUBLIC)
                .setReturnType(TypeConstants.LONG_PROPERTY)
                .setMethodName("getRowIdProperty");
        writer.beginMethodDefinition(params);
        writer.writeStringStatement("return " + modelSpec.getRowIdAliasPropertyName());
        writer.finishMethodDefinition();
    }

    @Override
    protected void writePropertiesInitializationBlock() throws IOException {
        writer.writeStatement(Expressions
                .assign(Expressions.arrayReference(PROPERTIES_ARRAY_NAME, 0),
                        Expressions.fromString(modelSpec.getRowIdAliasPropertyName())));
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
        if (!pluginEnv.hasOption(PluginEnvironment.OPTIONS_DISABLE_DEFAULT_GETTERS_AND_SETTERS)) {

            MethodDeclarationParameters params = new MethodDeclarationParameters()
                    .setModifiers(Modifier.PUBLIC)
                    .setMethodName("setId")
                    .setArgumentTypes(CoreTypes.PRIMITIVE_LONG)
                    .setArgumentNames("id")
                    .setReturnType(modelSpec.getGeneratedClassName());
            writer.writeAnnotation(CoreTypes.DEPRECATED);
            writer.writeAnnotation(CoreTypes.OVERRIDE)
                    .beginMethodDefinition(params)
                    .writeStringStatement("super.setRowId(id)")
                    .writeStringStatement("return this")
                    .finishMethodDefinition();

            params = new MethodDeclarationParameters()
                    .setModifiers(Modifier.PUBLIC)
                    .setMethodName("setRowId")
                    .setArgumentTypes(CoreTypes.PRIMITIVE_LONG)
                    .setArgumentNames("rowid")
                    .setReturnType(modelSpec.getGeneratedClassName());
            writer.writeAnnotation(CoreTypes.OVERRIDE)
                    .beginMethodDefinition(params)
                    .writeStringStatement("super.setRowId(rowid)")
                    .writeStringStatement("return this")
                    .finishMethodDefinition();
        }
    }
}
