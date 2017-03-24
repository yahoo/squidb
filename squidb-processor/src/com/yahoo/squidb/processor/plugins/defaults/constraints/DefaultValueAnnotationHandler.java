/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.plugins.defaults.constraints;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;
import com.yahoo.squidb.annotations.tables.defaults.DefaultBlob;
import com.yahoo.squidb.annotations.tables.defaults.DefaultBoolean;
import com.yahoo.squidb.annotations.tables.defaults.DefaultDouble;
import com.yahoo.squidb.annotations.tables.defaults.DefaultExpression;
import com.yahoo.squidb.annotations.tables.defaults.DefaultInt;
import com.yahoo.squidb.annotations.tables.defaults.DefaultLong;
import com.yahoo.squidb.annotations.tables.defaults.DefaultNull;
import com.yahoo.squidb.annotations.tables.defaults.DefaultString;
import com.yahoo.squidb.processor.data.ModelSpec;
import com.yahoo.squidb.processor.plugins.PluginEnvironment;
import com.yahoo.squidb.processor.plugins.defaults.properties.generators.interfaces.PropertyGenerator;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.lang.model.element.VariableElement;
import javax.tools.Diagnostic;

/**
 * Abstract base class for handling default value annotations
 */
public abstract class DefaultValueAnnotationHandler<ANNOTATION extends Annotation, VALUE>
        implements ColumnConstraintAnnotationHandler<ANNOTATION> {

    private static final Set<Class<? extends Annotation>> DEFAULT_VALUE_ANNOTATIONS = new HashSet<>(Arrays.asList(
            DefaultBlob.class,
            DefaultBoolean.class,
            DefaultDouble.class,
            DefaultInt.class,
            DefaultLong.class,
            DefaultString.class,
            DefaultNull.class,
            DefaultExpression.class
    ));

    @Override
    public void appendConstraintForColumn(StringBuilder constraintBuilder, PropertyGenerator propertyGenerator,
            PluginEnvironment pluginEnv) {
        VariableElement field = propertyGenerator.getField();
        if (field != null) {
            String defaultValueAsSql = null;
            if (field.getAnnotation(DefaultExpression.class) != null) {
                defaultValueAsSql = getDefaultExpressionValue(field.getAnnotation(DefaultExpression.class));
            } else if (field.getAnnotation(DefaultNull.class) != null) {
                defaultValueAsSql = "NULL";
            } else if (field.getAnnotation(getAnnotationClass()) != null) {
                defaultValueAsSql = getPrimitiveDefaultValueAsSql(field.getAnnotation(getAnnotationClass()));
            }

            if (defaultValueAsSql != null) {
                if (!constraintBuilder.toString().toUpperCase().contains("DEFAULT")) {
                    constraintBuilder.append(" DEFAULT ").append(defaultValueAsSql);
                } else {
                    pluginEnv.getMessager().printMessage(Diagnostic.Kind.WARNING,
                            "Column has more than one default value definition.", field);
                }
            }
        }
    }

    @Override
    public void validateAnnotationForColumn(PropertyGenerator propertyGenerator, ModelSpec<?, ?> modelSpec,
            PluginEnvironment pluginEnvironment) {
        // Validate that no more than one default annotation exists, and if one does that it is of the correct type
        VariableElement field = propertyGenerator.getField();
        if (field != null) {
            boolean foundAnnotation = false;
            Set<Class<? extends Annotation>> validAnnotations = new HashSet<>();
            validAnnotations.add(getAnnotationClass());
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
                        !TypeName.get(String.class).equals(propertyGenerator.getTypeForAccessors())) {
                    modelSpec.logError("Special default expression " + defaultExpression + " can only apply to "
                            + "string columns", field);
                }
            }
        }
    }

    public final CodeBlock getPutPrimitiveDefaultExpression(PropertyGenerator propertyGenerator,
            String contentValuesName) {
        VALUE primitiveDefaultValue = null;
        boolean explicitNullDefault = false;
        VariableElement field = propertyGenerator.getField();
        if (field != null) {
            explicitNullDefault = propertyGenerator.getField().getAnnotation(DefaultNull.class) != null;
            if (field.getAnnotation(getAnnotationClass()) != null) {
                primitiveDefaultValue = getPrimitiveDefaultValueFromAnnotation(field.getAnnotation(getAnnotationClass()));
            }
        }
        if (primitiveDefaultValue == null && !explicitNullDefault) {
            return null;
        }

        if (explicitNullDefault) {
            return CodeBlock.of("$L.putNull($L.getName())", contentValuesName, propertyGenerator.getPropertyName());
        } else {
            String formatSpecifier = "$L";
            if (primitiveDefaultValue instanceof String) {
                formatSpecifier = "$S";
            } else if (primitiveDefaultValue instanceof Long) {
                formatSpecifier = "$LL";
            }
            return CodeBlock.of("$L.put($L.getName(), " + formatSpecifier + ")", contentValuesName,
                    propertyGenerator.getPropertyName(), primitiveDefaultValue);
        }
    }

    private String getDefaultExpressionValue(DefaultExpression defaultExpressionAnnotation) {
        String defaultExpression = defaultExpressionAnnotation.value().trim();
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

    protected abstract VALUE getPrimitiveDefaultValueFromAnnotation(ANNOTATION annotation);

    protected String getPrimitiveDefaultValueAsSql(ANNOTATION annotation) {
        VALUE primitiveDefaultValue = getPrimitiveDefaultValueFromAnnotation(annotation);
        return primitiveDefaultValue != null ? primitiveDefaultValue.toString() : null;
    }
}
