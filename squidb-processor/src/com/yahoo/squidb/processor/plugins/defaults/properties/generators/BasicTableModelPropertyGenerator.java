package com.yahoo.squidb.processor.plugins.defaults.properties.generators;

import com.yahoo.aptutils.model.CoreTypes;
import com.yahoo.aptutils.utils.AptUtils;
import com.yahoo.aptutils.writer.JavaFileWriter;
import com.yahoo.aptutils.writer.expressions.Expressions;
import com.yahoo.squidb.annotations.ColumnSpec;
import com.yahoo.squidb.annotations.PrimaryKey;
import com.yahoo.squidb.processor.TypeConstants;
import com.yahoo.squidb.processor.data.ModelSpec;
import com.yahoo.squidb.processor.plugins.defaults.properties.generators.interfaces.TableModelPropertyGenerator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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

    public BasicTableModelPropertyGenerator(ModelSpec<?, ?> modelSpec, String columnName, AptUtils utils) {
        this(modelSpec, columnName, columnName, utils);
    }

    public BasicTableModelPropertyGenerator(ModelSpec<?, ?> modelSpec, String columnName, String propertyName,
            AptUtils utils) {
        super(modelSpec, null, propertyName, utils);

        this.columnSpec = null;
        this.columnName = columnName == null ? null : columnName.trim();
        this.constraintString = initConstraintString();

        validateColumnName();
    }

    public BasicTableModelPropertyGenerator(ModelSpec<?, ?> modelSpec, VariableElement field, AptUtils utils) {
        super(modelSpec, field, field.getSimpleName().toString(), utils);

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
        if (columnDef != null && !AptUtils.isEmpty(columnDef.name().trim())) {
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
                utils.getMessager().printMessage(Diagnostic.Kind.WARNING, "Duplicate default value definitions", field);
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
                utils.getMessager().printMessage(Diagnostic.Kind.WARNING, "Duplicate primary key definition in column constraints."
                        + " Use the @PrimaryKey annotation instead of declaring the constraint in ColumnSpec.");
            }
        }

        String toReturnString = toReturn.toString().trim();
        if (!AptUtils.isEmpty(toReturnString)) {
            return "\"" + toReturnString + "\"";
        }
        return null;
    }

    @Override
    public String getColumnName() {
        return columnName;
    }

    @Override
    public void emitTablePropertyDeclaration(JavaFileWriter writer, String tableModelArgName) throws IOException {
        if (isDeprecated) {
            writer.writeAnnotation(CoreTypes.DEPRECATED);
        }
        List<Object> constructorArgs = new ArrayList<>();
        constructorArgs.add(tableModelArgName);
        constructorArgs.add("\"" + columnName + "\"");
        String constraintString = getConstraintString();
        if (!AptUtils.isEmpty(constraintString)) {
            constructorArgs.add(constraintString);
        }

        writer.writeFieldDeclaration(getPropertyType(), propertyName,
                Expressions.callConstructor(getPropertyType(), constructorArgs), TypeConstants.PUBLIC_STATIC_FINAL);
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
    public void emitPutDefault(JavaFileWriter writer, String contentValuesName) throws IOException {
        String defaultValue = getDefaultValueFromColumnSpec();
        if (ColumnSpec.DEFAULT_NONE.equals(defaultValue)) {
            return;
        }

        String methodToInvoke;
        List<Object> arguments = new ArrayList<>();
        arguments.add(Expressions.callMethodOn(propertyName, "getName"));
        if (ColumnSpec.DEFAULT_NULL.equals(defaultValue)) {
            methodToInvoke = "putNull";
        } else {
            methodToInvoke = "put";
            arguments.add(getDefaultValueForContentValues());
        }

        writer.writeStatement(Expressions.callMethodOn(contentValuesName, methodToInvoke, arguments));
    }

    protected String getDefaultValueForContentValues() {
        return getDefaultValueFromColumnSpec();
    }
}
