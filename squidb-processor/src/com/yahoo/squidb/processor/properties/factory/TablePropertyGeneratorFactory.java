/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.properties.factory;

import com.yahoo.aptutils.model.DeclaredTypeName;
import com.yahoo.aptutils.utils.AptUtils;
import com.yahoo.squidb.annotations.TableModelSpec;
import com.yahoo.squidb.processor.properties.generators.BasicBlobPropertyGenerator;
import com.yahoo.squidb.processor.properties.generators.BasicBooleanPropertyGenerator;
import com.yahoo.squidb.processor.properties.generators.BasicDoublePropertyGenerator;
import com.yahoo.squidb.processor.properties.generators.BasicIntegerPropertyGenerator;
import com.yahoo.squidb.processor.properties.generators.BasicLongPropertyGenerator;
import com.yahoo.squidb.processor.properties.generators.BasicStringPropertyGenerator;
import com.yahoo.squidb.processor.properties.generators.PropertyGenerator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.tools.Diagnostic.Kind;

public class TablePropertyGeneratorFactory extends PluggablePropertyGeneratorFactory {

    private Map<DeclaredTypeName, Class<? extends PropertyGenerator>> generatorMap
            = new HashMap<DeclaredTypeName, Class<? extends PropertyGenerator>>();

    public TablePropertyGeneratorFactory(AptUtils utils) {
        super(utils);
        registerBasicPropertyGenerators();
    }

    @Override
    public boolean canHandleElement(VariableElement element, DeclaredTypeName elementType, TypeElement parentElement) {
        return parentElement.getAnnotation(TableModelSpec.class) != null && generatorMap.containsKey(elementType);
    }

    @Override
    public PropertyGenerator getPropertyGenerator(VariableElement element, DeclaredTypeName elementType,
            DeclaredTypeName modelClass) {
        Class<? extends PropertyGenerator> generatorClass = generatorMap.get(elementType);
        try {
            return generatorClass.getConstructor(VariableElement.class, DeclaredTypeName.class, AptUtils.class)
                    .newInstance(element, modelClass, utils);
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
