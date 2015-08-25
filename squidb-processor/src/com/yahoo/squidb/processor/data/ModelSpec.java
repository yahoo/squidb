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

public abstract class ModelSpec<T extends Annotation> {

    protected final T modelSpecAnnotation;
    protected DeclaredTypeName generatedClassName;
    protected DeclaredTypeName modelSpecName;
    protected TypeElement modelSpecElement;

    protected List<VariableElement> constantElements = new ArrayList<VariableElement>();
    protected List<PropertyGenerator> propertyGenerators = new ArrayList<PropertyGenerator>();
    protected List<PropertyGenerator> deprecatedPropertyGenerators = new ArrayList<PropertyGenerator>();

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
                } else {
                    processVariableElement((VariableElement) e, (DeclaredTypeName) typeName);
                }
            }
        }
    }

    protected abstract void processVariableElement(VariableElement e, DeclaredTypeName elementType);

    protected abstract String getGeneratedClassNameString();

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

    public Set<DeclaredTypeName> getRequiredImports() {
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

    public DeclaredTypeName getModelSpecName() {
        return modelSpecName;
    }

    public DeclaredTypeName getGeneratedClassName() {
        return generatedClassName;
    }

    public TypeElement getModelSpecElement() {
        return modelSpecElement;
    }

    public T getSpecAnnotation() {
        return modelSpecAnnotation;
    }

    public List<VariableElement> getConstantElements() {
        return constantElements;
    }

    public List<PropertyGenerator> getPropertyGenerators() {
        return propertyGenerators;
    }

    public List<PropertyGenerator> getDeprecatedPropertyGenerators() {
        return deprecatedPropertyGenerators;
    }
}
