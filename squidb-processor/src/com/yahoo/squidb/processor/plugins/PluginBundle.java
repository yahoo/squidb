/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.plugins;

import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.yahoo.squidb.processor.data.ModelSpec;
import com.yahoo.squidb.processor.plugins.defaults.properties.generators.interfaces.PropertyGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.VariableElement;

/**
 * This class wraps a list of {@link Plugin}s. It provides a convenient way of iterating over all the plugins
 * present during code generation and calling them during the generation of a model class for a particular model
 * spec.
 */
public class PluginBundle implements Plugin {

    private final List<Class<? extends Plugin>> pluginClasses = new ArrayList<>();
    private final List<Plugin> plugins = new ArrayList<>();

    private boolean isInitialized = false;

    public PluginBundle() {
        // No-arg constructor to satisfy Plugin contract
    }

    public PluginBundle(List<Class<? extends Plugin>> pluginClasses) {
        this();
        addPluginClasses(pluginClasses);
    }

    /**
     * @return true if this PluginBundle has been initialized, false otherwise
     */
    public boolean isInitialized() {
        return isInitialized;
    }

    /**
     * Add plugin classes to be instantiated when this plugin is initialized. Plugin classes cannot be added to the
     * bundle after it is initialized; an IllegalStateException will be thrown if this plugin bundle was already
     * initialized.
     *
     * @see #isInitialized()
     */
    public void addPluginClasses(List<Class<? extends Plugin>> pluginClasses) {
        if (isInitialized) {
            throw new IllegalStateException("Cannot call addPluginClasses() on a PluginBundle that is "
                    + "already initialized");
        }
        if (pluginClasses != null) {
            this.pluginClasses.addAll(pluginClasses);
        }
    }

    @Override
    public final boolean init(ModelSpec<?, ?> modelSpec, PluginEnvironment pluginEnv) {
        if (isInitialized) {
            throw new IllegalStateException("Cannot call init() on a PluginBundle more than once");
        }
        for (Class<? extends Plugin> pluginClass : pluginClasses) {
            Plugin instance = PluginEnvironment.createAndInitializePlugin(pluginClass, modelSpec, pluginEnv);
            if (instance != null) {
                this.plugins.add(instance);
            }
        }
        isInitialized = true;
        return true;
    }

    private void checkInitialized() {
        if (!isInitialized) {
            throw new IllegalStateException("PluginBundle was not initialized");
        }
    }

    /**
     * Calls {@link Plugin#processVariableElement(VariableElement, TypeName)} on all the bundled plugins until
     * one of them returns true to "claim" the field
     *
     * @param field a {@link VariableElement} field in a model spec
     * @param fieldType the type name of the field
     * @return true if any of the bundled plugins claimed the field for processing, false otherwise
     */
    @Override
    public boolean processVariableElement(VariableElement field, TypeName fieldType) {
        checkInitialized();
        for (Plugin plugin : plugins) {
            if (plugin.processVariableElement(field, fieldType)) {
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
        checkInitialized();
        for (Plugin plugin : plugins) {
            plugin.afterProcessVariableElements();
        }
    }

    /**
     * Calls {@link Plugin#getModelSuperclass()} on all the bundled plugins, returning the first non-null value returned
     * by any plugin
     *
     * @return the name of a class to use as the model superclass, or null if no plugin provided one
     */
    @Override
    public TypeName getModelSuperclass() {
        checkInitialized();
        for (Plugin plugin : plugins) {
            TypeName modelSuperclass = plugin.getModelSuperclass();
            if (modelSuperclass != null) {
                return modelSuperclass;
            }
        }
        return null;
    }

    /**
     * Calls {@link Plugin#addInterfacesToImplement(Set)} on all the bundled plugins
     */
    @Override
    public void addInterfacesToImplement(Set<TypeName> interfaces) {
        checkInitialized();
        for (Plugin plugin : plugins) {
            plugin.addInterfacesToImplement(interfaces);
        }
    }

    /**
     * Calls {@link Plugin#beforeBeginClassDeclaration(TypeSpec.Builder)} on all the bundled plugins
     */
    @Override
    public void beforeBeginClassDeclaration(TypeSpec.Builder builder) {
        checkInitialized();
        for (Plugin plugin : plugins) {
            plugin.beforeBeginClassDeclaration(builder);
        }
    }

    /**
     * Calls {@link Plugin#beforeDeclareSchema(TypeSpec.Builder)} on all the bundled plugins
     */
    @Override
    public void beforeDeclareSchema(TypeSpec.Builder builder) {
        checkInitialized();
        for (Plugin plugin : plugins) {
            plugin.beforeDeclareSchema(builder);
        }
    }

    /**
     * Calls {@link Plugin#willDeclareProperty(TypeSpec.Builder, PropertyGenerator, FieldSpec.Builder)} on
     * all the bundled plugins
     */
    @Override
    public void willDeclareProperty(TypeSpec.Builder builder,
            PropertyGenerator propertyGenerator, FieldSpec.Builder propertyDeclaration) {
        checkInitialized();
        for (Plugin plugin : plugins) {
            plugin.willDeclareProperty(builder, propertyGenerator, propertyDeclaration);
        }
    }

    /**
     * Calls {@link Plugin#didDeclareProperty(TypeSpec.Builder, PropertyGenerator, FieldSpec)} on
     * all the bundled plugins
     */
    @Override
    public void didDeclareProperty(TypeSpec.Builder builder, PropertyGenerator propertyGenerator,
            FieldSpec propertyDeclaration) {
        checkInitialized();
        for (Plugin plugin : plugins) {
            plugin.didDeclareProperty(builder, propertyGenerator, propertyDeclaration);
        }
    }

    /**
     * Calls {@link Plugin#afterDeclareSchema(TypeSpec.Builder)} on all the bundled plugins
     */
    @Override
    public void afterDeclareSchema(TypeSpec.Builder builder) {
        checkInitialized();
        for (Plugin plugin : plugins) {
            plugin.afterDeclareSchema(builder);
        }
    }

    /**
     * Calls {@link Plugin#declareMethodsOrConstructors(TypeSpec.Builder)} on all the bundled plugins
     */
    @Override
    public void declareMethodsOrConstructors(TypeSpec.Builder builder) {
        checkInitialized();
        for (Plugin plugin : plugins) {
            plugin.declareMethodsOrConstructors(builder);
        }
    }

    /**
     * Calls {@link Plugin#willDeclareGetter(TypeSpec.Builder, PropertyGenerator, MethodSpec.Builder)}
     * on all the bundled plugins
     */
    @Override
    public void willDeclareGetter(TypeSpec.Builder builder, PropertyGenerator propertyGenerator,
            MethodSpec.Builder getterParams) {
        checkInitialized();
        for (Plugin plugin : plugins) {
            plugin.willDeclareGetter(builder, propertyGenerator, getterParams);
        }
    }

    /**
     * Calls {@link Plugin#didDeclareGetter(TypeSpec.Builder, PropertyGenerator, MethodSpec)}
     * on all the bundled plugins
     */
    @Override
    public void didDeclareGetter(TypeSpec.Builder builder, PropertyGenerator propertyGenerator,
            MethodSpec getterParams) {
        checkInitialized();
        for (Plugin plugin : plugins) {
            plugin.didDeclareGetter(builder, propertyGenerator, getterParams);
        }
    }

    /**
     * Calls {@link Plugin#willDeclareSetter(TypeSpec.Builder, PropertyGenerator, MethodSpec.Builder)}
     * on all the bundled plugins
     */
    @Override
    public void willDeclareSetter(TypeSpec.Builder builder, PropertyGenerator propertyGenerator,
            MethodSpec.Builder getterParams) {
        checkInitialized();
        for (Plugin plugin : plugins) {
            plugin.willDeclareSetter(builder, propertyGenerator, getterParams);
        }
    }

    /**
     * Calls {@link Plugin#didDeclareSetter(TypeSpec.Builder, PropertyGenerator, MethodSpec)}
     * on all the bundled plugins
     */
    @Override
    public void didDeclareSetter(TypeSpec.Builder builder, PropertyGenerator propertyGenerator, MethodSpec getterParams) {
        checkInitialized();
        for (Plugin plugin : plugins) {
            plugin.didDeclareSetter(builder, propertyGenerator, getterParams);
        }
    }

    /**
     * Calls {@link Plugin#declareAdditionalJava(TypeSpec.Builder)} on all the bundled plugins
     */
    @Override
    public void declareAdditionalJava(TypeSpec.Builder builder) {
        checkInitialized();
        for (Plugin plugin : plugins) {
            plugin.declareAdditionalJava(builder);
        }
    }
}
