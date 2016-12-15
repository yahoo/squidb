/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.plugins.defaults;

import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.TypeSpec;
import com.yahoo.squidb.processor.StringUtils;
import com.yahoo.squidb.processor.plugins.AbstractPlugin;
import com.yahoo.squidb.processor.plugins.PluginEnvironment;
import com.yahoo.squidb.processor.plugins.defaults.properties.generators.interfaces.PropertyGenerator;

import javax.lang.model.element.Element;

/**
 * A plugin that controls copying javadocs from fields in the model spec to the corresponding properties in the
 * generated class. Other plugins can use the
 * {@link #getJavadocFromElement(PluginEnvironment, Element)} method to copy javadocs from other
 * elements in the model spec (e.g. model methods, constants, etc.). This plugin is enabled by default but can be
 * disabled by passing {@link PluginEnvironment#OPTIONS_DISABLE_JAVADOC_COPYING 'disableJavadoc'} as one of the
 * values for the 'squidbOptions' key.
 */
public class JavadocPlugin extends AbstractPlugin {

    @Override
    public void beforeBeginClassDeclaration(TypeSpec.Builder builder) {
        String generatedJavadoc = "This class was generated from the model spec at "
                + "{@link " + modelSpec.getModelSpecName() + "}\n";

        String elementJavadoc = getJavadocFromElement(pluginEnv, modelSpec.getModelSpecElement());
        if (!StringUtils.isEmpty(elementJavadoc)) {
            generatedJavadoc = (generatedJavadoc + "<p>\n" + elementJavadoc);
        }
        builder.addJavadoc(generatedJavadoc);
    }

    @Override
    public void willDeclareProperty(TypeSpec.Builder builder, PropertyGenerator propertyGenerator,
            FieldSpec.Builder propertyDeclaration) {
        if (propertyGenerator.getField() != null) {
            String javadoc = getJavadocFromElement(pluginEnv, propertyGenerator.getField());
            if (!StringUtils.isEmpty(javadoc)) {
                propertyDeclaration.addJavadoc(javadoc);
            }
        }
    }

    /**
     * Helper method that retrieves Javadocs from an element if Javadoc copying is enabled in the PluginEnvironment
     */
    public static String getJavadocFromElement(PluginEnvironment pluginEnv, Element element) {
        if (!pluginEnv.hasSquidbOption(PluginEnvironment.OPTIONS_DISABLE_JAVADOC_COPYING)) {
            String javadoc = pluginEnv.getProcessingEnvironment().getElementUtils().getDocComment(element);
            if (javadoc != null) {
                String toReturn = javadoc.trim().replaceAll("\n\\s+", "\n");
                if (!toReturn.endsWith("\n")) {
                    toReturn = toReturn + "\n";
                }
                return toReturn;
            }
        }
        return null;
    }
}
