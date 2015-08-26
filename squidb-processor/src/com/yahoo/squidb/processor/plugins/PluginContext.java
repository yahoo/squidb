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

public class PluginContext {

    private final List<Plugin> plugins;
    private final AptUtils utils;

    public PluginContext(List<Plugin> plugins, AptUtils utils) {
        this.plugins = plugins;
        this.utils = utils;
    }

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

    public void addRequiredImports(Set<DeclaredTypeName> imports) {
        for (Plugin plugin : plugins) {
            plugin.addRequiredImports(imports);
        }
    }

    public void addInterfacesToImplement(Set<DeclaredTypeName> interfaces) {
        for (Plugin plugin : plugins) {
            plugin.addInterfacesToImplement(interfaces);
        }
    }

    public void writeConstants(JavaFileWriter writer) throws IOException {
        for (Plugin plugin : plugins) {
            plugin.writeConstants(writer);
        }
    }

    public void writeConstructors(JavaFileWriter writer) throws IOException {
        for (Plugin plugin : plugins) {
            plugin.writeConstructors(writer);
        }
    }

    public void writeMethods(JavaFileWriter writer) throws IOException {
        for (Plugin plugin : plugins) {
            plugin.writeMethods(writer);
        }
    }

    public void writeAdditionalCode(JavaFileWriter writer) throws IOException {
        for (Plugin plugin : plugins) {
            plugin.writeAdditionalCode(writer);
        }
    }
}
