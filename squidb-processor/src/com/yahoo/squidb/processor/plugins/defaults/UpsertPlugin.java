/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.plugins.defaults;

import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.yahoo.squidb.annotations.tables.UpsertConfig;
import com.yahoo.squidb.annotations.tables.UpsertKey;
import com.yahoo.squidb.processor.SqlUtils;
import com.yahoo.squidb.processor.StringUtils;
import com.yahoo.squidb.processor.TypeConstants;
import com.yahoo.squidb.processor.data.ModelSpec;
import com.yahoo.squidb.processor.data.TableModelSpecWrapper;
import com.yahoo.squidb.processor.plugins.AbstractPlugin;
import com.yahoo.squidb.processor.plugins.Plugin;
import com.yahoo.squidb.processor.plugins.PluginEnvironment;
import com.yahoo.squidb.processor.plugins.defaults.properties.generators.interfaces.PropertyGenerator;
import com.yahoo.squidb.processor.plugins.defaults.properties.generators.interfaces.TableModelPropertyGenerator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;
import javax.tools.Diagnostic;

/**
 * A {@link Plugin} that processes the {@link UpsertKey} annotation and generates code to make any table model specs
 * containing UpsertKey fields implement the Upsertable interface. See that interface for more information. This plugin
 * can be disabled by passing the {@link PluginEnvironment#OPTIONS_DISABLE_DEFAULT_UPSERT} option in the squidbOptions
 * key.
 */
public class UpsertPlugin extends AbstractPlugin {

    private static final ClassName UPSERTABLE = ClassName.get(TypeConstants.SQUIDB_DATA_PACKAGE, "Upsertable");
    private static final ClassName CRITERION = ClassName.get(TypeConstants.SQUIDB_SQL_PACKAGE, "Criterion");
    private static final TypeName CRITERION_ARRAY = ArrayTypeName.of(CRITERION);

    private static final String LOGICAL_KEY_COL_ARRAY_NAME = "logicalKeyColumns";

    private static final Pattern NOT_NULL = Pattern.compile("(^|\\s+)NOT\\s+NULL($|\\s+)", Pattern.CASE_INSENSITIVE);

    private static final Pattern UNIQUE_SINGLE_COLUMN =
            Pattern.compile("(^|\\s+)UNIQUE($|\\s+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern PRIMARY_KEY_SINGLE_COLUMN =
            Pattern.compile("(^|\\s+)PRIMARY\\s+KEY($|\\s+)", Pattern.CASE_INSENSITIVE);

    private static final String INDEXED_COLUMN_REGEX =
            "(" + SqlUtils.IDENTIFIER_REGEX + ")(\\s+COLLATE\\s+\\p{Alpha}+)?(\\s+(ASC|DESC))?";
    private static final String INDEXED_COLUMN_LIST_REGEX =
            "(" + INDEXED_COLUMN_REGEX + "\\s*,\\s*)*" + INDEXED_COLUMN_REGEX;

    private static final Pattern SINGLE_INDEXED_COLUMN =
            Pattern.compile(INDEXED_COLUMN_REGEX, Pattern.CASE_INSENSITIVE);
    private static final Pattern UNIQUE_MULTI_COLUMN =
            Pattern.compile("UNIQUE\\s*\\(\\s*(" + INDEXED_COLUMN_LIST_REGEX + ")\\s*\\)",
                    Pattern.CASE_INSENSITIVE);
    private static final Pattern PRIMARY_KEY_MULTI_COLUMN =
            Pattern.compile("PRIMARY\\s+KEY\\s*\\(\\s*(" + INDEXED_COLUMN_LIST_REGEX + ")\\s*\\)",
                    Pattern.CASE_INSENSITIVE);

    private List<TableModelPropertyGenerator> upsertColumns = new ArrayList<>();
    private TableModelSpecWrapper tableModelSpec;

    @Override
    public boolean init(ModelSpec<?, ?> modelSpec, PluginEnvironment pluginEnv) {
        super.init(modelSpec, pluginEnv);
        if (!(modelSpec instanceof TableModelSpecWrapper)) {
            return false;
        }
        this.tableModelSpec = (TableModelSpecWrapper) modelSpec;
        return true;
    }

    @Override
    public void afterProcessVariableElements() {
        for (TableModelPropertyGenerator generator : tableModelSpec.getPropertyGenerators()) {
            VariableElement field = generator.getField();
            if (field != null && field.getAnnotation(UpsertKey.class) != null) {
                upsertColumns.add(generator);
            }
        }
        if (!upsertColumns.isEmpty()) {
            validateUpsertKeyColumns();
        }
    }

    private void validateUpsertKeyColumns() {
        checkUpsertColumnsForNotNullConstraints();
        checkForUniquenessConstraints();
    }

    private void checkUpsertColumnsForNotNullConstraints() {
        for (TableModelPropertyGenerator generator : upsertColumns) {
            String columnDefinition = generator.getConstraintString();
            if (columnDefinition == null || !NOT_NULL.matcher(columnDefinition).find()) {
                modelSpec.logError("No NOT NULL constraint found on UpsertKey column. Make sure this constraint "
                        + "is specified using the @ColumnSpec annotation", generator.getField());
            }
        }
    }

    private void checkForUniquenessConstraints() {
        if (upsertColumns.size() == 1) {
            TableModelPropertyGenerator column = upsertColumns.get(0);
            String columnConstraints = column.getConstraintString();
            if (!StringUtils.isEmpty(columnConstraints) &&
                    (UNIQUE_SINGLE_COLUMN.matcher(columnConstraints).find() || PRIMARY_KEY_SINGLE_COLUMN.matcher(columnConstraints).find())) {
                // Found the required uniqueness constraint in column definition, no additional checks necessary
                return;
            }
        }
        Set<String> columnNames = new HashSet<>();
        for (TableModelPropertyGenerator propertyGenerator : upsertColumns) {
            columnNames.add(propertyGenerator.getColumnName());
        }

        String tableConstraints = tableModelSpec.getTableConstraintString();
        if (!checkForUniquenessConstraints(columnNames, tableConstraints, UNIQUE_MULTI_COLUMN) &&
                !checkForUniquenessConstraints(columnNames, tableConstraints, PRIMARY_KEY_MULTI_COLUMN)) {
            modelSpec.logError("No uniqueness constraint found containing all upsert key columns",
                    tableModelSpec.getModelSpecElement());
        }
    }

    private boolean checkForUniquenessConstraints(Set<String> columnNames, String tableConstraints, Pattern regex) {
        if (tableConstraints == null) {
            return false;
        }
        Matcher matcher = regex.matcher(tableConstraints);
        while (matcher.find()) {
            Set<String> namesInExpressions = extractNamesFromExpressions(matcher.group(1));
            if (columnNames.equals(namesInExpressions)) {
                return true;
            }
        }
        return false;
    }

    private Set<String> extractNamesFromExpressions(String indexedColumnList) {
        String[] columnExpressions = indexedColumnList.split(",");
        Set<String> result = new HashSet<>();
        for (String expr : columnExpressions) {
            expr = expr.trim();
            if (StringUtils.isEmpty(expr)) {
                continue;
            }

            Matcher matcher = SINGLE_INDEXED_COLUMN.matcher(expr);
            if (matcher.find()) {
                String columnName = matcher.group(1);
                result.add(columnName);
            } else {
                // This should never happen, but if it does, we should at least log a warning
                pluginEnv.getMessager().printMessage(Diagnostic.Kind.WARNING, "Unable to extract column name from "
                        + "indexed column " + expr + " in indexed column list " + indexedColumnList,
                        modelSpec.getModelSpecElement());
                return null;
            }
        }
        return result;
    }

    @Override
    public void addInterfacesToImplement(Set<TypeName> interfaces) {
        if (!upsertColumns.isEmpty()) {
            interfaces.add(UPSERTABLE);
        }
    }

    @Override
    public void afterDeclareSchema(TypeSpec.Builder builder) {
        if (!upsertColumns.isEmpty()) {
            FieldSpec.Builder upsertColumnsField = FieldSpec.builder(TypeConstants.PROPERTY_ARRAY,
                    LOGICAL_KEY_COL_ARRAY_NAME, Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL);

            CodeBlock.Builder initializer = CodeBlock.builder()
                    .beginControlFlow("new $T[]", TypeConstants.PROPERTY);
            for (PropertyGenerator generator : upsertColumns) {
                initializer.add("$L,\n", generator.getPropertyName());
            }
            initializer.endControlFlow();
            upsertColumnsField.initializer(initializer.build());
            builder.addField(upsertColumnsField.build());
        }
    }

    private void declareRowidHasPriorityMethod(TypeSpec.Builder builder) {
        UpsertConfig upsertConfig = modelSpec.getModelSpecElement().getAnnotation(UpsertConfig.class);
        boolean rowidHasPriority = upsertConfig == null || upsertConfig.rowidHasPriority();

        MethodSpec.Builder params = MethodSpec.methodBuilder("rowidSupersedesLogicalKey")
                .addModifiers(Modifier.PUBLIC)
                .returns(TypeName.BOOLEAN)
                .addAnnotation(Override.class)
                .addStatement("return $L", rowidHasPriority);
        builder.addMethod(params.build());
    }

    @Override
    public void declareMethodsOrConstructors(TypeSpec.Builder builder) {
        if (!upsertColumns.isEmpty()) {
            declareRowidHasPriorityMethod(builder);
            declareGetUpsertKeyLookupCriterion(builder);
        }
    }

    private void declareGetUpsertKeyLookupCriterion(TypeSpec.Builder builder) {
        MethodSpec.Builder params = MethodSpec.methodBuilder("getLogicalKeyLookupCriterion")
                .addModifiers(Modifier.PUBLIC)
                .returns(CRITERION)
                .addAnnotation(Override.class);
        params.addCode(buildGetUpsertKeyLookupCriterionBody());

        builder.addMethod(params.build());
    }

    private CodeBlock buildGetUpsertKeyLookupCriterionBody() {
        UpsertConfig upsertConfig = modelSpec.getModelSpecElement().getAnnotation(UpsertConfig.class);
        boolean failureThrowsException = upsertConfig == null || upsertConfig.missingLookupValueThrows();

        CodeBlock.Builder methodBody = CodeBlock.builder();
        methodBody.beginControlFlow("for (int i = 0; i < $L; i++)", upsertColumns.size())
                .addStatement("$T col = $L[i]", TypeConstants.PROPERTY, LOGICAL_KEY_COL_ARRAY_NAME)
                .beginControlFlow("if (!containsNonNullValue(col))");
        if (failureThrowsException) {
            methodBody.addStatement("throw new $T($S + $L[i].getName() + $S + getClass())",
                    IllegalStateException.class, "Value for upsert key column ",  LOGICAL_KEY_COL_ARRAY_NAME,
                    " was missing when trying to upsert item of class ");
        } else {
            methodBody.addStatement("return null");
        }
        methodBody.endControlFlow();
        methodBody.endControlFlow();
        methodBody.addStatement("$T result = $L[0].eq(get($L[0]))", CRITERION,
                LOGICAL_KEY_COL_ARRAY_NAME, LOGICAL_KEY_COL_ARRAY_NAME);

        if (upsertColumns.size() > 1) {
            methodBody.addStatement("$T extraCriterion = new $T[$L]",
                    CRITERION_ARRAY, CRITERION, upsertColumns.size() - 1)
                    .beginControlFlow("for (int i = 1; i < $L; i++)", upsertColumns.size())
                    .addStatement("extraCriterion[i - 1] = $L[i].eq(get($L[i]))",
                            LOGICAL_KEY_COL_ARRAY_NAME, LOGICAL_KEY_COL_ARRAY_NAME)
                    .endControlFlow()
                    .addStatement("result = $T.and(result, extraCriterion)", CRITERION);
        }
        methodBody.addStatement("return result");
        return methodBody.build();
    }
}
