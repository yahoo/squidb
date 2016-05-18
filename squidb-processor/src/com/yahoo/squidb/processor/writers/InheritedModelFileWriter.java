/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.writers;

import com.yahoo.aptutils.utils.AptUtils;
import com.yahoo.aptutils.writer.expressions.Expressions;
import com.yahoo.squidb.processor.TypeConstants;
import com.yahoo.squidb.processor.data.InheritedModelSpecWrapper;
import com.yahoo.squidb.processor.plugins.PluginEnvironment;
import com.yahoo.squidb.processor.plugins.defaults.properties.generators.PropertyGenerator;

import java.io.IOException;

import javax.lang.model.element.TypeElement;

public class InheritedModelFileWriter extends ModelFileWriter<InheritedModelSpecWrapper> {

    public InheritedModelFileWriter(TypeElement element, PluginEnvironment pluginEnv, AptUtils utils) {
        super(new InheritedModelSpecWrapper(element, pluginEnv, utils), pluginEnv, utils);
    }

    @Override
    protected void emitAllProperties() throws IOException {
        for (PropertyGenerator e : modelSpec.getPropertyGenerators()) {
            emitSinglePropertyDeclaration(e);
        }
    }

    private void emitSinglePropertyDeclaration(PropertyGenerator generator) throws IOException {
        modelSpec.getPluginBundle().beforeEmitPropertyDeclaration(writer, generator);
        writer.writeFieldDeclaration(generator.getPropertyType(), generator.getPropertyName(),
                Expressions.staticReference(modelSpec.getModelSpecName(), generator.getPropertyName()),
                TypeConstants.PUBLIC_STATIC_FINAL)
                .writeNewline();
        modelSpec.getPluginBundle().afterEmitPropertyDeclaration(writer, generator);
    }

    @Override
    protected void emitPropertiesArray() throws IOException {
        writer.writeFieldDeclaration(TypeConstants.PROPERTY_ARRAY, PROPERTIES_ARRAY_NAME,
                Expressions.staticReference(modelSpec.getModelSuperclass(), PROPERTIES_ARRAY_NAME),
                TypeConstants.PUBLIC_STATIC_FINAL);
    }

    @Override
    protected void writePropertiesInitializationBlock() throws IOException {
        // Not needed
    }

    @Override
    protected void emitPropertyArrayInitialization() throws IOException {
        // The superclass declares this
    }

    @Override
    protected void emitDefaultValues() throws IOException {
        // Override: do nothing, the superclass should take care of default values
    }

    @Override
    protected void emitDefaultValuesInitializationBlock() throws IOException {
        // Nothing to do, see above
    }

}
