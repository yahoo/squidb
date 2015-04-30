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
import com.yahoo.squidb.annotations.ColumnSpec;
import com.yahoo.squidb.annotations.ViewModelSpec;
import com.yahoo.squidb.annotations.ViewQuery;
import com.yahoo.squidb.processor.TypeConstants;
import com.yahoo.squidb.processor.properties.factory.PropertyGeneratorFactory;
import com.yahoo.squidb.processor.properties.generators.PropertyGenerator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.tools.Diagnostic.Kind;

public class ViewModelFileWriter extends ModelFileWriter<ViewModelSpec> {

    private static final String BASE_PROPERTY_ARRAY_NAME = "BASE_PROPERTIES";
    private static final String ALIASED_PROPERTY_ARRAY_NAME = "ALIASED_PROPERTIES";
    private static final String QUERY_NAME = "QUERY";
    private static final String VIEW_NAME = "VIEW";
    private static final String SUBQUERY_NAME = "SUBQUERY";

    private VariableElement queryElement;

    private ViewQuery viewQueryAnnotation;

    private static final MethodDeclarationParameters GET_TABLE_MAPPING_VISITORS;

    static {
        GET_TABLE_MAPPING_VISITORS = new MethodDeclarationParameters()
                .setMethodName("getTableMappingVisitors")
                .setReturnType(TypeConstants.TABLE_MAPPING_VISITORS)
                .setModifiers(Modifier.PROTECTED);
    }

    public ViewModelFileWriter(TypeElement element, PropertyGeneratorFactory propertyGeneratorFactory, AptUtils utils) {
        super(element, ViewModelSpec.class, propertyGeneratorFactory, utils);
    }

    @Override
    protected String getGeneratedClassName() {
        return modelSpec.className();
    }

    @Override
    protected DeclaredTypeName getModelSuperclass() {
        return TypeConstants.VIEW_MODEL;
    }

    @Override
    protected void processVariableElement(VariableElement e, DeclaredTypeName typeName) {
        if (e.getAnnotation(Deprecated.class) != null) {
            return;
        }
        if (e.getAnnotation(ColumnSpec.class) != null) {
            utils.getMessager().printMessage(Kind.WARNING, "ColumnSpec is ignored outside of table models", e);
        }
        boolean isViewProperty = TypeConstants.isPropertyType(typeName);
        ViewQuery isViewQuery = e.getAnnotation(ViewQuery.class);
        Set<Modifier> modifiers = e.getModifiers();
        if (modifiers.containsAll(TypeConstants.PUBLIC_STATIC_FINAL)) {
            if (isViewQuery != null) {
                if (!TypeConstants.QUERY.equals(typeName)) {
                    utils.getMessager().printMessage(Kind.ERROR,
                            "ViewQuery must be an instance of " + TypeConstants.QUERY.toString());
                } else if (queryElement != null) {
                    utils.getMessager().printMessage(Kind.ERROR, "Only one ViewQuery per spec allowedd");
                } else {
                    viewQueryAnnotation = isViewQuery;
                    queryElement = e;
                }
            } else if (!isViewProperty) {
                constantElements.add(e);
            } else {
                initializePropertyGenerator(e);
            }
        } else if (isViewProperty) {
            utils.getMessager().printMessage(Kind.ERROR, "View properties must be public static final", e);
        } else {
            utils.getMessager().printMessage(Kind.WARNING, "Unused field in spec", e);
        }
    }

    @Override
    protected Collection<DeclaredTypeName> getModelSpecificImports() {
        if (queryElement != null) {
            List<DeclaredTypeName> imports = new ArrayList<DeclaredTypeName>();
            if (modelSpec.isSubquery()) {
                imports.add(TypeConstants.SUBQUERY_TABLE);
            } else {
                imports.add(TypeConstants.VIEW);
            }
            imports.add(TypeConstants.QUERY);
            return imports;
        }
        return null;
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
        for (PropertyGenerator e : propertyGenerators) {
            Expression reference = Expressions.staticReference(sourceElementName, e.getPropertyName());
            if (alias) {
                Alias aliasAnnotation = e.getElement().getAnnotation(Alias.class);
                if (aliasAnnotation != null && !AptUtils.isEmpty(aliasAnnotation.value())) {
                    reference = reference.callMethod("as", "\"" + aliasAnnotation.value() + "\"");
                }
            }
            writer.writeExpression(reference);
            writer.appendString(",\n");
        }
        return !AptUtils.isEmpty(propertyGenerators);
    }

    private void emitQueryAndTableDeclaration() throws IOException {
        emitSqlTableDeclaration(!modelSpec.isSubquery());
    }

    private void emitSqlTableDeclaration(boolean view) throws IOException {
        writer.writeComment("--- " + (view ? "view" : "subquery") + " declaration");
        String name = "\"" + modelSpec.viewName() + "\"";
        if (queryElement != null) {
            Expression queryReference = Expressions.staticReference(sourceElementName,
                    queryElement.getSimpleName().toString()).callMethod("selectMore", ALIASED_PROPERTY_ARRAY_NAME);
            if (viewQueryAnnotation.freeze()) {
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
            return Expressions.staticMethod(TypeConstants.VIEW, "fromQuery",
                    QUERY_NAME, name, Expressions.classObject(generatedClassName), PROPERTIES_ARRAY_NAME);
        } else {
            return Expressions.callMethodOn(QUERY_NAME, "as", name, Expressions.classObject(generatedClassName),
                    PROPERTIES_ARRAY_NAME);
        }
    }

    @Override
    protected void emitAllProperties() throws IOException {
        for (int i = 0; i < propertyGenerators.size(); i++) {
            emitSinglePropertyDeclaration(propertyGenerators.get(i), i);
        }
    }

    private void emitSinglePropertyDeclaration(PropertyGenerator generator, int index) throws IOException {
        generator.beforeEmitPropertyDeclaration(writer);
        DeclaredTypeName type = generator.getPropertyType();
        String fieldToQualify = ALIASED_PROPERTY_ARRAY_NAME + "[" + index + "]";
        Expression expressionToCast;
        if (queryElement != null) {
            String callOn = modelSpec.isSubquery() ? SUBQUERY_NAME : VIEW_NAME;
            expressionToCast = Expressions.callMethodOn(callOn, "qualifyField", fieldToQualify);
        } else {
            expressionToCast = Expressions.reference(fieldToQualify);
        }
        writer.writeFieldDeclaration(type, generator.getPropertyName(),
                expressionToCast.cast(type), TypeConstants.PUBLIC_STATIC_FINAL)
                .writeNewline();
        generator.afterEmitPropertyDeclaration(writer);
    }

    @Override
    protected int getPropertiesArrayLength() {
        return propertyGenerators.size();
    }

    @Override
    protected void writePropertiesInitializationBlock() throws IOException {
        for (int i = 0; i < propertyGenerators.size(); i++) {
            writer.writeStatement(Expressions
                    .assign(Expressions.arrayReference(PROPERTIES_ARRAY_NAME, i),
                            Expressions.fromString(propertyGenerators.get(i).getPropertyName())));
        }
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
