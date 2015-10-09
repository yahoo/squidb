/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.data;

import com.yahoo.aptutils.model.DeclaredTypeName;
import com.yahoo.aptutils.model.TypeName;
import com.yahoo.aptutils.utils.AptUtils;
import com.yahoo.squidb.annotations.Ignore;
import com.yahoo.squidb.processor.TypeConstants;
import com.yahoo.squidb.processor.plugins.PluginBundle;
import com.yahoo.squidb.processor.plugins.PluginEnvironment;
import com.yahoo.squidb.processor.plugins.defaults.properties.generators.PropertyGenerator;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.tools.Diagnostic;

/**
 * Base class for data representing a model spec. This class holds the following pieces of information common to all
 * types of models (table models, view models, and inherited models):
 * <ul>
 * <li>The model spec annotation itself (see {@link #getSpecAnnotation()})</li>
 * <li>The {@link TypeElement} representing the model spec class (see {@link #getModelSpecElement()})</li>
 * <li>The name of the TypeElement (see {@link #getModelSpecName()})</li>
 * <li>The name of the class to be generated (see {@link #getGeneratedClassName()})</li>
 * <li>A list of {@link PropertyGenerator}s for the generated model's fields
 * (see {@link #getPropertyGenerators()})</li>
 * <li>A list of {@link PropertyGenerator}s for the generated model's deprecated fields
 * (see {@link #getDeprecatedPropertyGenerators()})</li>
 * </ul>
 * <p>
 * Plugins can also store arbitrary metadata in a model spec using {@link #putMetadata(String, Object)} and
 * {@link #getMetadata(String)}
 */
public abstract class ModelSpec<T extends Annotation> {

    protected final T modelSpecAnnotation;
    protected final DeclaredTypeName generatedClassName;
    protected final DeclaredTypeName modelSpecName;
    protected final TypeElement modelSpecElement;

    private final List<PropertyGenerator> propertyGenerators = new ArrayList<PropertyGenerator>();
    private final List<PropertyGenerator> deprecatedPropertyGenerators = new ArrayList<PropertyGenerator>();
    private final Map<String, Object> metadataMap = new HashMap<String, Object>();

    protected final AptUtils utils;
    protected final PluginBundle pluginBundle;
    protected final boolean iosModels;

    public ModelSpec(TypeElement modelSpecElement, Class<T> modelSpecClass,
            PluginEnvironment pluginEnv, AptUtils utils) {
        this.utils = utils;
        this.modelSpecElement = modelSpecElement;
        this.modelSpecName = new DeclaredTypeName(modelSpecElement.getQualifiedName().toString());
        this.modelSpecAnnotation = modelSpecElement.getAnnotation(modelSpecClass);
        this.generatedClassName = new DeclaredTypeName(modelSpecName.getPackageName(), getGeneratedClassNameString());
        this.pluginBundle = pluginEnv.getPluginBundleForModelSpec(this);
        this.iosModels = pluginEnv.hasOption(PluginEnvironment.OPTIONS_GENERATE_IOS_MODELS);

        processVariableElements();
        pluginBundle.afterProcessVariableElements();
    }

    private void processVariableElements() {
        for (Element e : modelSpecElement.getEnclosedElements()) {
            if (e instanceof VariableElement && e.getAnnotation(Ignore.class) == null) {
                TypeName typeName = utils.getTypeNameFromTypeMirror(e.asType());
                if (!(typeName instanceof DeclaredTypeName)) {
                    utils.getMessager().printMessage(Diagnostic.Kind.WARNING,
                            "Element type " + typeName + " is not a concrete type, will be ignored", e);
                } else if (!pluginBundle.processVariableElement((VariableElement) e, (DeclaredTypeName) typeName)) {
                    // Deprecated things are generally ignored by plugins, so don't warn about them
                    if (e.getAnnotation(Deprecated.class) == null) {
                        utils.getMessager().printMessage(Diagnostic.Kind.WARNING,
                                "No plugin found to handle field", e);
                    }
                }
            }
        }
    }

    protected abstract String getGeneratedClassNameString();

    /**
     * @return the name of the superclass for the generated model
     */
    public abstract DeclaredTypeName getModelSuperclass();

    /**
     * Adds imports required by this model spec to the given accumulator set
     *
     * @param imports accumulator set
     */
    public final void addRequiredImports(Set<DeclaredTypeName> imports) {
        imports.add(TypeConstants.PROPERTY); // For PROPERTIES array
        imports.add(TypeConstants.VALUES_STORAGE);
        if (iosModels) {
            imports.add(TypeConstants.MAP_VALUES_STORAGE);
        } else {
            imports.add(TypeConstants.CONTENT_VALUES_STORAGE);
            imports.add(TypeConstants.MODEL_CREATOR);
        }
        imports.add(getModelSuperclass());
        for (PropertyGenerator generator : propertyGenerators) {
            generator.registerRequiredImports(imports);
        }
        addModelSpecificImports(imports);
        pluginBundle.addRequiredImports(imports);
    }

    protected abstract void addModelSpecificImports(Set<DeclaredTypeName> imports);

    /**
     * @return a {@link PluginBundle} for this model spec
     */
    public PluginBundle getPluginBundle() {
        return pluginBundle;
    }

    /**
     * @return the name of the model spec class
     */
    public DeclaredTypeName getModelSpecName() {
        return modelSpecName;
    }

    /**
     * @return the name of the generated model class
     */
    public DeclaredTypeName getGeneratedClassName() {
        return generatedClassName;
    }

    /**
     * @return the {@link TypeElement} for the model spec class
     */
    public TypeElement getModelSpecElement() {
        return modelSpecElement;
    }

    /**
     * @return the model spec annotation (e.g. an instance of {@link com.yahoo.squidb.annotations.TableModelSpec})
     */
    public T getSpecAnnotation() {
        return modelSpecAnnotation;
    }

    /**
     * @return a list of {@link PropertyGenerator}s for the fields in the generated model
     */
    public List<PropertyGenerator> getPropertyGenerators() {
        return propertyGenerators;
    }

    /**
     * Add a {@link PropertyGenerator} to the model spec
     */
    public void addPropertyGenerator(PropertyGenerator propertyGenerator) {
        propertyGenerators.add(propertyGenerator);
    }

    /**
     * @return a list of {@link PropertyGenerator}s for deprecated fields in the generated model
     */
    public List<PropertyGenerator> getDeprecatedPropertyGenerators() {
        return deprecatedPropertyGenerators;
    }

    /**
     * Add a deprecated {@link PropertyGenerator} to the model spec
     */
    public void addDeprecatedPropertyGenerator(PropertyGenerator propertyGenerator) {
        deprecatedPropertyGenerators.add(propertyGenerator);
    }

    /**
     * Attach arbitrary metadata to this model spec objects. Plugins can store metadata and then retrieve it later with
     * {@link #getMetadata(String)}
     *
     * @param metadataKey key for storing/retrieving the metadata
     * @param metadata the metadata to store
     * @see #hasMetadata(String)
     * @see #getMetadata(String)
     */
    public void putMetadata(String metadataKey, Object metadata) {
        metadataMap.put(metadataKey, metadata);
    }

    /**
     * @param metadataKey the metadata key to look up
     * @return true if there is metadata stored for the given key, false otherwise
     * @see #putMetadata(String, Object)
     * @see #getMetadata(String)
     */
    public boolean hasMetadata(String metadataKey) {
        return metadataMap.containsKey(metadataKey);
    }

    /**
     * Retrieve metadata that was previously attached with {@link #putMetadata(String, Object)}
     *
     * @param metadataKey key for storing/retrieving metadata
     * @return the metadata object for the given key if one was found, null otherwise
     * @see #putMetadata(String, Object)
     * @see #hasMetadata(String)
     */
    @SuppressWarnings("unchecked")
    public <TYPE> TYPE getMetadata(String metadataKey) {
        return (TYPE) metadataMap.get(metadataKey);
    }
}
