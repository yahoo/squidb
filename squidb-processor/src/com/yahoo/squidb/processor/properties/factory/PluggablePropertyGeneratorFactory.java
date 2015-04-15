/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.properties.factory;

import com.yahoo.aptutils.model.DeclaredTypeName;
import com.yahoo.aptutils.utils.AptUtils;
import com.yahoo.squidb.processor.properties.generators.PropertyGenerator;

import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;

public abstract class PluggablePropertyGeneratorFactory {

    protected final AptUtils utils;

    public PluggablePropertyGeneratorFactory(AptUtils utils) {
        this.utils = utils;
    }

    public abstract boolean canHandleElement(VariableElement e, DeclaredTypeName elementType,
            TypeElement parentElement);

    public abstract PropertyGenerator getPropertyGenerator(VariableElement e, DeclaredTypeName elementType,
            DeclaredTypeName modelClass);

}
