/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.properties.factory;

import com.yahoo.aptutils.model.DeclaredTypeName;
import com.yahoo.aptutils.model.TypeName;
import com.yahoo.aptutils.utils.AptUtils;
import com.yahoo.squidb.processor.properties.generators.PropertyGenerator;

import java.util.ArrayList;
import java.util.List;

import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;

public class PropertyGeneratorFactory {

    private AptUtils utils;
    private List<PluggablePropertyGeneratorFactory> baseFactories = new ArrayList<PluggablePropertyGeneratorFactory>();
    private List<PluggablePropertyGeneratorFactory> userFactories = new ArrayList<PluggablePropertyGeneratorFactory>();

    public PropertyGeneratorFactory(AptUtils utils) {
        this.utils = utils;
        baseFactories.add(new TablePropertyGeneratorFactory(utils));
        baseFactories.add(new ViewPropertyGeneratorFactory(utils));
        baseFactories.add(new InheritedModelPropertyGeneratorFactory(utils));
    }

    public void registerPluggablePropertyGeneratorFactory(PluggablePropertyGeneratorFactory factory) {
        userFactories.add(factory);
    }

    public PropertyGenerator getPropertyGeneratorForVariableElement(VariableElement element, DeclaredTypeName modelType,
            TypeElement parentElement) {
        TypeName type = utils.getTypeNameFromTypeMirror(element.asType());
        PropertyGenerator generator = null;
        if (type instanceof DeclaredTypeName) {
            generator = searchFactoriesForPropertyGenerator(element, (DeclaredTypeName) type, modelType, parentElement);
        }
        return generator;
    }

    private PropertyGenerator searchFactoriesForPropertyGenerator(VariableElement element, DeclaredTypeName elementType,
            DeclaredTypeName modelType, TypeElement parentElement) {
        PropertyGenerator generator = searchFactoryListForPropertyGenerator(userFactories, element,
                elementType, modelType, parentElement);
        if (generator != null) {
            return generator;
        }
        return searchFactoryListForPropertyGenerator(baseFactories, element, elementType, modelType, parentElement);
    }

    private PropertyGenerator searchFactoryListForPropertyGenerator(List<PluggablePropertyGeneratorFactory> factories,
            VariableElement element, DeclaredTypeName elementType, DeclaredTypeName modelType,
            TypeElement parentElement) {
        for (PluggablePropertyGeneratorFactory factory : factories) {
            if (factory.canHandleElement(element, elementType, parentElement)) {
                PropertyGenerator generator = factory.getPropertyGenerator(element, elementType, modelType);
                if (generator != null) {
                    return generator;
                }
            }
        }
        return null;
    }

}
