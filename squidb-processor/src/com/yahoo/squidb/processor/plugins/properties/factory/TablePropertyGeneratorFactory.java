/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.plugins.properties.factory;

import com.yahoo.aptutils.model.DeclaredTypeName;
import com.yahoo.aptutils.utils.AptUtils;
import com.yahoo.squidb.annotations.PrimaryKey;
import com.yahoo.squidb.annotations.TableModelSpec;
import com.yahoo.squidb.processor.TypeConstants;
import com.yahoo.squidb.processor.data.ModelSpec;
import com.yahoo.squidb.processor.data.TableModelSpecWrapper;
import com.yahoo.squidb.processor.plugins.properties.generators.BasicBlobPropertyGenerator;
import com.yahoo.squidb.processor.plugins.properties.generators.BasicBooleanPropertyGenerator;
import com.yahoo.squidb.processor.plugins.properties.generators.BasicDoublePropertyGenerator;
import com.yahoo.squidb.processor.plugins.properties.generators.BasicIdPropertyGenerator;
import com.yahoo.squidb.processor.plugins.properties.generators.BasicIntegerPropertyGenerator;
import com.yahoo.squidb.processor.plugins.properties.generators.BasicLongPropertyGenerator;
import com.yahoo.squidb.processor.plugins.properties.generators.BasicStringPropertyGenerator;
import com.yahoo.squidb.processor.plugins.properties.generators.PropertyGenerator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;
import javax.tools.Diagnostic;
import javax.tools.Diagnostic.Kind;

/**
 * This plugin controls generating property declarations, getters, and setters for fields in a table model. It can
 * create instances of {@link PropertyGenerator} for each of the basic supported column types (String, int, long, etc.)
 */
public class TablePropertyGeneratorFactory extends PropertyGeneratorPlugin<TableModelSpec> {

    private Map<DeclaredTypeName, Class<? extends PropertyGenerator>> generatorMap
            = new HashMap<DeclaredTypeName, Class<? extends PropertyGenerator>>();

    public TablePropertyGeneratorFactory(ModelSpec<?> modelSpec, AptUtils utils) {
        super(modelSpec, utils);
        registerBasicPropertyGenerators();
    }

    @Override
    public boolean canProcessModelSpec() {
        return modelSpec instanceof TableModelSpecWrapper;
    }

    @Override
    public boolean processVariableElement(VariableElement field, DeclaredTypeName fieldType) {
        utils.getMessager().printMessage(Kind.WARNING, "Table plugin processing field " + field.getSimpleName());
        Set<Modifier> modifiers = field.getModifiers();
        if (modifiers.containsAll(TypeConstants.PUBLIC_STATIC_FINAL)) {
            if (field.getAnnotation(Deprecated.class) != null) {
                return true;
            }
            if (TypeConstants.isPropertyType(fieldType)) {
                utils.getMessager().printMessage(Diagnostic.Kind.WARNING, "Can't copy Property constants to model "
                        + "definition--they'd become part of the model", field);
            } else {
                modelSpec.addConstantElement(field);
                return true;
            }
        } else {
            if (field.getAnnotation(PrimaryKey.class) != null) {
                if (!BasicLongPropertyGenerator.handledColumnTypes().contains(fieldType)) {
                    utils.getMessager().printMessage(Diagnostic.Kind.ERROR,
                            "Only long primary key columns are supported at this time.", field);
                } else if (modelSpec.hasMetadata(TableModelSpecWrapper.METADATA_KEY_ID_PROPERTY_GENERATOR)) {
                    utils.getMessager().printMessage(Diagnostic.Kind.ERROR,
                            "Only a single primary key column is supported at this time.", field);
                } else {
                    modelSpec.attachMetadata(TableModelSpecWrapper.METADATA_KEY_ID_PROPERTY_GENERATOR,
                            getPropertyGenerator(field, fieldType));
                    return true;
                }
            } else {
                return createPropertyGenerator(field, fieldType);
            }
        }
        return false;
    }

    @Override
    protected boolean hasPropertyGeneratorForField(VariableElement field, DeclaredTypeName fieldType) {
        return generatorMap.containsKey(fieldType);
    }

    @Override
    protected PropertyGenerator getPropertyGenerator(VariableElement field, DeclaredTypeName fieldType) {
        Class<? extends PropertyGenerator> generatorClass = generatorMap.get(fieldType);
        try {
            if (field.getAnnotation(PrimaryKey.class) != null &&
                    BasicLongPropertyGenerator.class.equals(generatorClass)) {
                return new BasicIdPropertyGenerator(modelSpec, field, utils);
            }
            return generatorClass.getConstructor(ModelSpec.class, VariableElement.class, AptUtils.class)
                    .newInstance(modelSpec, field, utils);
        } catch (Exception e) {
            utils.getMessager().printMessage(Kind.ERROR,
                    "Exception instantiating PropertyGenerator: " + generatorClass + ", " + e);
        }
        return null;
    }

    private void registerBasicPropertyGenerators() {
        registerHandledTypes(BasicStringPropertyGenerator.handledColumnTypes(), getStringPropertyGenerator());
        registerHandledTypes(BasicLongPropertyGenerator.handledColumnTypes(), getLongPropertyGenerator());
        registerHandledTypes(BasicIntegerPropertyGenerator.handledColumnTypes(), getIntegerPropertyGenerator());
        registerHandledTypes(BasicDoublePropertyGenerator.handledColumnTypes(), getDoublePropertyGenerator());
        registerHandledTypes(BasicBooleanPropertyGenerator.handledColumnTypes(), getBooleanPropertyGenerator());
        registerHandledTypes(BasicBlobPropertyGenerator.handledColumnTypes(), getBlobPropertyGenerator());
    }

    private void registerHandledTypes(List<DeclaredTypeName> handledTypes,
            Class<? extends PropertyGenerator> generatorClass) {
        for (DeclaredTypeName type : handledTypes) {
            generatorMap.put(type, generatorClass);
        }
    }

    protected Class<? extends BasicStringPropertyGenerator> getStringPropertyGenerator() {
        return BasicStringPropertyGenerator.class;
    }

    protected Class<? extends BasicLongPropertyGenerator> getLongPropertyGenerator() {
        return BasicLongPropertyGenerator.class;
    }

    protected Class<? extends BasicIntegerPropertyGenerator> getIntegerPropertyGenerator() {
        return BasicIntegerPropertyGenerator.class;
    }

    protected Class<? extends BasicDoublePropertyGenerator> getDoublePropertyGenerator() {
        return BasicDoublePropertyGenerator.class;
    }

    protected Class<? extends BasicBooleanPropertyGenerator> getBooleanPropertyGenerator() {
        return BasicBooleanPropertyGenerator.class;
    }

    protected Class<? extends BasicBlobPropertyGenerator> getBlobPropertyGenerator() {
        return BasicBlobPropertyGenerator.class;
    }
}