/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.properties.factory;

import com.yahoo.aptutils.model.DeclaredTypeName;
import com.yahoo.aptutils.utils.AptUtils;
import com.yahoo.squidb.annotations.ViewModelSpec;

import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;

public class ViewPropertyGeneratorFactory extends FieldReferencePropertyGeneratorFactory {

    public ViewPropertyGeneratorFactory(AptUtils utils) {
        super(utils);
    }

    @Override
    public boolean canHandleElement(VariableElement element, DeclaredTypeName elementType, TypeElement parentElement) {
        return super.canHandleElement(element, elementType, parentElement)
                && parentElement.getAnnotation(ViewModelSpec.class) != null;
    }

}
