/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.plugins.defaults.properties;

import com.yahoo.aptutils.model.CoreTypes;
import com.yahoo.aptutils.model.DeclaredTypeName;
import com.yahoo.aptutils.utils.AptUtils;
import com.yahoo.aptutils.writer.JavaFileWriter;
import com.yahoo.aptutils.writer.expressions.Expressions;
import com.yahoo.aptutils.writer.parameters.MethodDeclarationParameters;
import com.yahoo.squidb.annotations.PrimaryKey;
import com.yahoo.squidb.processor.SqlUtils;
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
import com.yahoo.squidb.processor.plugins.defaults.properties.generators.RowidPropertyGenerator;
import com.yahoo.squidb.processor.writers.TableModelFileWriter;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;
import javax.tools.Diagnostic.Kind;

/**
 * This plugin controls generating property declarations, getters, and setters for fields in a table model. It can
 * create instances of {@link PropertyGenerator} for each of the basic supported column types (String, int, long, etc.)
 * <p>
 * Users who want to tweak the default field handling for table models can subclass this plugin and override the
 * protected methods for determining PropertyGenerator subclasses ({@link #getStringPropertyGenerator()},
 * {@link #getLongPropertyGenerator()}, etc.). Such a user plugin should be registered with "high" priority so it takes
 * precedence over the default version of this plugin.
 */
public class TableModelSpecFieldPlugin extends BaseFieldPlugin {

    public static final String DEFAULT_ID_PROPERTY_NAME = "ID";
    public static final String DEFAULT_ROWID_PROPERTY_NAME = "ROWID";
    private static final String METADATA_KEY_ROWID_ALIAS_PROPERTY_GENERATOR = "rowidAliasPropertyGenerator";
    private static final String METADATA_KEY_HAS_PRIMARY_KEY = "hasPrimaryKey";

    private Map<DeclaredTypeName, Class<? extends BasicPropertyGenerator>> generatorMap = new HashMap<>();

    public TableModelSpecFieldPlugin(ModelSpec<?> modelSpec, PluginEnvironment pluginEnv) {
        super(modelSpec, pluginEnv);
        registerBasicPropertyGenerators();
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

    @Override
    public boolean hasChangesForModelSpec() {
        return modelSpec instanceof TableModelSpecWrapper;
    }

    @Override
    protected boolean hasPropertyGeneratorForField(VariableElement field, DeclaredTypeName fieldType) {
        return !TypeConstants.isConstant(field) && generatorMap.containsKey(fieldType);
    }

    @Override
    public boolean processVariableElement(VariableElement field, DeclaredTypeName fieldType) {
        if (field.getAnnotation(PrimaryKey.class) != null) {
            return handlePrimaryKeyField(field, fieldType);
        } else {
            return super.processVariableElement(field, fieldType);
        }
    }

    private boolean handlePrimaryKeyField(VariableElement field, DeclaredTypeName fieldType) {
        if (modelSpec instanceof TableModelSpecWrapper
                && ((TableModelSpecWrapper) modelSpec).isVirtualTable()) {
            modelSpec.logError("Virtual tables cannot declare a custom primary key", field);
        } else if (modelSpec.hasMetadata(METADATA_KEY_HAS_PRIMARY_KEY)) {
            modelSpec.logError("Only a single field can be annotated as @PrimaryKey. If you want a multi-column"
                    + " primary key, specify it using SQL in TableModelSpec#tableConstraint() and set "
                    + "TableModelSpec#noRowIdAlias() to true in your TableModelSpec annotation.", field);
        } else {
            boolean result = false;
            if (TypeConstants.isIntegerType(fieldType)) {
                PropertyGenerator propertyGenerator = getPropertyGenerator(field, fieldType);
                if (propertyGenerator != null) {
                    modelSpec.putMetadata(METADATA_KEY_ROWID_ALIAS_PROPERTY_GENERATOR, propertyGenerator);
                    result = true;
                }
            } else {
                result = super.processVariableElement(field, fieldType);
            }
            if (result) {
                modelSpec.putMetadata(METADATA_KEY_HAS_PRIMARY_KEY, true);
            }
            return result;
        }
        return false;
    }

    @Override
    protected PropertyGenerator getPropertyGenerator(VariableElement field, DeclaredTypeName fieldType) {
        Class<? extends BasicPropertyGenerator> generatorClass;
        if (isIntegerPrimaryKey(field, fieldType)) {
            // Force INTEGER PRIMARY KEY properties to be LongProperty, even if declared as e.g. int
            generatorClass = getRowidPropertyGenerator();
        } else {
            generatorClass = generatorMap.get(fieldType);
        }
        try {
            BasicPropertyGenerator propertyGenerator = generatorClass.getConstructor(ModelSpec.class,
                    VariableElement.class, AptUtils.class).newInstance(modelSpec, field, utils);
            if (DEFAULT_ROWID_PROPERTY_NAME.equalsIgnoreCase(propertyGenerator.getColumnName()) ||
                    DEFAULT_ROWID_PROPERTY_NAME.equalsIgnoreCase(propertyGenerator.getPropertyName())) {
                modelSpec.logError("Columns in a table model spec cannot be named rowid, as "
                        + "they would clash with the SQLite rowid column used for SquiDB bookkeeping", field);
                return null;
            }

            String propertyName = propertyGenerator.getPropertyName();
            if (DEFAULT_ID_PROPERTY_NAME.equalsIgnoreCase(propertyName) && !isIntegerPrimaryKey(field, fieldType)) {
                modelSpec.logError("User-defined non-primary-key columns cannot currently be "
                        + "named 'ID' for the sake of backwards compatibility. This restriction will be removed in a "
                        + "future version of SquiDB.", field);
                return null;
            }

            String columnName = propertyGenerator.getColumnName();
            if (!SqlUtils.checkIdentifier(columnName, "column", modelSpec, field, utils)) {
                return null;
            }

            return propertyGenerator;
        } catch (Exception e) {
            modelSpec.logError("Exception instantiating PropertyGenerator: " + generatorClass + ", " + e, field);
        }
        return null;
    }

    private boolean isIntegerPrimaryKey(VariableElement field, DeclaredTypeName fieldType) {
        return field.getAnnotation(PrimaryKey.class) != null &&
                TypeConstants.isIntegerType(fieldType);
    }

    @Override
    public void afterProcessVariableElements() {
        RowidPropertyGenerator rowidPropertyGenerator;
        if (modelSpec.hasMetadata(METADATA_KEY_ROWID_ALIAS_PROPERTY_GENERATOR)) {
            rowidPropertyGenerator = modelSpec.getMetadata(METADATA_KEY_ROWID_ALIAS_PROPERTY_GENERATOR);
        } else {
            if (shouldGenerateROWIDProperty()) {
                rowidPropertyGenerator = new RowidPropertyGenerator(modelSpec, "rowid",
                        DEFAULT_ROWID_PROPERTY_NAME, utils);
            } else {
                utils.getMessager().printMessage(Kind.WARNING, "Model class " + modelSpec.getGeneratedClassName() +
                        " is currently generating an integer primary key ID property to act as an alias to the table's "
                        + "rowid. Future versions of SquiDB will remove this default property for the sake of better "
                        + "support for arbitrary primary keys. If you are using the ID property, you should update "
                        + "your model spec by explicitly declaring a field, named id with column name '_id' and "
                        + "annotated with @PrimaryKey", modelSpec.getModelSpecElement());
                rowidPropertyGenerator = new RowidPropertyGenerator(modelSpec, "_id",
                        DEFAULT_ID_PROPERTY_NAME, utils) {
                    @Override
                    protected String getColumnDefinition() {
                        return "\"PRIMARY KEY AUTOINCREMENT\"";
                    }
                };
            }
            modelSpec.putMetadata(METADATA_KEY_ROWID_ALIAS_PROPERTY_GENERATOR, rowidPropertyGenerator);
        }
        modelSpec.getPropertyGenerators().add(0, rowidPropertyGenerator);

        // Sanity check to make sure there is exactly 1 RowidPropertyGenerator
        RowidPropertyGenerator foundRowidPropertyGenerator = null;
        for (PropertyGenerator generator : modelSpec.getPropertyGenerators()) {
            if (generator instanceof RowidPropertyGenerator) {
                if (foundRowidPropertyGenerator != null) {
                    modelSpec.logError("Found redundant rowid property generator for property"
                            + generator.getPropertyName() + ". Rowid property generator " +
                            foundRowidPropertyGenerator.getPropertyName() + " already exists", generator.getField());
                } else {
                    foundRowidPropertyGenerator = (RowidPropertyGenerator) generator;
                }
            }
        }
    }

    private boolean shouldGenerateROWIDProperty() {
        TableModelSpecWrapper tableModelSpec = (TableModelSpecWrapper) modelSpec;
        return tableModelSpec.isVirtualTable() ||
                tableModelSpec.hasMetadata(METADATA_KEY_HAS_PRIMARY_KEY) ||
                tableModelSpec.getSpecAnnotation().noRowIdAlias();
    }

    @Override
    public void afterEmitPropertyDeclaration(JavaFileWriter writer, PropertyGenerator propertyGenerator)
            throws IOException {
        if (propertyGenerator instanceof RowidPropertyGenerator) {
            if (((TableModelSpecWrapper) modelSpec).isVirtualTable()) {
                writer.writeAnnotation(CoreTypes.DEPRECATED);
                writer.writeFieldDeclaration(TypeConstants.LONG_PROPERTY,
                        DEFAULT_ID_PROPERTY_NAME,
                        Expressions.fromString(DEFAULT_ROWID_PROPERTY_NAME),
                        TypeConstants.PUBLIC_STATIC_FINAL);
            }
            writeRowidSupportMethods(writer, propertyGenerator.getPropertyName());
        }
    }

    private void writeRowidSupportMethods(JavaFileWriter writer, String propertyName)
            throws IOException {
        // Write TABLE.setRowIdProperty call
        writer.beginInitializerBlock(true, true);
        writer.writeStatement(Expressions.callMethodOn(TableModelFileWriter.TABLE_NAME,
                "setRowIdProperty", propertyName));
        writer.finishInitializerBlock(true, true);
        writer.writeNewline();

        // Write getRowIdProperty() method
        writer.writeAnnotation(CoreTypes.OVERRIDE);
        MethodDeclarationParameters params = new MethodDeclarationParameters()
                .setModifiers(Modifier.PUBLIC)
                .setReturnType(TypeConstants.LONG_PROPERTY)
                .setMethodName("getRowIdProperty");
        writer.beginMethodDefinition(params);
        writer.writeStringStatement("return " + propertyName);
        writer.finishMethodDefinition();
    }

    @Override
    public void beforeEmitGetter(JavaFileWriter writer, PropertyGenerator propertyGenerator,
            MethodDeclarationParameters getterParams) throws IOException {
        addAccessorDocumentationForRowids(writer, propertyGenerator, true);
    }

    @Override
    public void beforeEmitSetter(JavaFileWriter writer, PropertyGenerator propertyGenerator,
            MethodDeclarationParameters setterParams) throws IOException {
        addAccessorDocumentationForRowids(writer, propertyGenerator, false);
    }

    private void addAccessorDocumentationForRowids(JavaFileWriter writer, PropertyGenerator propertyGenerator,
            boolean getter) throws IOException {
        if (propertyGenerator instanceof RowidPropertyGenerator) {
            if (DEFAULT_ROWID_PROPERTY_NAME.equals(propertyGenerator.getPropertyName())) {
                writer.writeAnnotation(CoreTypes.OVERRIDE);
            } else {
                writer.writeJavadoc(" This " + (getter ? "getter" : "setter") + " is an alias for " +
                        (getter ? "get" : "set") + "RowId(), as the underlying column is an INTEGER PRIMARY KEY");
            }
        }
    }

    @Override
    public void emitMethods(JavaFileWriter writer) throws IOException {
        // If rowid property generator hasn't already done it, need to generate
        // overridden setRowId with appropriate return type
        if (!pluginEnv.hasSquidbOption(PluginEnvironment.OPTIONS_DISABLE_DEFAULT_GETTERS_AND_SETTERS)) {
            RowidPropertyGenerator rowidPropertyGenerator = modelSpec
                    .getMetadata(METADATA_KEY_ROWID_ALIAS_PROPERTY_GENERATOR);
            if (rowidPropertyGenerator != null && !"setRowId".equals(rowidPropertyGenerator.setterMethodName())) {
                MethodDeclarationParameters params = new MethodDeclarationParameters()
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

    /**
     * @return the generator class this plugin should use for handling String fields
     */
    protected Class<? extends BasicStringPropertyGenerator> getStringPropertyGenerator() {
        return BasicStringPropertyGenerator.class;
    }

    /**
     * @return the generator class this plugin should use for handling long fields
     */
    protected Class<? extends BasicLongPropertyGenerator> getLongPropertyGenerator() {
        return BasicLongPropertyGenerator.class;
    }

    /**
     * @return the generator class this plugin should use for handling integer primary key fields
     */
    protected Class<? extends RowidPropertyGenerator> getRowidPropertyGenerator() {
        return RowidPropertyGenerator.class;
    }

    /**
     * @return the generator class this plugin should use for handling integer fields
     */
    protected Class<? extends BasicIntegerPropertyGenerator> getIntegerPropertyGenerator() {
        return BasicIntegerPropertyGenerator.class;
    }

    /**
     * @return the generator class this plugin should use for handling double or float fields
     */
    protected Class<? extends BasicDoublePropertyGenerator> getDoublePropertyGenerator() {
        return BasicDoublePropertyGenerator.class;
    }

    /**
     * @return the generator class this plugin should use for handling boolean fields
     */
    protected Class<? extends BasicBooleanPropertyGenerator> getBooleanPropertyGenerator() {
        return BasicBooleanPropertyGenerator.class;
    }

    /**
     * @return the generator class this plugin should use for handling blob fields
     */
    protected Class<? extends BasicBlobPropertyGenerator> getBlobPropertyGenerator() {
        return BasicBlobPropertyGenerator.class;
    }
}
