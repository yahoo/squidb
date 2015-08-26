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
import com.yahoo.squidb.processor.plugins.PluginContext;
import com.yahoo.squidb.processor.plugins.properties.generators.PropertyGenerator;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.tools.Diagnostic;

/**
 * Base class for data representing a model spec. This class holds the following pieces of information common to all
 * types of models (table models, view models, and inherited models):
 * <ul>
 *     <li>The model spec annotation itself (see {@link #getSpecAnnotation()})</li>
 *     <li>The {@link TypeElement} representing the model spec class (see {@link #getModelSpecElement()})</li>
 *     <li>The name of the TypeElement (see {@link #getModelSpecName()})</li>
 *     <li>The name of the class to be generated (see {@link #getGeneratedClassName()})</li>
 *     <li>A list of {@link VariableElement}s representing constant fields to be copied
 *          (see {@link #getConstantElements()})</li>
 *     <li>A list of {@link PropertyGenerator}s for the generated model's fields
 *          (see {@link #getPropertyGenerators()})</li>
 *     <li>A list of {@link PropertyGenerator}s for the generated model's deprecated fields
 *          (see {@link #getDeprecatedPropertyGenerators()})</li>
 * </ul>
 * @param <T>
 */
public abstract class ModelSpec<T extends Annotation> {

    protected final T modelSpecAnnotation;
    protected final DeclaredTypeName generatedClassName;
    protected final DeclaredTypeName modelSpecName;
    protected final TypeElement modelSpecElement;

    protected final List<VariableElement> constantElements = new ArrayList<VariableElement>();
    protected final List<PropertyGenerator> propertyGenerators = new ArrayList<PropertyGenerator>();
    protected final List<PropertyGenerator> deprecatedPropertyGenerators = new ArrayList<PropertyGenerator>();

    protected final AptUtils utils;
    protected final PluginContext pluginContext;

    public ModelSpec(TypeElement modelSpecElement, Class<T> modelSpecClass,
            PluginContext pluginContext, AptUtils utils) {
        this.pluginContext = pluginContext;
        this.utils = utils;
        this.modelSpecElement = modelSpecElement;
        this.modelSpecName = new DeclaredTypeName(modelSpecElement.getQualifiedName().toString());
        this.modelSpecAnnotation = modelSpecElement.getAnnotation(modelSpecClass);
        this.generatedClassName = new DeclaredTypeName(modelSpecName.getPackageName(), getGeneratedClassNameString());
        accumulatePropertyGenerators();
    }

    private void accumulatePropertyGenerators() {
        List<? extends Element> enclosedElements = modelSpecElement.getEnclosedElements();
        for (Element e : enclosedElements) {
            if (e instanceof VariableElement && e.getAnnotation(Ignore.class) == null) {
                TypeName typeName = utils.getTypeNameFromTypeMirror(e.asType());
                if (!(typeName instanceof DeclaredTypeName)) {
                    utils.getMessager().printMessage(Diagnostic.Kind.WARNING,
                            "Element type " + typeName + " is not a concrete type, will be ignored", e);
                } else {
                    processVariableElement((VariableElement) e, (DeclaredTypeName) typeName);
                }
            }
        }
    }

    protected abstract void processVariableElement(VariableElement e, DeclaredTypeName elementType);

    protected abstract String getGeneratedClassNameString();

    /**
     * @return the name of the superclass for the generated model
     */
    public abstract DeclaredTypeName getModelSuperclass();

    protected void initializePropertyGenerator(VariableElement e) {
        PropertyGenerator generator = propertyGeneratorForElement(e);
        if (generator != null) {
            if (generator.isDeprecated()) {
                deprecatedPropertyGenerators.add(generator);
            } else {
                propertyGenerators.add(generator);
            }
        } else {
            utils.getMessager()
                    .printMessage(Diagnostic.Kind.WARNING, "No PropertyGenerator found to handle this modelSpecElement", e);
        }
    }

    protected PropertyGenerator propertyGeneratorForElement(VariableElement e) {
        return pluginContext.getPropertyGeneratorForVariableElement(this, e);
    }

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
     * @return a list of {@link PropertyGenerator}s for the fields in the generated model
     */
    public List<PropertyGenerator> getPropertyGenerators() {
        return propertyGenerators;
    }

    /**
     * @return a list of {@link PropertyGenerator}s for deprecated fields in the generated model
     */
    public List<PropertyGenerator> getDeprecatedPropertyGenerators() {
        return deprecatedPropertyGenerators;
    }
}
