/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.plugins;

import com.yahoo.aptutils.model.DeclaredTypeName;
import com.yahoo.aptutils.writer.JavaFileWriter;

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

    /**
     * @param plugins a list of Plugins instantiated for a given model spec
     */
    public PluginBundle(List<Plugin> plugins) {
        this.plugins = plugins;
    }

    /**
     * Calls {@link Plugin#processVariableElement(VariableElement, DeclaredTypeName)} on all the bundled plugins until
     * one of them returns true to "claim" the field
     *
     * @param field a {@link VariableElement} field in a model spec
     * @param fieldType the type name of the field
     * @return true if any of the bundled plugins claimed the field for processing, false otherwise
     */
    public boolean processVariableElement(VariableElement field, DeclaredTypeName fieldType) {
        for (Plugin plugin : plugins) {
            if (plugin.hasChangesForModelSpec() && plugin.processVariableElement(field, fieldType)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Calls {@link Plugin#afterProcessVariableElements()} on all the bundled plugins
     */
    public void afterProcessVariableElements() {
        for (Plugin plugin : plugins) {
            if (plugin.hasChangesForModelSpec()) {
                plugin.afterProcessVariableElements();
            }
        }
    }

    /**
     * Calls {@link Plugin#getModelSuperclass()} on all the bundled plugins, returning the first non-null value returned
     * by any plugin
     *
     * @return the name of a class to use as the model superclass, or null if no plugin provided one
     */
    public DeclaredTypeName getModelSuperclass() {
        for (Plugin plugin : plugins) {
            DeclaredTypeName modelSuperclass = plugin.getModelSuperclass();
            if (modelSuperclass != null) {
                return modelSuperclass;
            }
        }
        return null;
    }

    /**
     * Calls {@link Plugin#addRequiredImports(Set)} on all the bundled plugins
     */
    public void addRequiredImports(Set<DeclaredTypeName> imports) {
        for (Plugin plugin : plugins) {
            if (plugin.hasChangesForModelSpec()) {
                plugin.addRequiredImports(imports);
            }
        }
    }

    /**
     * Calls {@link Plugin#addInterfacesToImplement(Set)} on all the bundled plugins
     */
    public void addInterfacesToImplement(Set<DeclaredTypeName> interfaces) {
        for (Plugin plugin : plugins) {
            if (plugin.hasChangesForModelSpec()) {
                plugin.addInterfacesToImplement(interfaces);
            }
        }
    }

    /**
     * Calls {@link Plugin#beforeEmitSchema(JavaFileWriter)} on all the bundled plugins
     */
    public void beforeEmitSchema(JavaFileWriter writer) throws IOException {
        for (Plugin plugin : plugins) {
            if (plugin.hasChangesForModelSpec()) {
                plugin.beforeEmitSchema(writer);
            }
        }
    }

    /**
     * Calls {@link Plugin#afterEmitSchema(JavaFileWriter)} on all the bundled plugins
     */
    public void afterEmitSchema(JavaFileWriter writer) throws IOException {
        for (Plugin plugin : plugins) {
            if (plugin.hasChangesForModelSpec()) {
                plugin.afterEmitSchema(writer);
            }
        }
    }

    /**
     * Calls {@link Plugin#emitConstructors(JavaFileWriter)} on all the bundled plugins
     */
    public void emitConstructors(JavaFileWriter writer) throws IOException {
        for (Plugin plugin : plugins) {
            if (plugin.hasChangesForModelSpec()) {
                plugin.emitConstructors(writer);
            }
        }
    }

    /**
     * Calls {@link Plugin#beforeEmitMethods(JavaFileWriter)} on all the bundled plugins
     */
    public void beforeEmitMethods(JavaFileWriter writer) throws IOException {
        for (Plugin plugin : plugins) {
            if (plugin.hasChangesForModelSpec()) {
                plugin.beforeEmitMethods(writer);
            }
        }
    }

    /**
     * Calls {@link Plugin#emitMethods(JavaFileWriter)} on all the bundled plugins
     */
    public void emitMethods(JavaFileWriter writer) throws IOException {
        for (Plugin plugin : plugins) {
            if (plugin.hasChangesForModelSpec()) {
                plugin.emitMethods(writer);
            }
        }
    }

    /**
     * Calls {@link Plugin#afterEmitMethods(JavaFileWriter)} on all the bundled plugins
     */
    public void afterEmitMethods(JavaFileWriter writer) throws IOException {
        for (Plugin plugin : plugins) {
            if (plugin.hasChangesForModelSpec()) {
                plugin.afterEmitMethods(writer);
            }
        }
    }

    /**
     * Calls {@link Plugin#emitAdditionalJava(JavaFileWriter)} on all the bundled plugins
     */
    public void emitAdditionalJava(JavaFileWriter writer) throws IOException {
        for (Plugin plugin : plugins) {
            if (plugin.hasChangesForModelSpec()) {
                plugin.emitAdditionalJava(writer);
            }
        }
    }
}
