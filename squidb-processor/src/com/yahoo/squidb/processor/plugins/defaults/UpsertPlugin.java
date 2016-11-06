/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.plugins.defaults;

import com.yahoo.aptutils.model.CoreTypes;
import com.yahoo.aptutils.model.DeclaredTypeName;
import com.yahoo.aptutils.writer.JavaFileWriter;
import com.yahoo.aptutils.writer.expressions.Expression;
import com.yahoo.aptutils.writer.expressions.Expressions;
import com.yahoo.aptutils.writer.parameters.MethodDeclarationParameters;
import com.yahoo.squidb.annotations.UpsertConfig;
import com.yahoo.squidb.annotations.UpsertKey;
import com.yahoo.squidb.processor.TypeConstants;
import com.yahoo.squidb.processor.data.ModelSpec;
import com.yahoo.squidb.processor.data.TableModelSpecWrapper;
import com.yahoo.squidb.processor.plugins.Plugin;
import com.yahoo.squidb.processor.plugins.PluginEnvironment;
import com.yahoo.squidb.processor.plugins.defaults.properties.generators.interfaces.PropertyGenerator;
import com.yahoo.squidb.processor.plugins.defaults.properties.generators.interfaces.TableModelPropertyGenerator;

import java.io.IOException;
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

    private static final DeclaredTypeName UPSERTABLE =
            new DeclaredTypeName(TypeConstants.SQUIDB_DATA_PACKAGE, "Upsertable");

    private static final DeclaredTypeName CRITERION =
            new DeclaredTypeName(TypeConstants.SQUIDB_SQL_PACKAGE, "Criterion");

    private static final DeclaredTypeName CRITERION_ARRAY = CRITERION.clone();
    static {
        CRITERION_ARRAY.setArrayDepth(1);
    }

    private static final DeclaredTypeName INDEX =
            new DeclaredTypeName(TypeConstants.SQUIDB_SQL_PACKAGE, "Index");

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
    public void addRequiredImports(Set<DeclaredTypeName> imports) {
        if (!upsertColumns.isEmpty()) {
            imports.add(UPSERTABLE);
            imports.add(CRITERION);
            imports.add(INDEX);
        }
    }

    @Override
    public void addInterfacesToImplement(Set<DeclaredTypeName> interfaces) {
        if (!upsertColumns.isEmpty()) {
            interfaces.add(UPSERTABLE);
        }
    }

    @Override
    public void afterEmitSchema(JavaFileWriter writer) throws IOException {
        if (!upsertColumns.isEmpty()) {
            writer.writeComment("--- columns and uniqueness index for Upsertable logical key columns");

            writer.writeFieldDeclaration(TypeConstants.PROPERTY_ARRAY, LOGICAL_KEY_COL_ARRAY_NAME, new Expression() {
                @Override
                public boolean writeExpression(JavaFileWriter writer) throws IOException {
                    writer.appendString("new ").appendString(writer.shortenName(TypeConstants.PROPERTY, false));
                    writer.appendString("[] ");
                    Expressions.block(new Expression() {
                        @Override
                        public boolean writeExpression(JavaFileWriter writer) throws IOException {
                            for (PropertyGenerator generator : upsertColumns) {
                                writer.writeString(generator.getPropertyName())
                                        .appendString(",")
                                        .writeNewline();
                            }
                            return true;
                        }
                    }, false, false, false, false).writeExpression(writer);
                    return true;
                }
            }, TypeConstants.PRIVATE_STATIC_FINAL);
            writer.writeNewline();

            writer.writeFieldDeclaration(INDEX, "UPSERT_INDEX", Expressions.callMethodOn("TABLE", "uniqueIndex",
                    "\"" + getUpsertIndexName() + "\"", LOGICAL_KEY_COL_ARRAY_NAME),
                    TypeConstants.PUBLIC_STATIC_FINAL);
            writer.writeNewline();
        }
    }

    private String getUpsertIndexName() {
        TableModelSpecWrapper tableModelSpec = (TableModelSpecWrapper) modelSpec;
        String tableName = tableModelSpec.getSpecAnnotation().tableName();
        return "idx_" + tableName + "_upsertColumns";
    }

    @Override
    public void emitMethods(JavaFileWriter writer) throws IOException {
        if (!upsertColumns.isEmpty()) {
            emitRowidHasPriorityMethod(writer);
            emitGetUpsertKeyLookupCriterion(writer);
        }
    }

    private void emitRowidHasPriorityMethod(JavaFileWriter writer) throws IOException {
        boolean rowidHasPriority = true;
        UpsertConfig upsertConfig = modelSpec.getModelSpecElement().getAnnotation(UpsertConfig.class);
        if (upsertConfig != null) {
            rowidHasPriority = upsertConfig.rowidHasPriority();
        }

        MethodDeclarationParameters params = new MethodDeclarationParameters()
                .setMethodName("rowidHasPriority")
                .setReturnType(CoreTypes.PRIMITIVE_BOOLEAN)
                .setModifiers(Modifier.PUBLIC);

        writer.writeAnnotation(CoreTypes.OVERRIDE);
        writer.beginMethodDefinition(params);
        writer.writeStatement(Expressions.fromString(Boolean.toString(rowidHasPriority)).returnExpr());
        writer.finishMethodDefinition();
    }

    private void emitGetUpsertKeyLookupCriterion(JavaFileWriter writer) throws IOException {
        MethodDeclarationParameters params = new MethodDeclarationParameters()
                .setMethodName("getUpsertKeyLookupCriterion")
                .setReturnType(CRITERION)
                .setModifiers(Modifier.PUBLIC);

        writer.writeAnnotation(CoreTypes.OVERRIDE);
        writer.beginMethodDefinition(params);

        emitGetUpsertKeyLookupCriterionBody(writer);

        writer.finishMethodDefinition();
    }

    private void emitGetUpsertKeyLookupCriterionBody(JavaFileWriter writer) throws IOException {
        final boolean failureThrowsException;
        UpsertConfig upsertConfig = modelSpec.getModelSpecElement().getAnnotation(UpsertConfig.class);
        failureThrowsException = upsertConfig == null || upsertConfig.lookupCriterionFailLoudly();

        writer.writeString("for (int i = 0; i < " + upsertColumns.size() + "; i++) {").writeNewline();
        writer.moveToScope(JavaFileWriter.Scope.METHOD_DEFINITION);

        String arrayReference = LOGICAL_KEY_COL_ARRAY_NAME + "[i]";
        writer.writeFieldDeclaration(TypeConstants.PROPERTY, "col",
                Expressions.fromString(arrayReference));
        writer.writeString("if (!containsNonNullValue(col)) {").writeNewline();
        writer.moveToScope(JavaFileWriter.Scope.METHOD_DEFINITION);
        writer.writeStatement(badValueFailureExpression(arrayReference, failureThrowsException));
        writer.finishScope(JavaFileWriter.Scope.METHOD_DEFINITION);
        writer.writeString("}").writeNewline();
        writer.finishScope(JavaFileWriter.Scope.METHOD_DEFINITION);
        writer.writeString("}").writeNewline();

        writer.writeFieldDeclaration(CRITERION, "result",
                Expressions.arrayReference(LOGICAL_KEY_COL_ARRAY_NAME, 0).callMethod("eq",
                Expressions.callMethod("get", Expressions.arrayReference(LOGICAL_KEY_COL_ARRAY_NAME, 0))));
        if (upsertColumns.size() > 1) {
            writer.writeFieldDeclaration(CRITERION_ARRAY, "extraCriterion",
                    Expressions.arrayAllocation(CRITERION, 1, upsertColumns.size() - 1));
            writer.writeString("for (int i = 1; i < " + upsertColumns.size() + "; i++) {").writeNewline();
            writer.moveToScope(JavaFileWriter.Scope.METHOD_DEFINITION);
            writer.writeStringStatement("extraCriterion[i - 1] = " + LOGICAL_KEY_COL_ARRAY_NAME
                    + "[i].eq(get(" + LOGICAL_KEY_COL_ARRAY_NAME + "[i]))");
            writer.finishScope(JavaFileWriter.Scope.METHOD_DEFINITION);
            writer.writeString("}").writeNewline();
            writer.writeStatement(Expressions.assign(Expressions.fromString("result"),
                    Expressions.staticMethod(CRITERION, "and", "result", "extraCriterion")));
        }

        writer.writeStatement(Expressions.fromString("result").returnExpr());
    }

    private Expression badValueFailureExpression(final String columnRef, boolean failureThrowsException) {
        if (failureThrowsException) {
            return new Expression() {
                @Override
                public boolean writeExpression(JavaFileWriter writer) throws IOException {
                    writer.appendString("throw new IllegalArgumentException(\"Value for upsert logical key column \""
                            + " + " + columnRef + ".getName() + \" was missing when trying to upsert item of class \""
                            + " + getClass())");
                    return true;
                }
            };
        } else {
            return Expressions.fromString("null").returnExpr();
        }
    }
}
