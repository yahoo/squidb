/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.writers;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.yahoo.squidb.processor.TypeConstants;
import com.yahoo.squidb.processor.data.InheritedModelSpecWrapper;
import com.yahoo.squidb.processor.plugins.PluginEnvironment;
import com.yahoo.squidb.processor.plugins.defaults.properties.generators.interfaces.InheritedModelPropertyGenerator;

import javax.lang.model.element.Modifier;

public class InheritedModelFileWriter extends ModelFileWriter<InheritedModelSpecWrapper> {

    public InheritedModelFileWriter(InheritedModelSpecWrapper modelSpec, PluginEnvironment pluginEnv) {
        super(modelSpec, pluginEnv);
    }

    @Override
    protected void declareAllProperties() {
        for (InheritedModelPropertyGenerator generator : modelSpec.getPropertyGenerators()) {
            FieldSpec.Builder propertyBuilder = generator.buildInheritedPropertyDeclaration();
            modelSpec.getPluginBundle().beforeDeclareProperty(builder, generator, propertyBuilder);
            FieldSpec property = propertyBuilder.build();
            builder.addField(property);
            modelSpec.getPluginBundle().afterDeclareProperty(builder, generator, property);
        }
    }

    @Override
    protected void declarePropertiesArray() {
        builder.addField(FieldSpec.builder(TypeConstants.PROPERTY_ARRAY, PROPERTIES_ARRAY_NAME,
                Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                .initializer("$T.$L", modelSpec.getModelSuperclass(), PROPERTIES_ARRAY_NAME).build());
    }

    @Override
    protected void buildPropertiesInitializationBlock(CodeBlock.Builder block) {
        // Not needed
    }

    @Override
    protected void declareDefaultValues() {
        // Override: do nothing, the superclass should take care of default values
    }

    @Override
    protected void buildDefaultValuesInitializationBlock(CodeBlock.Builder block) {
        // Nothing to do, see above
    }

}
