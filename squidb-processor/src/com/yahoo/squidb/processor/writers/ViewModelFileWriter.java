/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.writers;

import com.yahoo.aptutils.model.CoreTypes;
import com.yahoo.aptutils.model.DeclaredTypeName;
import com.yahoo.aptutils.utils.AptUtils;
import com.yahoo.aptutils.writer.JavaFileWriter;
import com.yahoo.aptutils.writer.expressions.Expression;
import com.yahoo.aptutils.writer.expressions.Expressions;
import com.yahoo.aptutils.writer.parameters.MethodDeclarationParameters;
import com.yahoo.squidb.annotations.Alias;
import com.yahoo.squidb.processor.TypeConstants;
import com.yahoo.squidb.processor.data.ViewModelSpecWrapper;
import com.yahoo.squidb.processor.plugins.PluginEnvironment;
import com.yahoo.squidb.processor.plugins.defaults.properties.generators.PropertyGenerator;

import java.io.IOException;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;

public class ViewModelFileWriter extends ModelFileWriter<ViewModelSpecWrapper> {

    private static final String BASE_PROPERTY_ARRAY_NAME = "BASE_PROPERTIES";
    private static final String ALIASED_PROPERTY_ARRAY_NAME = "ALIASED_PROPERTIES";
    private static final String QUERY_NAME = "QUERY";
    private static final String VIEW_NAME = "VIEW";
    private static final String SUBQUERY_NAME = "SUBQUERY";

    private static final MethodDeclarationParameters GET_TABLE_MAPPING_VISITORS;

    static {
        GET_TABLE_MAPPING_VISITORS = new MethodDeclarationParameters()
                .setMethodName("getTableMappingVisitors")
                .setReturnType(TypeConstants.TABLE_MAPPING_VISITORS)
                .setModifiers(Modifier.PROTECTED);
    }

    public ViewModelFileWriter(TypeElement element, PluginEnvironment pluginEnv, AptUtils utils) {
        super(new ViewModelSpecWrapper(element, pluginEnv, utils), pluginEnv, utils);
    }

    @Override
    protected void emitModelSpecificFields() throws IOException {
        emitUnaliasedPropertyArray();
        emitAliasedPropertyArray();
        emitQueryAndTableDeclaration();
    }

    private void emitUnaliasedPropertyArray() throws IOException {
        writer.writeComment("--- unaliased property references");
        Expression basePropertiesInit = Expressions.block(new Expression() {
            @Override
            public boolean writeExpression(JavaFileWriter writer) throws IOException {
                return emitPropertyReferenceArrayBody(false);
            }
        }, false, false, false, false);

        writer.writeFieldDeclaration(TypeConstants.PROPERTY_ARRAY, BASE_PROPERTY_ARRAY_NAME,
                basePropertiesInit, TypeConstants.PRIVATE_STATIC_FINAL)
                .writeNewline();
    }

    private void emitAliasedPropertyArray() throws IOException {
        writer.writeComment("--- aliased property references");
        Expression aliasedPropertiesInit = Expressions.block(new Expression() {
            @Override
            public boolean writeExpression(JavaFileWriter writer) throws IOException {
                return emitPropertyReferenceArrayBody(true);
            }
        }, false, false, false, false);

        writer.writeFieldDeclaration(TypeConstants.PROPERTY_ARRAY, ALIASED_PROPERTY_ARRAY_NAME,
                aliasedPropertiesInit, TypeConstants.PUBLIC_STATIC_FINAL)
                .writeNewline();
        writer.beginInitializerBlock(true, true);
        writer.writeStatement(Expressions.callMethod("validateAliasedProperties", ALIASED_PROPERTY_ARRAY_NAME));
        writer.finishInitializerBlock(false, true);
        writer.writeNewline();
    }

    private boolean emitPropertyReferenceArrayBody(boolean alias) throws IOException {
        for (PropertyGenerator propertyGenerator : modelSpec.getPropertyGenerators()) {
            Expression reference = Expressions.staticReference(modelSpec.getModelSpecName(),
                    propertyGenerator.getPropertyName());
            if (alias) {
                VariableElement field = propertyGenerator.getField();
                if (field != null) {
                    Alias aliasAnnotation = field.getAnnotation(Alias.class);
                    if (aliasAnnotation != null && !AptUtils.isEmpty(aliasAnnotation.value().trim())) {
                        reference = reference.callMethod("as", "\"" + aliasAnnotation.value().trim() + "\"");
                    }
                }
            }
            writer.writeExpression(reference);
            writer.appendString(",\n");
        }
        return !AptUtils.isEmpty(modelSpec.getPropertyGenerators());
    }

    private void emitQueryAndTableDeclaration() throws IOException {
        emitSqlTableDeclaration(!modelSpec.getSpecAnnotation().isSubquery());
    }

    private void emitSqlTableDeclaration(boolean view) throws IOException {
        writer.writeComment("--- " + (view ? "view" : "subquery") + " declaration");
        String name = "\"" + modelSpec.getSpecAnnotation().viewName().trim() + "\"";
        if (modelSpec.getQueryElement() != null) {
            Expression queryReference = Expressions.staticReference(modelSpec.getModelSpecName(),
                    modelSpec.getQueryElement().getSimpleName().toString())
                    .callMethod("selectMore", ALIASED_PROPERTY_ARRAY_NAME);
            if (modelSpec.getViewQueryAnnotation().freeze()) {
                queryReference = queryReference.callMethod("freeze");
            }

            writer.writeFieldDeclaration(TypeConstants.QUERY, QUERY_NAME, queryReference,
                    TypeConstants.PUBLIC_STATIC_FINAL);

            Expression initializer = constructInitializer(name, view);
            writer.writeFieldDeclaration(view ? TypeConstants.VIEW : TypeConstants.SUBQUERY_TABLE,
                    view ? VIEW_NAME : SUBQUERY_NAME, initializer, TypeConstants.PUBLIC_STATIC_FINAL);
        } else {
            writer.writeFieldDeclaration(CoreTypes.JAVA_STRING, view ? "VIEW_NAME" : "SUBQUERY_NAME",
                    Expressions.fromString(name), TypeConstants.PUBLIC_STATIC_FINAL);
        }
        writer.writeNewline();
    }

    private Expression constructInitializer(String name, boolean view) {
        if (view) {
            return Expressions.staticMethod(TypeConstants.VIEW, "fromQuery", QUERY_NAME, name,
                    Expressions.classObject(modelSpec.getGeneratedClassName()), PROPERTIES_ARRAY_NAME);
        } else {
            return Expressions.callMethodOn(QUERY_NAME, "as", name,
                    Expressions.classObject(modelSpec.getGeneratedClassName()), PROPERTIES_ARRAY_NAME);
        }
    }

    @Override
    protected void emitAllProperties() throws IOException {
        for (int i = 0; i < modelSpec.getPropertyGenerators().size(); i++) {
            emitSinglePropertyDeclaration(modelSpec.getPropertyGenerators().get(i), i);
        }
    }

    private void emitSinglePropertyDeclaration(PropertyGenerator generator, int index) throws IOException {
        modelSpec.getPluginBundle().beforeEmitPropertyDeclaration(writer, generator);
        DeclaredTypeName type = generator.getPropertyType();
        String fieldToQualify = ALIASED_PROPERTY_ARRAY_NAME + "[" + index + "]";
        Expression expressionToCast;
        if (modelSpec.getQueryElement() != null) {
            String callOn = modelSpec.getSpecAnnotation().isSubquery() ? SUBQUERY_NAME : VIEW_NAME;
            expressionToCast = Expressions.callMethodOn(callOn, "qualifyField", fieldToQualify);
        } else {
            expressionToCast = Expressions.reference(fieldToQualify);
        }
        writer.writeFieldDeclaration(type, generator.getPropertyName(),
                expressionToCast.cast(type), TypeConstants.PUBLIC_STATIC_FINAL)
                .writeNewline();
        modelSpec.getPluginBundle().afterEmitPropertyDeclaration(writer, generator);
    }

    private void emitTableModelMapper() throws IOException {
        writer.writeComment("--- mappers");
        writer.writeFieldDeclaration(TypeConstants.TABLE_MAPPING_VISITORS, "tableMappingInfo",
                Expressions.callMethod("generateTableMappingVisitors",
                        PROPERTIES_ARRAY_NAME, ALIASED_PROPERTY_ARRAY_NAME, BASE_PROPERTY_ARRAY_NAME),
                TypeConstants.PRIVATE_STATIC_FINAL)
                .writeNewline();
        writer.writeAnnotation(CoreTypes.OVERRIDE)
                .beginMethodDefinition(GET_TABLE_MAPPING_VISITORS)
                .writeStringStatement("return tableMappingInfo")
                .finishMethodDefinition();
    }

    @Override
    protected void emitDefaultValuesInitializationBlock() throws IOException {
        //
    }

    @Override
    protected void emitModelSpecificHelpers() throws IOException {
        emitTableModelMapper();
    }
}
