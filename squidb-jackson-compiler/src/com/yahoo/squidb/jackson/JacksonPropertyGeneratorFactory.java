/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.jackson;

import com.yahoo.aptutils.model.DeclaredTypeName;
import com.yahoo.aptutils.utils.AptUtils;
import com.yahoo.squidb.processor.properties.factory.PluggablePropertyGeneratorFactory;
import com.yahoo.squidb.processor.properties.generators.PropertyGenerator;

import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;

public class JacksonPropertyGeneratorFactory extends PluggablePropertyGeneratorFactory {

    public JacksonPropertyGeneratorFactory(AptUtils utils) {
        super(utils);
    }

    @Override
    public boolean canHandleElement(VariableElement element, DeclaredTypeName elementType, TypeElement parentElement) {
        if (element.getAnnotation(JacksonProperty.class) == null) {
            return false;
        }
        if (elementType.equals(JacksonTypeConstants.MAP)) {
            return elementType.getTypeArgs().get(0) instanceof DeclaredTypeName &&
                    elementType.getTypeArgs().get(1) instanceof DeclaredTypeName;
        } else if (elementType.equals(JacksonTypeConstants.LIST)) {
            return elementType.getTypeArgs().get(0) instanceof DeclaredTypeName;
        }
        return false;
    }

    @Override
    public PropertyGenerator getPropertyGenerator(VariableElement element, DeclaredTypeName elementType,
            DeclaredTypeName modelClass) {
        if (elementType.equals(JacksonTypeConstants.MAP)) {
            return new JacksonMapPropertyGenerator(element, elementType, modelClass, utils);
        } else if (elementType.equals(JacksonTypeConstants.LIST)) {
            return new JacksonListPropertyGenerator(element, elementType, modelClass, utils);
        }
        return null;
    }

}
