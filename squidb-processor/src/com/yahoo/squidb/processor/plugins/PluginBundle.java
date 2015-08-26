/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.plugins;

import com.yahoo.aptutils.model.DeclaredTypeName;
import com.yahoo.aptutils.model.TypeName;
import com.yahoo.aptutils.utils.AptUtils;
import com.yahoo.aptutils.writer.JavaFileWriter;
import com.yahoo.squidb.processor.plugins.properties.generators.PropertyGenerator;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.VariableElement;

/**
 * This class wraps a list of {@link Plugin}s that have been instantiated for a single
 * {@link com.yahoo.squidb.processor.data.ModelSpec}. It provides a convenient way of iterating over all the plugins
 * present during code generation and calling them during the generation of a model class for a particular model
 * spec.
 */
public class PluginBundle {

    private final List<Plugin> plugins;
    private final AptUtils utils;

    /**
     * @param plugins a list of Plugins instantiated for a given model spec
     * @param utils annotation processing utilities class
     */
    public PluginBundle(List<Plugin> plugins, AptUtils utils) {
        this.plugins = plugins;
        this.utils = utils;
    }

    /**
     * This class searches all available plugins for one that can handle and return a {@link PropertyGenerator} for the
     * given field
     * @param field a {@link VariableElement} field in a model spec representing a Property to be generated
     * @return a PropertyGenerator for handling the given field, or null if none could be found in the available plugins
     */
    public PropertyGenerator getPropertyGeneratorForVariableElement(VariableElement field) {
        TypeName fieldType = utils.getTypeNameFromTypeMirror(field.asType());
        PropertyGenerator generator = null;
        if (fieldType instanceof DeclaredTypeName) {
            generator = searchPluginsForPropertyGenerator(field, (DeclaredTypeName) fieldType);
        }
        return generator;
    }

    private PropertyGenerator searchPluginsForPropertyGenerator(VariableElement field,
            DeclaredTypeName fieldType) {
        for (Plugin plugin : plugins) {
            if (plugin.hasPropertyGeneratorForField(field, fieldType)) {
                PropertyGenerator generator = plugin.getPropertyGenerator(field, fieldType);
                if (generator != null) {
                    return generator;
                }
            }
        }
        return null;
    }

    /**
     * Calls {@link Plugin#addRequiredImports(Set)} on all the bundled plugins
     */
    public void addRequiredImports(Set<DeclaredTypeName> imports) {
        for (Plugin plugin : plugins) {
            plugin.addRequiredImports(imports);
        }
    }

    /**
     * Calls {@link Plugin#addInterfacesToImplement(Set)} on all the bundled plugins
     */
    public void addInterfacesToImplement(Set<DeclaredTypeName> interfaces) {
        for (Plugin plugin : plugins) {
            plugin.addInterfacesToImplement(interfaces);
        }
    }

    /**
     * Calls {@link Plugin#writeConstants(JavaFileWriter)} on all the bundled plugins
     */
    public void writeConstants(JavaFileWriter writer) throws IOException {
        for (Plugin plugin : plugins) {
            plugin.writeConstants(writer);
        }
    }

    /**
     * Calls {@link Plugin#writeConstructors(JavaFileWriter)} on all the bundled plugins
     */
    public void writeConstructors(JavaFileWriter writer) throws IOException {
        for (Plugin plugin : plugins) {
            plugin.writeConstructors(writer);
        }
    }

    /**
     * Calls {@link Plugin#writeMethods(JavaFileWriter)} on all the bundled plugins
     */
    public void writeMethods(JavaFileWriter writer) throws IOException {
        for (Plugin plugin : plugins) {
            plugin.writeMethods(writer);
        }
    }

    /**
     * Calls {@link Plugin#writeAdditionalCode(JavaFileWriter)} on all the bundled plugins
     */
    public void writeAdditionalCode(JavaFileWriter writer) throws IOException {
        for (Plugin plugin : plugins) {
            plugin.writeAdditionalCode(writer);
        }
    }
}
