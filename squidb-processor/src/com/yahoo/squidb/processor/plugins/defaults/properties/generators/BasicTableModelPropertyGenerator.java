/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.plugins.defaults.properties.generators;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.TypeName;
import com.yahoo.squidb.annotations.ColumnName;
import com.yahoo.squidb.annotations.ColumnSpec;
import com.yahoo.squidb.annotations.PrimaryKey;
import com.yahoo.squidb.annotations.defaults.DefaultBool;
import com.yahoo.squidb.annotations.defaults.DefaultDouble;
import com.yahoo.squidb.annotations.defaults.DefaultExpression;
import com.yahoo.squidb.annotations.defaults.DefaultInt;
import com.yahoo.squidb.annotations.defaults.DefaultLong;
import com.yahoo.squidb.annotations.defaults.DefaultNull;
import com.yahoo.squidb.annotations.defaults.DefaultString;
import com.yahoo.squidb.processor.StringUtils;
import com.yahoo.squidb.processor.TypeConstants;
import com.yahoo.squidb.processor.data.ModelSpec;
import com.yahoo.squidb.processor.plugins.PluginEnvironment;
import com.yahoo.squidb.processor.plugins.defaults.properties.generators.interfaces.TableModelPropertyGenerator;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    private static final Set<Class<? extends Annotation>> DEFAULT_VALUE_ANNOTATIONS = new HashSet<>(Arrays.asList(
            DefaultBool.class,
            DefaultDouble.class,
            DefaultInt.class,
            DefaultLong.class,
            DefaultString.class,
            DefaultNull.class,
            DefaultExpression.class
    ));

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

        doValidation();
    }

    public BasicTableModelPropertyGenerator(ModelSpec<?, ?> modelSpec, VariableElement field,
            PluginEnvironment pluginEnv) {
        super(modelSpec, field, field.getSimpleName().toString(), pluginEnv);

        this.columnSpec = field.getAnnotation(ColumnSpec.class);
        this.columnName = initColumnName(field);
        this.constraintString = initConstraintString();

        doValidation();
    }

    private void doValidation() {
        validateColumnName();
        validateDefaultAnnotations();
    }

    // TODO remove when SqlUtils reports an error for identifiers containing '$'
    private void validateColumnName() {
        if (columnName.indexOf('$') >= 0) {
            modelSpec.logError("Column names cannot contain the $ symbol", field);
        }
    }

    private void validateDefaultAnnotations() {
        // Validate that no more than one default annotation exists, and if one does that it is of the correct type
        if (field != null) {
            boolean foundAnnotation = false;
            Set<Class<? extends Annotation>> validAnnotations = new HashSet<>();
            validAnnotations.add(getDefaultAnnotationType());
            validAnnotations.add(DefaultNull.class);
            validAnnotations.add(DefaultExpression.class);
            for (Class<? extends Annotation> annotationClass : DEFAULT_VALUE_ANNOTATIONS) {
                if (field.getAnnotation(annotationClass) != null) {
                    if (!validAnnotations.contains(annotationClass)) {
                        modelSpec.logError("Default value annotation type mismatch -- found " +
                                annotationClass.getSimpleName() + "but only one of " + validAnnotations +
                                " is allowed", field);
                        return;
                    } else if (foundAnnotation) {
                        modelSpec.logError("Only one default value annotation per field is allowed", field);
                        return;
                    } else {
                        foundAnnotation = true;
                    }
                }
            }
            if (field.getAnnotation(DefaultExpression.class) != null) {
                String defaultExpression = field.getAnnotation(DefaultExpression.class).value().trim();
                if (isDefaultExpressionSpecialTimeCase(defaultExpression) &&
                        !TypeName.get(String.class).equals(getTypeForAccessors())) {
                    modelSpec.logError("Special default expression " + defaultExpression + " can only apply to "
                            + "string columns", field);
                }
            }
        }
    }

    private String initColumnName(VariableElement field) {
        ColumnName columnName = field.getAnnotation(ColumnName.class);
        if (columnName != null && !StringUtils.isEmpty(columnName.value().trim())) {
            return columnName.value().trim();
        }
        return camelCasePropertyName;
    }

    private String initConstraintString() {
        StringBuilder toReturn = new StringBuilder();
        String constraints = columnSpec != null ? columnSpec.constraints() : "";
        if (!StringUtils.isEmpty(constraints)) {
            toReturn.append(constraints);
        }

        handleDefaultValueAnnotations(toReturn);

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

    private void handleDefaultValueAnnotations(StringBuilder constraintString) {
        if (field != null) {
            String defaultValueAsSql;
            if (field.getAnnotation(DefaultExpression.class) != null) {
                defaultValueAsSql = getDefaultExpressionValue();
            } else if (field.getAnnotation(DefaultNull.class) != null) {
                defaultValueAsSql = "NULL";
            } else {
                defaultValueAsSql = getPrimitiveDefaultValueAsSql();
            }

            if (defaultValueAsSql != null) {
                if (!constraintString.toString().toUpperCase().contains("DEFAULT")) {
                    constraintString.append(" DEFAULT ").append(defaultValueAsSql);
                } else {
                    pluginEnv.getMessager().printMessage(Diagnostic.Kind.WARNING,
                            "Column has more than one default value definition.", field);
                }
            }
        }
    }

    private String getDefaultExpressionValue() {
        String defaultExpression = field.getAnnotation(DefaultExpression.class).value().trim();
        if (isDefaultExpressionSpecialTimeCase(defaultExpression)) {
            return defaultExpression;
        } else {
            if (!defaultExpression.startsWith("(") || !defaultExpression.endsWith(")")) {
                defaultExpression = "(" + defaultExpression + ")";
            }
            return defaultExpression;
        }
    }

    private boolean isDefaultExpressionSpecialTimeCase(String defaultExpression) {
        return DefaultExpression.CURRENT_TIME.equalsIgnoreCase(defaultExpression) ||
                DefaultExpression.CURRENT_DATE.equalsIgnoreCase(defaultExpression) ||
                DefaultExpression.CURRENT_TIMESTAMP.equalsIgnoreCase(defaultExpression);
    }

    @Override
    public CodeBlock buildPutDefault(String contentValuesName) {
        Object primitiveDefaultValue = null;
        boolean explicitNullDefault = false;
        if (field != null) {
            explicitNullDefault = field.getAnnotation(DefaultNull.class) != null;
            primitiveDefaultValue = getPrimitiveDefaultValueFromAnnotation();
        }
        if (primitiveDefaultValue == null && !explicitNullDefault) {
            return null;
        }

        if (explicitNullDefault) {
            return CodeBlock.of("$L.putNull($L.getName())", contentValuesName, propertyName);
        } else {
            String formatSpecifier = "$L";
            if (primitiveDefaultValue instanceof String) {
                formatSpecifier = "$S";
            } else if (primitiveDefaultValue instanceof Long) {
                formatSpecifier = "$LL";
            }
            return CodeBlock.of("$L.put($L.getName(), " + formatSpecifier + ")", contentValuesName, propertyName, primitiveDefaultValue);
        }
    }

    protected abstract Class<? extends Annotation> getDefaultAnnotationType();

    protected abstract Object getPrimitiveDefaultValueFromAnnotation();

    protected String getPrimitiveDefaultValueAsSql() {
        Object primitiveDefaultValue = getPrimitiveDefaultValueFromAnnotation();
        return primitiveDefaultValue != null ? primitiveDefaultValue.toString() : null;
    }
}
