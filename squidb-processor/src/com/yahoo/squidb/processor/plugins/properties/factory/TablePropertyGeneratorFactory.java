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
import com.yahoo.squidb.processor.data.ModelSpec;
import com.yahoo.squidb.processor.plugins.Plugin;
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

import javax.lang.model.element.VariableElement;
import javax.tools.Diagnostic.Kind;

public class TablePropertyGeneratorFactory extends Plugin {

    private Map<DeclaredTypeName, Class<? extends PropertyGenerator>> generatorMap
            = new HashMap<DeclaredTypeName, Class<? extends PropertyGenerator>>();

    public TablePropertyGeneratorFactory(AptUtils utils) {
        super(utils);
        registerBasicPropertyGenerators();
    }

    @Override
    public boolean hasPropertyGeneratorForField(ModelSpec<?> modelSpec, VariableElement field, DeclaredTypeName fieldType) {
        return modelSpec.getModelSpecElement().getAnnotation(TableModelSpec.class) != null
                && generatorMap.containsKey(fieldType);
    }

    @Override
    public PropertyGenerator getPropertyGenerator(ModelSpec<?> modelSpec, VariableElement field, DeclaredTypeName fieldType) {
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
