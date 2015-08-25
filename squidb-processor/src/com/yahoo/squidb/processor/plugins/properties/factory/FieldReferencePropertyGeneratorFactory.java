/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.plugins.properties.factory;

import com.yahoo.aptutils.model.DeclaredTypeName;
import com.yahoo.aptutils.utils.AptUtils;
import com.yahoo.squidb.processor.TypeConstants;
import com.yahoo.squidb.processor.plugins.Plugin;
import com.yahoo.squidb.processor.plugins.properties.generators.PropertyGenerator;
import com.yahoo.squidb.processor.plugins.properties.generators.ViewPropertyGenerator;

import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.tools.Diagnostic.Kind;

abstract class FieldReferencePropertyGeneratorFactory extends Plugin {

    public FieldReferencePropertyGeneratorFactory(AptUtils utils) {
        super(utils);
    }

    @Override
    public boolean hasPropertyGeneratorForField(VariableElement field, DeclaredTypeName elementType,
            TypeElement modelSpecElement) {
        return TypeConstants.isPropertyType(elementType);
    }

    @Override
    public PropertyGenerator getPropertyGenerator(VariableElement element, DeclaredTypeName elementType,
            DeclaredTypeName modelClass) {
        Class<? extends ViewPropertyGenerator> generatorClass = getViewPropertyGenerator();
        try {
            return generatorClass.getConstructor(VariableElement.class, DeclaredTypeName.class, DeclaredTypeName.class,
                    AptUtils.class).newInstance(element, elementType, modelClass, utils);
        } catch (Exception e) {
            utils.getMessager().printMessage(Kind.ERROR,
                    "Exception instantiating PropertyGenerator: " + generatorClass + ", " + e);
        }
        return null;
    }

    protected Class<? extends ViewPropertyGenerator> getViewPropertyGenerator() {
        return ViewPropertyGenerator.class;
    }

}
