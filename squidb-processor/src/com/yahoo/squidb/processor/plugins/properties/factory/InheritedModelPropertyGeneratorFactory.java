/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.plugins.properties.factory;

import com.yahoo.aptutils.model.DeclaredTypeName;
import com.yahoo.aptutils.utils.AptUtils;
import com.yahoo.squidb.annotations.InheritedModelSpec;

import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;

public class InheritedModelPropertyGeneratorFactory extends FieldReferencePropertyGeneratorFactory {

    public InheritedModelPropertyGeneratorFactory(AptUtils utils) {
        super(utils);
    }

    @Override
    public boolean hasPropertyGeneratorForField(VariableElement field, DeclaredTypeName elementType,
            TypeElement modelSpecElement) {
        return super.hasPropertyGeneratorForField(field, elementType, modelSpecElement)
                && modelSpecElement.getAnnotation(InheritedModelSpec.class) != null;
    }

}
