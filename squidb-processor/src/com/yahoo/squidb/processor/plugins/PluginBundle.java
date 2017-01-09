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

    public PluginBundle(ModelSpec<?, ?> modelSpec, PluginEnvironment pluginEnv, List<? extends Plugin> plugins) {
        super(modelSpec, pluginEnv);
        this.plugins = plugins;
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
     * Calls {@link Plugin#getModelSuperclass()} on all the bundled plugins, returning the first non-null value returned
     * by any plugin
     *
     * @return the name of a class to use as the model superclass, or null if no plugin provided one
     */
    @Override
    public TypeName getModelSuperclass() {
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
        for (Plugin plugin : plugins) {
            if (plugin.hasChangesForModelSpec()) {
                plugin.addInterfacesToImplement(interfaces);
            }
        }
    }

    /**
     * Calls {@link Plugin#beforeBeginClassDeclaration(TypeSpec.Builder)} on all the bundled plugins
     */
    @Override
    public void beforeBeginClassDeclaration(TypeSpec.Builder builder) {
        for (Plugin plugin : plugins) {
            if (plugin.hasChangesForModelSpec()) {
                plugin.beforeBeginClassDeclaration(builder);
            }
        }
    }

    /**
     * Calls {@link Plugin#beforeDeclareSchema(TypeSpec.Builder)} on all the bundled plugins
     */
    @Override
    public void beforeDeclareSchema(TypeSpec.Builder builder) {
        for (Plugin plugin : plugins) {
            if (plugin.hasChangesForModelSpec()) {
                plugin.beforeDeclareSchema(builder);
            }
        }
    }

    /**
     * Calls {@link Plugin#willDeclareProperty(TypeSpec.Builder, PropertyGenerator, FieldSpec.Builder)} on
     * all the bundled plugins
     */
    @Override
    public void willDeclareProperty(TypeSpec.Builder builder,
            PropertyGenerator propertyGenerator, FieldSpec.Builder propertyDeclaration) {
        for (Plugin plugin : plugins) {
            if (plugin.hasChangesForModelSpec()) {
                plugin.willDeclareProperty(builder, propertyGenerator, propertyDeclaration);
            }
        }
    }

    /**
     * Calls {@link Plugin#didDeclareProperty(TypeSpec.Builder, PropertyGenerator, FieldSpec)} on
     * all the bundled plugins
     */
    @Override
    public void didDeclareProperty(TypeSpec.Builder builder, PropertyGenerator propertyGenerator,
            FieldSpec propertyDeclaration) {
        for (Plugin plugin : plugins) {
            if (plugin.hasChangesForModelSpec()) {
                plugin.didDeclareProperty(builder, propertyGenerator, propertyDeclaration);
            }
        }
    }

    /**
     * Calls {@link Plugin#afterDeclareSchema(TypeSpec.Builder)} on all the bundled plugins
     */
    @Override
    public void afterDeclareSchema(TypeSpec.Builder builder) {
        for (Plugin plugin : plugins) {
            if (plugin.hasChangesForModelSpec()) {
                plugin.afterDeclareSchema(builder);
            }
        }
    }

    /**
     * Calls {@link Plugin#declareMethodsOrConstructors(TypeSpec.Builder)} on all the bundled plugins
     */
    @Override
    public void declareMethodsOrConstructors(TypeSpec.Builder builder) {
        for (Plugin plugin : plugins) {
            if (plugin.hasChangesForModelSpec()) {
                plugin.declareMethodsOrConstructors(builder);
            }
        }
    }

    /**
     * Calls {@link Plugin#willDeclareGetter(TypeSpec.Builder, PropertyGenerator, MethodSpec.Builder)}
     * on all the bundled plugins
     */
    @Override
    public void willDeclareGetter(TypeSpec.Builder builder, PropertyGenerator propertyGenerator,
            MethodSpec.Builder getterParams) {
        for (Plugin plugin : plugins) {
            if (plugin.hasChangesForModelSpec()) {
                plugin.willDeclareGetter(builder, propertyGenerator, getterParams);
            }
        }
    }

    /**
     * Calls {@link Plugin#didDeclareGetter(TypeSpec.Builder, PropertyGenerator, MethodSpec)}
     * on all the bundled plugins
     */
    @Override
    public void didDeclareGetter(TypeSpec.Builder builder, PropertyGenerator propertyGenerator,
            MethodSpec getterParams) {
        for (Plugin plugin : plugins) {
            if (plugin.hasChangesForModelSpec()) {
                plugin.didDeclareGetter(builder, propertyGenerator, getterParams);
            }
        }
    }

    /**
     * Calls {@link Plugin#willDeclareSetter(TypeSpec.Builder, PropertyGenerator, MethodSpec.Builder)}
     * on all the bundled plugins
     */
    @Override
    public void willDeclareSetter(TypeSpec.Builder builder, PropertyGenerator propertyGenerator,
            MethodSpec.Builder getterParams) {
        for (Plugin plugin : plugins) {
            if (plugin.hasChangesForModelSpec()) {
                plugin.willDeclareSetter(builder, propertyGenerator, getterParams);
            }
        }
    }

    /**
     * Calls {@link Plugin#didDeclareSetter(TypeSpec.Builder, PropertyGenerator, MethodSpec)}
     * on all the bundled plugins
     */
    @Override
    public void didDeclareSetter(TypeSpec.Builder builder, PropertyGenerator propertyGenerator, MethodSpec getterParams) {
        for (Plugin plugin : plugins) {
            if (plugin.hasChangesForModelSpec()) {
                plugin.didDeclareSetter(builder, propertyGenerator, getterParams);
            }
        }
    }

    /**
     * Calls {@link Plugin#declareAdditionalJava(TypeSpec.Builder)} on all the bundled plugins
     */
    @Override
    public void declareAdditionalJava(TypeSpec.Builder builder) {
        for (Plugin plugin : plugins) {
            if (plugin.hasChangesForModelSpec()) {
                plugin.declareAdditionalJava(builder);
            }
        }
    }
}
