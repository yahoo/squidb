/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.plugins.defaults.properties;

import com.yahoo.aptutils.model.DeclaredTypeName;
import com.yahoo.aptutils.utils.AptUtils;
import com.yahoo.squidb.annotations.PrimaryKey;
import com.yahoo.squidb.processor.TypeConstants;
import com.yahoo.squidb.processor.data.ModelSpec;
import com.yahoo.squidb.processor.data.TableModelSpecWrapper;
import com.yahoo.squidb.processor.plugins.PluginEnvironment;
import com.yahoo.squidb.processor.plugins.defaults.properties.generators.BasicBlobPropertyGenerator;
import com.yahoo.squidb.processor.plugins.defaults.properties.generators.BasicBooleanPropertyGenerator;
import com.yahoo.squidb.processor.plugins.defaults.properties.generators.BasicDoublePropertyGenerator;
import com.yahoo.squidb.processor.plugins.defaults.properties.generators.BasicIntegerPropertyGenerator;
import com.yahoo.squidb.processor.plugins.defaults.properties.generators.BasicLongPropertyGenerator;
import com.yahoo.squidb.processor.plugins.defaults.properties.generators.BasicPropertyGenerator;
import com.yahoo.squidb.processor.plugins.defaults.properties.generators.BasicStringPropertyGenerator;
import com.yahoo.squidb.processor.plugins.defaults.properties.generators.PropertyGenerator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.lang.model.element.VariableElement;
import javax.tools.Diagnostic;
import javax.tools.Diagnostic.Kind;

/**
 * This plugin controls generating property declarations, getters, and setters for fields in a table model. It can
 * create instances of {@link PropertyGenerator} for each of the basic supported column types (String, int, long, etc.)
 */
public class TableModelSpecFieldPlugin extends BaseFieldPlugin {

    private Map<DeclaredTypeName, Class<? extends BasicPropertyGenerator>> generatorMap = new HashMap<>();

    public TableModelSpecFieldPlugin(ModelSpec<?> modelSpec, PluginEnvironment pluginEnv) {
        super(modelSpec, pluginEnv);
        registerBasicPropertyGenerators();
    }

    @Override
    public boolean hasChangesForModelSpec() {
        return modelSpec instanceof TableModelSpecWrapper;
    }

    @Override
    public boolean processVariableElement(VariableElement field, DeclaredTypeName fieldType) {
        if (TypeConstants.isConstant(field)) {
            // Looks like a constant, ignore
            return false;
        } else {
            if (field.getAnnotation(PrimaryKey.class) != null) {
                if (modelSpec instanceof TableModelSpecWrapper
                        && ((TableModelSpecWrapper) modelSpec).isVirtualTable()) {
                    utils.getMessager().printMessage(Kind.ERROR,
                            "Virtual tables cannot declare a custom primary key", field);
                } else if (modelSpec.hasMetadata(TableModelSpecWrapper.METADATA_KEY_HAS_PRIMARY_KEY)) {
                    utils.getMessager().printMessage(Diagnostic.Kind.ERROR,
                            "Only a single field can be annotated as @PrimaryKey. If you want a multi-column primary "
                                    + "key, specify it using SQL in TableModelSpec#tableConstraint() and set "
                                    + "TableModelSpec#noRowIdAlias() to true in your TableModelSpec annotation.",
                            field);
                } else {
                    modelSpec.putMetadata(TableModelSpecWrapper.METADATA_KEY_HAS_PRIMARY_KEY, true);
                    if (TypeConstants.isIntegerType(fieldType)) {
                        modelSpec.putMetadata(TableModelSpecWrapper.METADATA_KEY_ROWID_ALIAS_PROPERTY_GENERATOR,
                                getPropertyGenerator(field, fieldType));
                        return true;
                    } else {
                        return super.processVariableElement(field, fieldType);
                    }
                }
            } else {
                return super.processVariableElement(field, fieldType);
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
        Class<? extends BasicPropertyGenerator> generatorClass;
        if (isIntegerPrimaryKey(field, fieldType)) {
            // Force INTEGER PRIMARY KEY properties to be LongProperty, even if declared as e.g. int
            generatorClass = getLongPropertyGenerator();
        } else {
            generatorClass = generatorMap.get(fieldType);
        }
        try {
            BasicPropertyGenerator propertyGenerator = generatorClass.getConstructor(ModelSpec.class,
                    VariableElement.class, AptUtils.class).newInstance(modelSpec, field, utils);
            if ("rowid".equalsIgnoreCase(propertyGenerator.getColumnName())) {
                utils.getMessager().printMessage(Kind.ERROR, "Columns in a table model spec cannot be named rowid, as "
                        + "they would clash with the internal SQLite rowid column.");
                return null;
            }

            String propertyName = propertyGenerator.getPropertyName();
            if ("ID".equals(propertyName) && !isIntegerPrimaryKey(field, fieldType)) {
                utils.getMessager().printMessage(Kind.ERROR, "User-defined non-primary-key columns cannot currently be "
                        + "named 'ID' for the sake of backwards compatibility. This restriction will be removed in a "
                        + "future version of SquiDB.");
                return null;
            }

            return propertyGenerator;
        } catch (Exception e) {
            utils.getMessager().printMessage(Kind.ERROR,
                    "Exception instantiating PropertyGenerator: " + generatorClass + ", " + e);
        }
        return null;
    }

    private boolean isIntegerPrimaryKey(VariableElement field, DeclaredTypeName fieldType) {
        return field.getAnnotation(PrimaryKey.class) != null &&
                TypeConstants.isIntegerType(fieldType);
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
            Class<? extends BasicPropertyGenerator> generatorClass) {
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
