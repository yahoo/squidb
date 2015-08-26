/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.plugins.properties.factory;

import com.yahoo.aptutils.model.DeclaredTypeName;
import com.yahoo.aptutils.utils.AptUtils;
import com.yahoo.squidb.annotations.ViewModelSpec;
import com.yahoo.squidb.processor.data.ModelSpec;
import com.yahoo.squidb.processor.plugins.properties.generators.PropertyGenerator;

import javax.lang.model.element.VariableElement;

/**
 * This plugin controls generating property declarations, getters, and setters for fields in a view model. It can
 * create instances of {@link PropertyGenerator} for references to other Property subclasses (StringProperty,
 * LongProperty, etc.)
 */
public class ViewPropertyGeneratorFactory extends FieldReferencePropertyGeneratorFactory {

    public ViewPropertyGeneratorFactory(ModelSpec<?> modelSpec, AptUtils utils) {
        super(modelSpec, utils);
    }

    @Override
    public boolean hasPropertyGeneratorForField(VariableElement field, DeclaredTypeName fieldType) {
        return super.hasPropertyGeneratorForField(field, fieldType)
                && modelSpec.getModelSpecElement().getAnnotation(ViewModelSpec.class) != null;
    }

}
