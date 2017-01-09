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

import java.util.Set;

import javax.lang.model.element.VariableElement;

/**
 * A stubbed implementation of {@link Plugin}. User plugins can extend this base class and override only the hooks that
 * they need. By default, the {@link Plugin#init(ModelSpec, PluginEnvironment)} implementation in this class returns
 * true. Plugins extending this class that do not want to accept all model specs should override the init() method
 * to perform additional checking (being sure to call super.init()):
 * <pre>
 * // Example implementation for a plugin that only processes TableModelSpecs
 * &#064;Override
 * public boolean init(ModelSpec&lt;?, ?&gt; modelSpec, PluginEnvironment pluginEnv) {
 *     super.init(modelSpec, pluginEnv);
 *     if (!(modelSpec instanceof TableModelSpecWrapper)) {
 *         return false;
 *     }
 *     this.tableModelSpec = (TableModelSpecWrapper)modelSpec;
 *     return true;
 * }
 * </pre>
 * This implementation also provides protected references to the ModelSpec and PluginEnvironment with which the
 * plugin was initialized. Note that if accessed before init() is called (e.g. in a constructor) or if super.init()
 * was not called, these instances may be null. When in doubt, use {@link #isInitialized()} to check if these fields
 * are safe to use.
 */
public abstract class AbstractPlugin implements Plugin {

    protected ModelSpec<?, ?> modelSpec;
    protected PluginEnvironment pluginEnv;

    @Override
    public boolean init(ModelSpec<?, ?> modelSpec, PluginEnvironment pluginEnv) {
        this.modelSpec = modelSpec;
        this.pluginEnv = pluginEnv;
        return true;
    }

    /**
     * @return true if this plugin instance has been initialized, false otherwise
     */
    protected boolean isInitialized() {
        return modelSpec != null;
    }

    /**
     * Stub of {@link Plugin#processVariableElement(VariableElement, TypeName)} for subclasses to override.
     * Returns false by default.
     * <p>
     * {@inheritDoc}
     */
    @Override
    public boolean processVariableElement(VariableElement field, TypeName fieldType) {
        return false;
    }

    /**
     * Stub of {@link Plugin#afterProcessVariableElements()} for subclasses to override.
     * <p>
     * {@inheritDoc}
     */
    @Override
    public void afterProcessVariableElements() {
    }

    /**
     * Stub of {@link Plugin#getModelSuperclass()} for subclasses to override. Returns null by default.
     * <p>
     * {@inheritDoc}
     */
    @Override
    public TypeName getModelSuperclass() {
        return null;
    }

    /**
     * Stub of {@link Plugin#addInterfacesToImplement(Set)} for subclasses to override
     * <p>
     * {@inheritDoc}
     */
    @Override
    public void addInterfacesToImplement(Set<TypeName> interfaces) {
    }

    /**
     * Stub of {@link Plugin#beforeBeginClassDeclaration(TypeSpec.Builder)} for subclasses to override
     * <p>
     * {@inheritDoc}
     */
    @Override
    public void beforeBeginClassDeclaration(TypeSpec.Builder builder) {
    }

    /**
     * Stub of {@link Plugin#beforeDeclareProperty(TypeSpec.Builder, PropertyGenerator, FieldSpec.Builder)}
     * for subclasses to override
     * <p>
     * {@inheritDoc}
     */
    @Override
    public void beforeDeclareProperty(TypeSpec.Builder builder,
            PropertyGenerator propertyGenerator, FieldSpec.Builder propertyDeclaration) {
    }

    /**
     * Stub of {@link Plugin#afterDeclareProperty(TypeSpec.Builder, PropertyGenerator, FieldSpec)} for subclasses to
     * override
     * <p>
     * {@inheritDoc}
     */
    @Override
    public void afterDeclareProperty(TypeSpec.Builder builder,
            PropertyGenerator propertyGenerator, FieldSpec propertyDeclaration) {
    }

    /**
     * Stub of {@link Plugin#beforeDeclareSchema(TypeSpec.Builder)} for subclasses to override
     * <p>
     * {@inheritDoc}
     */
    @Override
    public void beforeDeclareSchema(TypeSpec.Builder builder) {
    }

    /**
     * Stub of {@link Plugin#afterDeclareSchema(TypeSpec.Builder)} for subclasses to override
     * <p>
     * {@inheritDoc}
     */
    @Override
    public void afterDeclareSchema(TypeSpec.Builder builder) {
    }

    /**
     * Stub of {@link Plugin#beforeDeclareGetter(TypeSpec.Builder, PropertyGenerator, MethodSpec.Builder)} for subclasses
     * to override
     * <p>
     * {@inheritDoc}
     */
    @Override
    public void beforeDeclareGetter(TypeSpec.Builder builder, PropertyGenerator propertyGenerator,
            MethodSpec.Builder getterParams) {
    }

    /**
     * Stub of {@link Plugin#afterDeclareGetter(TypeSpec.Builder, PropertyGenerator, MethodSpec)} for subclasses
     * to override
     * <p>
     * {@inheritDoc}
     */
    @Override
    public void afterDeclareGetter(TypeSpec.Builder builder, PropertyGenerator propertyGenerator,
            MethodSpec getterParams) {
    }

    /**
     * Stub of {@link Plugin#beforeDeclareSetter(TypeSpec.Builder, PropertyGenerator, MethodSpec.Builder)} for subclasses
     * to override
     * <p>
     * {@inheritDoc}
     */
    @Override
    public void beforeDeclareSetter(TypeSpec.Builder builder, PropertyGenerator propertyGenerator,
            MethodSpec.Builder setterParams) {
    }

    /**
     * Stub of {@link Plugin#afterDeclareSetter(TypeSpec.Builder, PropertyGenerator, MethodSpec)} for subclasses to
     * override
     * <p>
     * {@inheritDoc}
     */
    @Override
    public void afterDeclareSetter(TypeSpec.Builder builder, PropertyGenerator propertyGenerator,
            MethodSpec setterParams) {
    }

    /**
     * Stub of {@link Plugin#declareMethodsOrConstructors(TypeSpec.Builder)} for subclasses to override
     * <p>
     * {@inheritDoc}
     */
    @Override
    public void declareMethodsOrConstructors(TypeSpec.Builder builder) {
    }

    /**
     * Stub of {@link Plugin#declareAdditionalJava(TypeSpec.Builder)} for subclasses to override
     * <p>
     * {@inheritDoc}
     */
    @Override
    public void declareAdditionalJava(TypeSpec.Builder builder) {
    }

}
