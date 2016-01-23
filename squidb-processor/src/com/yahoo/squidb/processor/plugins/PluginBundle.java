/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.plugins;

import com.yahoo.aptutils.model.DeclaredTypeName;
import com.yahoo.aptutils.writer.JavaFileWriter;
import com.yahoo.squidb.processor.data.ModelSpec;

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
public class PluginBundle extends Plugin {

    private final List<? extends Plugin> plugins;

    public PluginBundle(ModelSpec<?> modelSpec, PluginEnvironment pluginEnv, List<? extends Plugin> plugins) {
        super(modelSpec, pluginEnv);
        this.plugins = plugins;
    }

    /**
     * Calls {@link Plugin#processVariableElement(VariableElement, DeclaredTypeName)} on all the bundled plugins until
     * one of them returns true to "claim" the field
     * @param field a {@link VariableElement} field in a model spec
     * @param fieldType the type name of the field
     * @return true if any of the bundled plugins claimed the field for processing, false otherwise
     */
    @Override
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
    @Override
    public void afterProcessVariableElements() {
        for (Plugin plugin : plugins) {
            if (plugin.hasChangesForModelSpec()) {
                plugin.afterProcessVariableElements();
            }
        }
    }

    /**
     * Calls {@link Plugin#addRequiredImports(Set)} on all the bundled plugins
     */
    @Override
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
    @Override
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
    @Override
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
    @Override
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
    @Override
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
    @Override
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
    @Override
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
    @Override
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
    @Override
    public void emitAdditionalJava(JavaFileWriter writer) throws IOException {
        for (Plugin plugin : plugins) {
            if (plugin.hasChangesForModelSpec()) {
                plugin.emitAdditionalJava(writer);
            }
        }
    }
}
