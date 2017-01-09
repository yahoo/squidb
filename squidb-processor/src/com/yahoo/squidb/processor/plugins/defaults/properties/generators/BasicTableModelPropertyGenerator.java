package com.yahoo.squidb.processor.plugins.defaults.properties.generators;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.yahoo.squidb.annotations.ColumnSpec;
import com.yahoo.squidb.annotations.PrimaryKey;
import com.yahoo.squidb.processor.StringUtils;
import com.yahoo.squidb.processor.TypeConstants;
import com.yahoo.squidb.processor.data.ModelSpec;
import com.yahoo.squidb.processor.plugins.PluginEnvironment;
import com.yahoo.squidb.processor.plugins.defaults.properties.generators.interfaces.TableModelPropertyGenerator;

import java.util.ArrayList;
import java.util.List;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;
import javax.tools.Diagnostic;

/**
 * Basic implementation of {@link TableModelPropertyGenerator} that builds off of {@link BasicPropertyGeneratorImpl}
 */
public abstract class BasicTableModelPropertyGenerator extends BasicPropertyGeneratorImpl
        implements TableModelPropertyGenerator {

    protected final ColumnSpec columnSpec;
    protected final String columnName;
    protected final String constraintString;

    public BasicTableModelPropertyGenerator(ModelSpec<?, ?> modelSpec, String columnName,
            PluginEnvironment pluginEnv) {
        this(modelSpec, columnName, columnName, pluginEnv);
    }

    public BasicTableModelPropertyGenerator(ModelSpec<?, ?> modelSpec, String columnName, String propertyName,
            PluginEnvironment pluginEnv) {
        super(modelSpec, null, propertyName, pluginEnv);

        this.columnSpec = null;
        this.columnName = columnName == null ? null : columnName.trim();
        this.constraintString = initConstraintString();

        validateColumnName();
    }

    public BasicTableModelPropertyGenerator(ModelSpec<?, ?> modelSpec, VariableElement field,
            PluginEnvironment pluginEnv) {
        super(modelSpec, field, field.getSimpleName().toString(), pluginEnv);

        this.columnSpec = field.getAnnotation(ColumnSpec.class);
        this.columnName = initColumnName(columnSpec);
        this.constraintString = initConstraintString();

        validateColumnName();
    }

    // TODO remove when SqlUtils reports an error for identifiers containing '$'
    private void validateColumnName() {
        if (columnName.indexOf('$') >= 0) {
            modelSpec.logError("Column names cannot contain the $ symbol", field);
        }
    }

    private String initColumnName(ColumnSpec columnDef) {
        if (columnDef != null && !StringUtils.isEmpty(columnDef.name().trim())) {
            return columnDef.name().trim();
        }
        return camelCasePropertyName;
    }

    private String initConstraintString() {
        StringBuilder toReturn = new StringBuilder();
        String constraints = columnSpec != null ? columnSpec.constraints() : ColumnSpec.DEFAULT_NONE;
        if (!ColumnSpec.DEFAULT_NONE.equals(constraints)) {
            toReturn.append(constraints);
        }

        if (!ColumnSpec.DEFAULT_NONE.equals(getDefaultValueFromColumnSpec())) {
            String columnDefaultValue = columnSpecDefaultValueToSql();

            if (!toReturn.toString().toUpperCase().contains("DEFAULT")) {
                toReturn.append(" DEFAULT ").append(columnDefaultValue);
            } else {
                pluginEnv.getMessager().printMessage(Diagnostic.Kind.WARNING,
                        "Duplicate default value definitions", field);
            }
        }

        if (field != null && field.getAnnotation(PrimaryKey.class) != null) {
            PrimaryKey primaryKeyAnnotation = field.getAnnotation(PrimaryKey.class);
            if (!toReturn.toString().toUpperCase().contains("PRIMARY KEY")) {
                toReturn.append(" PRIMARY KEY ");
                if (TypeConstants.isIntegerType(getTypeForAccessors()) && primaryKeyAnnotation.autoincrement()) {
                    toReturn.append("AUTOINCREMENT");
                }
            } else {
                pluginEnv.getMessager().printMessage(Diagnostic.Kind.WARNING, "Duplicate primary key definition in "
                        + "column constraints. Use the @PrimaryKey annotation instead of declaring the constraint in "
                        + "ColumnSpec.");
            }
        }

        String toReturnString = toReturn.toString().trim();
        if (!StringUtils.isEmpty(toReturnString)) {
            return toReturnString;
        }
        return null;
    }

    @Override
    public String getColumnName() {
        return columnName;
    }

    @Override
    public FieldSpec.Builder buildTablePropertyDeclaration(String tableModelArgName) {
        FieldSpec.Builder property = FieldSpec.builder(getPropertyType(), propertyName,
                Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL);
        if (isDeprecated) {
            property.addAnnotation(Deprecated.class);
        }

        String initializerFormat = "new $T($L, $S";

        List<Object> formatArgs = new ArrayList<>();
        formatArgs.add(getPropertyType());
        formatArgs.add(tableModelArgName);
        formatArgs.add(columnName);

        String constraintString = getConstraintString();
        if (!StringUtils.isEmpty(constraintString)) {
            initializerFormat += ", $S";
            formatArgs.add(constraintString);
        }
        initializerFormat += ")";
        property.initializer(initializerFormat, formatArgs.toArray(new Object[formatArgs.size()]));
        return property;
    }

    @Override
    public String getConstraintString() {
        return constraintString;
    }

    private String getDefaultValueFromColumnSpec() {
        return columnSpec != null ? columnSpec.defaultValue() : ColumnSpec.DEFAULT_NONE;
    }

    protected String columnSpecDefaultValueToSql() {
        String columnDefault = getDefaultValueFromColumnSpec();
        return ColumnSpec.DEFAULT_NULL.equals(columnDefault) ? "NULL" : columnDefault;
    }

    @Override
    public CodeBlock buildPutDefault(String contentValuesName) {
        String defaultValue = getDefaultValueFromColumnSpec();
        if (ColumnSpec.DEFAULT_NONE.equals(defaultValue)) {
            return null;
        }

        if (ColumnSpec.DEFAULT_NULL.equals(defaultValue)) {
            return CodeBlock.of("$L.putNull($L.getName())", contentValuesName, propertyName);
        } else {
            return CodeBlock.of("$L.put($L.getName(), $L)", contentValuesName, propertyName, getDefaultValueForContentValues());
        }
    }

    protected String getDefaultValueForContentValues() {
        return getDefaultValueFromColumnSpec();
    }
}
