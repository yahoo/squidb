/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.writers;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.yahoo.squidb.processor.TypeConstants;
import com.yahoo.squidb.processor.data.ViewModelSpecWrapper;
import com.yahoo.squidb.processor.plugins.PluginEnvironment;
import com.yahoo.squidb.processor.plugins.defaults.properties.generators.interfaces.ViewModelPropertyGenerator;

import javax.lang.model.element.Modifier;

public class ViewModelFileWriter extends ModelFileWriter<ViewModelSpecWrapper> {

    private static final String BASE_PROPERTY_ARRAY_NAME = "BASE_PROPERTIES";
    private static final String ALIASED_PROPERTY_ARRAY_NAME = "ALIASED_PROPERTIES";
    private static final String QUERY_NAME = "QUERY";
    private static final String VIEW_NAME = "VIEW";
    private static final String SUBQUERY_NAME = "SUBQUERY";

    public ViewModelFileWriter(ViewModelSpecWrapper modelSpec, PluginEnvironment pluginEnv) {
        super(modelSpec, pluginEnv);
    }

    @Override
    protected void declareModelSpecificFields() {
        declareUnaliasedPropertyArray();
        declareAliasedPropertyArray();
        declareViewOrSubqueryDeclaration();
        declareViewInitializer();
    }

    private void declareUnaliasedPropertyArray() {
        FieldSpec.Builder unaliasedProperties = FieldSpec.builder(TypeConstants.PROPERTY_ARRAY, BASE_PROPERTY_ARRAY_NAME,
                Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL);
        CodeBlock.Builder unaliasedPropertiesInitializer = CodeBlock.builder()
                .beginControlFlow("");
        buildPropertyReferenceArrayBody(unaliasedPropertiesInitializer, false);
        unaliasedPropertiesInitializer.endControlFlow();
        unaliasedProperties.initializer(unaliasedPropertiesInitializer.build());
        builder.addField(unaliasedProperties.build());
    }

    private void declareAliasedPropertyArray() {
        FieldSpec.Builder aliasedProperties = FieldSpec.builder(TypeConstants.PROPERTY_ARRAY,
                ALIASED_PROPERTY_ARRAY_NAME, Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL);
        CodeBlock.Builder aliasedPropertiesInitializer = CodeBlock.builder()
                .beginControlFlow("");
        buildPropertyReferenceArrayBody(aliasedPropertiesInitializer, true);
        aliasedPropertiesInitializer.endControlFlow();
        aliasedProperties.initializer(aliasedPropertiesInitializer.build());
        builder.addField(aliasedProperties.build());
    }

    private void declareViewInitializer() {
        CodeBlock.Builder viewInitializer = CodeBlock.builder()
                .addStatement("validateAliasedProperties($L)", ALIASED_PROPERTY_ARRAY_NAME);
        if (modelSpec.getQueryElement() != null) {
            String queryElementName = modelSpec.getQueryElement().getSimpleName().toString();
            if (modelSpec.getViewQueryAnnotation().freeze()) {
                viewInitializer.addStatement("$L = $T.$L.selectMore($L).freeze()", QUERY_NAME,
                        modelSpec.getModelSpecName(), queryElementName, ALIASED_PROPERTY_ARRAY_NAME);
            } else {
                viewInitializer.addStatement("$L = $T.$L.selectMore($L)", QUERY_NAME,
                        modelSpec.getModelSpecName(), queryElementName);
            }

            boolean view = !modelSpec.getSpecAnnotation().isSubquery();
            String fieldName = view ? VIEW_NAME : SUBQUERY_NAME;
            String name = modelSpec.getSpecAnnotation().viewName().trim();
            if (view) {
                viewInitializer.addStatement("$L = $T.fromQuery($L, $S, $T.class, $L)", fieldName, TypeConstants.VIEW,
                        QUERY_NAME, name, modelSpec.getGeneratedClassName(), PROPERTIES_LIST_NAME);
            } else {
                viewInitializer.addStatement("$L = $L.as($S, $T.class, $L)", fieldName, QUERY_NAME, name,
                        modelSpec.getGeneratedClassName(), PROPERTIES_LIST_NAME);
            }
        }
        builder.addStaticBlock(viewInitializer.build());
    }

    private void buildPropertyReferenceArrayBody(CodeBlock.Builder block, boolean writeAlias) {
        for (ViewModelPropertyGenerator propertyGenerator : modelSpec.getPropertyGenerators()) {
            block.add("$L,\n", propertyGenerator.buildViewPropertyReference(writeAlias));
        }
    }

    private void declareViewOrSubqueryDeclaration() {
        boolean view = !modelSpec.getSpecAnnotation().isSubquery();
        String name = modelSpec.getSpecAnnotation().viewName().trim();
        if (modelSpec.getQueryElement() != null) {
            FieldSpec.Builder query = FieldSpec.builder(TypeConstants.QUERY, QUERY_NAME,
                    Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL);
            builder.addField(query.build());


            TypeName type = view ? TypeConstants.VIEW : TypeConstants.SUBQUERY_TABLE;
            String fieldName = view ? VIEW_NAME : SUBQUERY_NAME;
            FieldSpec.Builder viewOrSubquery = FieldSpec.builder(type, fieldName,
                    Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL);
            builder.addField(viewOrSubquery.build());
        } else {
            builder.addField(FieldSpec.builder(TypeName.get(String.class), view ? "VIEW_NAME" : "SUBQUERY_NAME",
                    Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL).initializer("$S", name).build());
        }
    }

    @Override
    protected void declareAllProperties() {
        for (int i = 0; i < modelSpec.getPropertyGenerators().size(); i++) {
            declareSingleProperty(modelSpec.getPropertyGenerators().get(i));
        }
    }

    private void declareSingleProperty(ViewModelPropertyGenerator generator) {
        TypeName type = generator.getPropertyType();

        FieldSpec.Builder propertyBuilder = FieldSpec.builder(type, generator.getPropertyName(),
                Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL);
        modelSpec.getPluginBundle().beforeDeclareProperty(builder, generator, propertyBuilder);
        FieldSpec property = propertyBuilder.build();
        builder.addField(property);
        modelSpec.getPluginBundle().afterDeclareProperty(builder, generator, property);
    }

    private void declareTableModelMapper() {
        FieldSpec.Builder tableMappers = FieldSpec.builder(TypeConstants.TABLE_MAPPING_VISITORS, "tableMappingInfo",
                Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL);
        builder.addField(tableMappers.build());

        MethodSpec.Builder params = MethodSpec.methodBuilder("getTableMappingVisitors")
                .addAnnotation(Override.class)
                .returns(TypeConstants.TABLE_MAPPING_VISITORS)
                .addModifiers(Modifier.PROTECTED)
                .addStatement("return tableMappingInfo");
        builder.addMethod(params.build());

        builder.addStaticBlock(CodeBlock.of("tableMappingInfo = generateTableMappingVisitors($L, $L, $L);\n",
                PROPERTIES_LIST_NAME, ALIASED_PROPERTY_ARRAY_NAME, BASE_PROPERTY_ARRAY_NAME));
    }

    @Override
    protected void buildPropertiesInitializationBlock(CodeBlock.Builder block) {
        for (int i = 0; i < modelSpec.getPropertyGenerators().size(); i++) {
            String name = modelSpec.getPropertyGenerators().get(i).getPropertyName();
            TypeName type = modelSpec.getPropertyGenerators().get(i).getPropertyType();
            CodeBlock initializer;
            if (modelSpec.getQueryElement() != null) {
                String callOn = modelSpec.getSpecAnnotation().isSubquery() ? SUBQUERY_NAME : VIEW_NAME;
                initializer = CodeBlock.of("($T) $L.qualifyField($L[$L])", type, callOn,
                        ALIASED_PROPERTY_ARRAY_NAME, i);
            } else {
                initializer = CodeBlock.of("($T) $L[$L]", type, ALIASED_PROPERTY_ARRAY_NAME, i);
            }
            block.addStatement("$L = $L", name, initializer);
            block.addStatement("$L.add($L)", PROPERTIES_INTERNAL_ARRAY, name);
        }
    }

    @Override
    protected void buildDefaultValuesInitializationBlock(CodeBlock.Builder block) {
        //
    }

    @Override
    protected void declareModelSpecificHelpers() {
        declareTableModelMapper();
    }
}
