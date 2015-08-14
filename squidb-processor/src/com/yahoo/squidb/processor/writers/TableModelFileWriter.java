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
import com.yahoo.squidb.annotations.PrimaryKey;
import com.yahoo.squidb.annotations.TableModelSpec;
import com.yahoo.squidb.processor.TypeConstants;
import com.yahoo.squidb.processor.properties.factory.PropertyGeneratorFactory;
import com.yahoo.squidb.processor.properties.generators.BasicLongPropertyGenerator;
import com.yahoo.squidb.processor.properties.generators.PropertyGenerator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.tools.Diagnostic.Kind;

/**
 * Responsible for writing all aspects of the Java file for a generated model class.
 */
public class TableModelFileWriter extends ModelFileWriter<TableModelSpec> {

    public static final String TABLE_NAME = "TABLE";

    private DeclaredTypeName tableType;
    private PropertyGenerator idPropertyGenerator;

    public TableModelFileWriter(TypeElement element, PropertyGeneratorFactory propertyGeneratorFactory,
            AptUtils utils) {
        super(element, TableModelSpec.class, propertyGeneratorFactory, utils);
        if (isVirtualTable()) {
            tableType = TypeConstants.VIRTUAL_TABLE;
        } else {
            tableType = TypeConstants.TABLE;
        }
    }

    @Override
    protected String getGeneratedClassName() {
        return modelSpec.className();
    }

    @Override
    protected DeclaredTypeName getModelSuperclass() {
        return TypeConstants.TABLE_MODEL;
    }

    private boolean isVirtualTable() {
        return !AptUtils.isEmpty(modelSpec.virtualModule());
    }

    @Override
    protected void emitModelSpecificFields() throws IOException {
        emitTableDeclaration();
    }

    @Override
    protected void processVariableElement(VariableElement e, DeclaredTypeName typeName) {
        Set<Modifier> modifiers = e.getModifiers();
        if (modifiers.containsAll(TypeConstants.PUBLIC_STATIC_FINAL)) {
            if (e.getAnnotation(Deprecated.class) != null) {
                return;
            }
            if (TypeConstants.isPropertyType(typeName)) {
                utils.getMessager().printMessage(Kind.WARNING, "Can't copy Property constants to model "
                        + "defintion--they'd become part of the model", e);
            } else {
                constantElements.add(e);
            }
        } else {
            if (e.getAnnotation(PrimaryKey.class) != null) {
                if (!BasicLongPropertyGenerator.handledColumnTypes().contains(typeName)) {
                    utils.getMessager().printMessage(Kind.ERROR,
                            "Only long primary key columns are supported at this time", e);
                } else if (idPropertyGenerator != null) {
                    utils.getMessager().printMessage(Kind.ERROR,
                            "Only a single primary key column is supported at this time", e);
                } else if (isVirtualTable()) {
                    utils.getMessager().printMessage(Kind.ERROR,
                            "Virtual tables cannot declare a custom primary key", e);
                } else {
                    idPropertyGenerator = propertyGeneratorForElement(e);
                }
            } else {
                initializePropertyGenerator(e);
            }
        }
    }

    @Override
    protected Collection<DeclaredTypeName> getModelSpecificImports() {
        ArrayList<DeclaredTypeName> imports = new ArrayList<DeclaredTypeName>();
        imports.add(TypeConstants.LONG_PROPERTY);
        imports.add(TypeConstants.TABLE_MODEL);
        imports.add(tableType);
        return imports;
    }

    private void emitTableDeclaration() throws IOException {
        writer.writeComment("--- table declaration");
        List<Object> arguments = new ArrayList<Object>();
        arguments.add(Expressions.classObject(generatedClassName)); // modelClass
        arguments.add(PROPERTIES_ARRAY_NAME); // properties
        arguments.add("\"" + modelSpec.tableName() + "\""); // name
        arguments.add(null); // database name, null by default
        if (isVirtualTable()) {
            if (AptUtils.isEmpty(modelSpec.virtualModule())) {
                utils.getMessager()
                        .printMessage(Kind.ERROR, "virtualModule should be non-empty for virtual table models",
                                modelSpecElement);
            }
            arguments.add("\"" + modelSpec.virtualModule() + "\"");
        } else if (!modelSpec.tableConstraint().isEmpty()) {
            arguments.add("\"" + modelSpec.tableConstraint() + "\"");
        }
        writer.writeFieldDeclaration(tableType,
                TABLE_NAME, Expressions.callConstructor(tableType, arguments),
                TypeConstants.PUBLIC_STATIC_FINAL)
                .writeNewline();
    }

    @Override
    protected int getPropertiesArrayLength() {
        return super.getPropertiesArrayLength() + 1;
    }

    @Override
    protected void emitAllProperties() throws IOException {
        emitIdPropertyDeclaration();
        for (PropertyGenerator generator : propertyGenerators) {
            generator.beforeEmitPropertyDeclaration(writer);
            generator.emitPropertyDeclaration(writer);
            generator.afterEmitPropertyDeclaration(writer);
            writer.writeNewline();
        }

        for (PropertyGenerator deprecatedProperty : deprecatedPropertyGenerators) {
            deprecatedProperty.beforeEmitPropertyDeclaration(writer);
            deprecatedProperty.emitPropertyDeclaration(writer);
            deprecatedProperty.afterEmitPropertyDeclaration(writer);
            writer.writeNewline();
        }

        emitGetIdPropertyMethod();
    }

    private void emitIdPropertyDeclaration() throws IOException {
        if (idPropertyGenerator != null) {
            idPropertyGenerator.beforeEmitPropertyDeclaration(writer);
            idPropertyGenerator.emitPropertyDeclaration(writer);
            idPropertyGenerator.afterEmitPropertyDeclaration(writer);
        } else {
            // Default ID property
            Expression constructor;
            if (isVirtualTable()) {
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
        if (idPropertyGenerator != null) {
            writer.writeStringStatement("return " + idPropertyGenerator.getPropertyName());
        } else {
            writer.writeStringStatement("return ID");
        }
        writer.finishMethodDefinition();
    }

    @Override
    protected void writePropertiesInitializationBlock() throws IOException {
        writer.writeStatement(Expressions
                .assign(Expressions.arrayReference(PROPERTIES_ARRAY_NAME, 0), Expressions.fromString("ID")));
        for (int i = 0; i < propertyGenerators.size(); i++) {
            writer.writeStatement(Expressions
                    .assign(Expressions.arrayReference(PROPERTIES_ARRAY_NAME, i + 1),
                            Expressions.fromString(propertyGenerators.get(i).getPropertyName())));
        }
    }

    @Override
    protected void emitDefaultValuesInitializationBlock() throws IOException {
        for (PropertyGenerator generator : propertyGenerators) {
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
                .setReturnType(generatedClassName);
        writer.writeAnnotation(CoreTypes.OVERRIDE)
                .beginMethodDefinition(params)
                .writeStringStatement("super.setId(id)")
                .writeStringStatement("return this")
                .finishMethodDefinition();
    }
}
