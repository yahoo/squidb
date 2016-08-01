/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.plugins.defaults;

import com.yahoo.aptutils.utils.AptUtils;
import com.yahoo.aptutils.writer.JavaFileWriter;
import com.yahoo.squidb.processor.data.ModelSpec;
import com.yahoo.squidb.processor.plugins.Plugin;
import com.yahoo.squidb.processor.plugins.PluginEnvironment;
import com.yahoo.squidb.processor.plugins.defaults.properties.generators.PropertyGenerator;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import javax.lang.model.element.Element;

/**
 * A plugin that controls copying javadocs from fields in the model spec to the corresponding properties in the
 * generated class. Other plugins can use the
 * {@link #writeJavadocFromElement(PluginEnvironment, JavaFileWriter, Element)} method to copy javadocs from other
 * elements in the model spec (e.g. model methods, constants, etc.)
 */
public class JavadocPlugin extends Plugin {

    public JavadocPlugin(ModelSpec<?> modelSpec, PluginEnvironment pluginEnv) {
        super(modelSpec, pluginEnv);
    }

    @Override
    public List<String> getSupportedOptions() {
        return Collections.singletonList(PluginEnvironment.OPTIONS_DISABLE_JAVADOC_COPYING);
    }

    @Override
    public void beforeEmitClassDeclaration(JavaFileWriter writer) throws IOException {
        String generatedJavadoc = " This class was generated from the model spec at "
                + "{@link " + modelSpec.getModelSpecName() + "}";

        String elementJavadoc = utils.getElements().getDocComment(modelSpec.getModelSpecElement());
        if (!AptUtils.isEmpty(elementJavadoc)) {
            generatedJavadoc = (generatedJavadoc + "\n <br/>\n" + elementJavadoc);
        }
        writer.writeJavadoc(generatedJavadoc);
        writer.writeComment("Generated code -- do not modify!");
    }

    @Override
    public void beforeEmitPropertyDeclaration(JavaFileWriter writer, PropertyGenerator propertyGenerator)
            throws IOException {
        writeJavadocFromElement(pluginEnv, writer, propertyGenerator.getField());
    }

    /**
     * Helper method that other plugins can use to copy javadocs from an Element
     */
    public static void writeJavadocFromElement(PluginEnvironment pluginEnv, JavaFileWriter writer, Element element)
            throws IOException {
        if (!pluginEnv.hasOption(PluginEnvironment.OPTIONS_DISABLE_JAVADOC_COPYING)) {
            writer.writeJavadoc(pluginEnv.getUtils().getElements().getDocComment(element));
        }
    }
}
