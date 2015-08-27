/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.plugins;

import com.yahoo.aptutils.model.DeclaredTypeName;
import com.yahoo.aptutils.utils.AptUtils;
import com.yahoo.aptutils.writer.JavaFileWriter;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.VariableElement;
import javax.tools.Diagnostic;

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

    public boolean processVariableElement(VariableElement field, DeclaredTypeName fieldType) {
        for (Plugin plugin : plugins) {
            if (plugin.modelSpec.getGeneratedClassName().getSimpleName().equals("TestModel")) {
                utils.getMessager().printMessage(Diagnostic.Kind.WARNING,
                        "Checking plugin " + plugin.getClass() + " for field " + field.getSimpleName());
            }
            if (plugin.canProcessModelSpec() && plugin.processVariableElement(field, fieldType)) {
                utils.getMessager().printMessage(Diagnostic.Kind.WARNING, "Processed, returning true");
                return true;
            }
        }
        return false;
    }

    /**
     * Calls {@link Plugin#addRequiredImports(Set)} on all the bundled plugins
     */
    public void addRequiredImports(Set<DeclaredTypeName> imports) {
        for (Plugin plugin : plugins) {
            if (plugin.canProcessModelSpec()) {
                plugin.addRequiredImports(imports);
            }
        }
    }

    /**
     * Calls {@link Plugin#addInterfacesToImplement(Set)} on all the bundled plugins
     */
    public void addInterfacesToImplement(Set<DeclaredTypeName> interfaces) {
        for (Plugin plugin : plugins) {
            if (plugin.canProcessModelSpec()) {
                plugin.addInterfacesToImplement(interfaces);
            }
        }
    }

    /**
     * Calls {@link Plugin#writeConstants(JavaFileWriter)} on all the bundled plugins
     */
    public void writeConstants(JavaFileWriter writer) throws IOException {
        for (Plugin plugin : plugins) {
            if (plugin.canProcessModelSpec()) {
                plugin.writeConstants(writer);
            }
        }
    }

    /**
     * Calls {@link Plugin#writeConstructors(JavaFileWriter)} on all the bundled plugins
     */
    public void writeConstructors(JavaFileWriter writer) throws IOException {
        for (Plugin plugin : plugins) {
            if (plugin.canProcessModelSpec()) {
                plugin.writeConstructors(writer);
            }
        }
    }

    /**
     * Calls {@link Plugin#writeMethods(JavaFileWriter)} on all the bundled plugins
     */
    public void writeMethods(JavaFileWriter writer) throws IOException {
        for (Plugin plugin : plugins) {
            if (plugin.canProcessModelSpec()) {
                plugin.writeMethods(writer);
            }
        }
    }

    /**
     * Calls {@link Plugin#writeAdditionalCode(JavaFileWriter)} on all the bundled plugins
     */
    public void writeAdditionalCode(JavaFileWriter writer) throws IOException {
        for (Plugin plugin : plugins) {
            if (plugin.canProcessModelSpec()) {
                plugin.writeAdditionalCode(writer);
            }
        }
    }
}
