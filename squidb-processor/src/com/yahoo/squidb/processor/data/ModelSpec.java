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
import com.yahoo.squidb.processor.plugins.PluginManager;
import com.yahoo.squidb.processor.plugins.properties.generators.PropertyGenerator;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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
 * <li>A list of {@link VariableElement}s representing constant fields to be copied
 * (see {@link #getConstantElements()})</li>
 * <li>A list of {@link PropertyGenerator}s for the generated model's fields
 * (see {@link #getPropertyGenerators()})</li>
 * <li>A list of {@link PropertyGenerator}s for the generated model's deprecated fields
 * (see {@link #getDeprecatedPropertyGenerators()})</li>
 * </ul>
 */
public abstract class ModelSpec<T extends Annotation> {

    protected final T modelSpecAnnotation;
    protected final DeclaredTypeName generatedClassName;
    protected final DeclaredTypeName modelSpecName;
    protected final TypeElement modelSpecElement;

    private final List<VariableElement> constantElements = new ArrayList<VariableElement>();
    private final List<PropertyGenerator> propertyGenerators = new ArrayList<PropertyGenerator>();
    private final List<PropertyGenerator> deprecatedPropertyGenerators = new ArrayList<PropertyGenerator>();
    private final Map<String, Object> metadataMap = new HashMap<String, Object>();

    protected final AptUtils utils;
    protected final PluginBundle pluginBundle;

    public ModelSpec(TypeElement modelSpecElement, Class<T> modelSpecClass,
            PluginManager pluginManager, AptUtils utils) {
        this.utils = utils;
        this.modelSpecElement = modelSpecElement;
        this.modelSpecName = new DeclaredTypeName(modelSpecElement.getQualifiedName().toString());
        this.modelSpecAnnotation = modelSpecElement.getAnnotation(modelSpecClass);
        this.generatedClassName = new DeclaredTypeName(modelSpecName.getPackageName(), getGeneratedClassNameString());
        this.pluginBundle = pluginManager.getPluginBundleForModelSpec(this);
        processVariableElements();
    }

    private void processVariableElements() {
        List<? extends Element> enclosedElements = modelSpecElement.getEnclosedElements();
        for (Element e : enclosedElements) {
            if (e instanceof VariableElement && e.getAnnotation(Ignore.class) == null) {
                TypeName typeName = utils.getTypeNameFromTypeMirror(e.asType());
                if (!(typeName instanceof DeclaredTypeName)) {
                    utils.getMessager().printMessage(Diagnostic.Kind.WARNING,
                            "Element type " + typeName + " is not a concrete type, will be ignored", e);
                } else if (!pluginBundle.processVariableElement((VariableElement) e, (DeclaredTypeName) typeName)) {
                        utils.getMessager().printMessage(Diagnostic.Kind.WARNING,
                                "No plugin found to handle field", e);
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
     * @return a set of imports needed to include in the generated model
     */
    public final Set<DeclaredTypeName> getRequiredImports() {
        Set<DeclaredTypeName> imports = new HashSet<DeclaredTypeName>();
        imports.add(TypeConstants.PROPERTY); // For PROPERTIES array
        imports.add(TypeConstants.ABSTRACT_MODEL); // For CREATOR
        imports.add(getModelSuperclass());
        for (PropertyGenerator generator : propertyGenerators) {
            generator.registerRequiredImports(imports);
        }
        addModelSpecificImports(imports);
        utils.accumulateImportsFromElements(imports, constantElements);
        return imports;
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
     * @return a list of constant elements to be copied to the generated model
     */
    public List<VariableElement> getConstantElements() {
        return constantElements;
    }

    /**
     * Add a constant element to be copied to the generated model
     */
    public void addConstantElement(VariableElement e) {
        constantElements.add(e);
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

    public void attachMetadata(String metadataKey, Object metadata) {
        metadataMap.put(metadataKey, metadata);
    }

    public boolean hasMetadata(String metadataKey) {
        return metadataMap.containsKey(metadataKey);
    }

    @SuppressWarnings("unchecked")
    public <TYPE> TYPE getAttachedMetadata(String metadataKey) {
        return (TYPE) metadataMap.get(metadataKey);
    }
}
