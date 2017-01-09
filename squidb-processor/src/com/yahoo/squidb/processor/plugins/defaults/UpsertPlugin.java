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
import com.yahoo.squidb.annotations.UpsertConfig;
import com.yahoo.squidb.annotations.UpsertKey;
import com.yahoo.squidb.processor.TypeConstants;
import com.yahoo.squidb.processor.data.ModelSpec;
import com.yahoo.squidb.processor.data.TableModelSpecWrapper;
import com.yahoo.squidb.processor.plugins.Plugin;
import com.yahoo.squidb.processor.plugins.PluginEnvironment;
import com.yahoo.squidb.processor.plugins.defaults.properties.generators.interfaces.PropertyGenerator;
import com.yahoo.squidb.processor.plugins.defaults.properties.generators.interfaces.TableModelPropertyGenerator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;

/**
 * A {@link Plugin} that processes the {@link UpsertKey} annotation and generates code to make any table model specs
 * containing UpsertKey fields implement the Upsertable interface. See that interface for more information. This plugin
 * can be disabled by passing the {@link PluginEnvironment#OPTIONS_DISABLE_DEFAULT_UPSERT} option in the squidbOptions
 * key.
 */
public class UpsertPlugin extends Plugin {

    private static final ClassName UPSERTABLE = ClassName.get(TypeConstants.SQUIDB_DATA_PACKAGE, "Upsertable");
    private static final ClassName CRITERION = ClassName.get(TypeConstants.SQUIDB_SQL_PACKAGE, "Criterion");
    private static final TypeName CRITERION_ARRAY = ArrayTypeName.of(CRITERION);
    private static final ClassName INDEX = ClassName.get(TypeConstants.SQUIDB_SQL_PACKAGE, "Index");

    private static final String LOGICAL_KEY_COL_ARRAY_NAME = "logicalKeyColumns";

    private static final Pattern NOT_NULL = Pattern.compile("NOT\\s+NULL", Pattern.CASE_INSENSITIVE);

    private List<TableModelPropertyGenerator> upsertColumns = new ArrayList<>();

    public UpsertPlugin(ModelSpec<?, ?> modelSpec, PluginEnvironment pluginEnv) {
        super(modelSpec, pluginEnv);
    }

    @Override
    public boolean hasChangesForModelSpec() {
        return modelSpec instanceof TableModelSpecWrapper;
    }

    @Override
    public void afterProcessVariableElements() {
        for (TableModelPropertyGenerator generator : ((TableModelSpecWrapper) modelSpec).getPropertyGenerators()) {
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
        Collections.sort(upsertColumns, new Comparator<PropertyGenerator>() {
            @Override
            public int compare(PropertyGenerator o1, PropertyGenerator o2) {
                int key1Order = o1.getField().getAnnotation(UpsertKey.class).order();
                int key2Order = o2.getField().getAnnotation(UpsertKey.class).order();

                if (key1Order < key2Order) {
                    return -1;
                } else if (key1Order > key2Order) {
                    return 1;
                } else {
                    return 0;
                }
            }
        });
        checkUpsertColumnOrder();
        checkUpsertColumnsForNotNullConstraints();
    }

    // Make sure that the order values were reasonable, i.e. monotonically increasing
    private void checkUpsertColumnOrder() {
        int lastOrderValue = upsertColumns.get(0).getField().getAnnotation(UpsertKey.class).order();
        for (int i = 1; i < upsertColumns.size(); i++) {
            VariableElement nextField = upsertColumns.get(i).getField();
            int nextOrderValue = nextField.getAnnotation(UpsertKey.class).order();
            if (nextOrderValue <= lastOrderValue) {
                modelSpec.logError("UpsertKey logical column order was not increasing, shared an order value with "
                        + "field " + upsertColumns.get(i - 1).getField().getSimpleName(),
                        upsertColumns.get(i).getField());
                break;
            }
            lastOrderValue = nextOrderValue;
        }
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

            FieldSpec.Builder upsertIndexField = FieldSpec.builder(INDEX, "UPSERT_INDEX",
                    Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                    .initializer("TABLE.uniqueIndex($S, $L)", getUpsertIndexName(), LOGICAL_KEY_COL_ARRAY_NAME);
            builder.addField(upsertIndexField.build());
        }
    }

    private String getUpsertIndexName() {
        TableModelSpecWrapper tableModelSpec = (TableModelSpecWrapper) modelSpec;
        String tableName = tableModelSpec.getSpecAnnotation().tableName();
        return "idx_" + tableName + "_upsertColumns";
    }

    private void declareRowidHasPriorityMethod(TypeSpec.Builder builder) {
        boolean rowidHasPriority = true;
        UpsertConfig upsertConfig = modelSpec.getModelSpecElement().getAnnotation(UpsertConfig.class);
        if (upsertConfig != null) {
            rowidHasPriority = upsertConfig.rowidHasPriority();
        }

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
        final boolean failureThrowsException;
        UpsertConfig upsertConfig = modelSpec.getModelSpecElement().getAnnotation(UpsertConfig.class);
        failureThrowsException = upsertConfig == null || upsertConfig.missingLookupValueThrows();

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
