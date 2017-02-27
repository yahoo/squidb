/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.plugins.defaults.properties;

import com.squareup.javapoet.TypeName;
import com.yahoo.squidb.processor.TypeConstants;
import com.yahoo.squidb.processor.data.ModelSpec;
import com.yahoo.squidb.processor.plugins.defaults.properties.generators.interfaces.PropertyGenerator;

import javax.lang.model.element.VariableElement;

/**
 * Abstract class for plugins that handle fields that are constant references to Property objects in other models
 */
public abstract class PropertyReferencePlugin<T extends ModelSpec<?, P>, P extends PropertyGenerator>
        extends BaseFieldPlugin<T, P> {

    @Override
    protected boolean hasPropertyGeneratorForField(VariableElement field, TypeName fieldType) {
        return field.getAnnotation(Deprecated.class) == null
                && TypeConstants.isVisibleConstant(field)
                && isSupportedPropertyType(fieldType);
    }

    protected boolean isSupportedPropertyType(TypeName fieldType) {
        return TypeConstants.isBasicPropertyType(fieldType);
    }

    @Override
    public boolean processVariableElement(VariableElement field, TypeName fieldType) {
        if (field.getAnnotation(Deprecated.class) != null) {
            return false;
        }
        return super.processVariableElement(field, fieldType);
    }
}
