/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.plugins.defaults.properties.generators;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.yahoo.squidb.annotations.tables.ColumnName;
import com.yahoo.squidb.annotations.tables.defaults.DefaultNull;
import com.yahoo.squidb.processor.StringUtils;
import com.yahoo.squidb.processor.data.ModelSpec;
import com.yahoo.squidb.processor.plugins.PluginEnvironment;
import com.yahoo.squidb.processor.plugins.defaults.constraints.CheckAnnotationHandler;
import com.yahoo.squidb.processor.plugins.defaults.constraints.CollateAnnotationHandler;
import com.yahoo.squidb.processor.plugins.defaults.constraints.ColumnConstraintAnnotationHandler;
import com.yahoo.squidb.processor.plugins.defaults.constraints.ConstraintSqlAnnotationHandler;
import com.yahoo.squidb.processor.plugins.defaults.constraints.DefaultValueAnnotationHandler;
import com.yahoo.squidb.processor.plugins.defaults.constraints.JavaxNonnullAnnotationHandler;
import com.yahoo.squidb.processor.plugins.defaults.constraints.NotNullAnnotationHandler;
import com.yahoo.squidb.processor.plugins.defaults.constraints.PrimaryKeyAnnotationHandler;
import com.yahoo.squidb.processor.plugins.defaults.constraints.UniqueAnnotationHandler;
import com.yahoo.squidb.processor.plugins.defaults.properties.generators.interfaces.TableModelPropertyGenerator;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;

/**
 * Basic implementation of {@link TableModelPropertyGenerator} that builds off of {@link BasicPropertyGeneratorImpl}
 */
public abstract class BasicTableModelPropertyGenerator extends BasicPropertyGeneratorImpl
        implements TableModelPropertyGenerator {

    protected final String columnName;
    protected final String constraintString;
    protected final Set<ColumnConstraintAnnotationHandler<?>> annotationHandlers;

    public BasicTableModelPropertyGenerator(ModelSpec<?, ?> modelSpec, String columnName,
            PluginEnvironment pluginEnv) {
        this(modelSpec, columnName, columnName, pluginEnv);
    }

    public BasicTableModelPropertyGenerator(ModelSpec<?, ?> modelSpec, String columnName, String propertyName,
            PluginEnvironment pluginEnv) {
        super(modelSpec, null, propertyName, pluginEnv);

        this.annotationHandlers = initConstraintHandlers();
        this.columnName = columnName == null ? null : columnName.trim();
        this.constraintString = initConstraintString();

        doValidation();
    }

    public BasicTableModelPropertyGenerator(ModelSpec<?, ?> modelSpec, VariableElement field,
            PluginEnvironment pluginEnv) {
        super(modelSpec, field, field.getSimpleName().toString(), pluginEnv);

        this.annotationHandlers = initConstraintHandlers();
        this.columnName = initColumnName(field);
        this.constraintString = initConstraintString();

        doValidation();
    }

    protected Set<ColumnConstraintAnnotationHandler<?>> initConstraintHandlers() {
        Set<ColumnConstraintAnnotationHandler<?>> handlers = new LinkedHashSet<>();
        handlers.add(new ConstraintSqlAnnotationHandler.ColumnConstraintSqlAnnotationHandler());
        handlers.add(new PrimaryKeyAnnotationHandler());
        handlers.add(new UniqueAnnotationHandler());
        handlers.add(new NotNullAnnotationHandler());
        handlers.add(new JavaxNonnullAnnotationHandler());
        handlers.add(new CollateAnnotationHandler());
        handlers.add(new CheckAnnotationHandler.ColumnCheckAnnotationHandler());
        handlers.add(getDefaultValueAnnotationHandler());
        return handlers;
    }

    private String initColumnName(VariableElement field) {
        ColumnName columnName = field.getAnnotation(ColumnName.class);
        if (columnName != null && !StringUtils.isEmpty(columnName.value().trim())) {
            return columnName.value().trim();
        }
        return camelCasePropertyName;
    }

    private void doValidation() {
        validateColumnName();
        for (ColumnConstraintAnnotationHandler<?> handler : annotationHandlers) {
            handler.validateAnnotationForColumn(this, modelSpec, pluginEnv);
        }
        validateNullability();
    }

    // TODO remove when SqlUtils reports an error for identifiers containing '$'
    private void validateColumnName() {
        if (columnName.indexOf('$') >= 0) {
            modelSpec.logError("Column names cannot contain the $ symbol", field);
        }
    }

    private void validateNullability() {
        if (field != null && field.getAnnotation(DefaultNull.class) != null &&
                Nonnull.class.equals(getAccessorNullabilityAnnotation())) {
            modelSpec.logError("Field cannot be annotated with @DefaultNull and have @Nonnull as its accessor "
                    + "nullability specifier", field);
        }
    }

    private String initConstraintString() {
        StringBuilder constraintBuilder = new StringBuilder();
        if (field != null) {
            for (ColumnConstraintAnnotationHandler<?> handler : annotationHandlers) {
                handler.appendConstraintForColumn(constraintBuilder, this, pluginEnv);
            }
        }
        String constraintString = constraintBuilder.toString().trim();
        if (!StringUtils.isEmpty(constraintString)) {
            return constraintString;
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

    @Override
    public CodeBlock buildPutDefault(String contentValuesName) {
        return getDefaultValueAnnotationHandler().getPutPrimitiveDefaultExpression(this, contentValuesName);
    }

    protected abstract DefaultValueAnnotationHandler<?, ?> getDefaultValueAnnotationHandler();
}
