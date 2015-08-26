/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.plugins.properties.factory;

import com.yahoo.aptutils.model.DeclaredTypeName;
import com.yahoo.aptutils.utils.AptUtils;
import com.yahoo.squidb.processor.TypeConstants;
import com.yahoo.squidb.processor.data.ModelSpec;
import com.yahoo.squidb.processor.plugins.Plugin;
import com.yahoo.squidb.processor.plugins.properties.generators.PropertyGenerator;
import com.yahoo.squidb.processor.plugins.properties.generators.ViewPropertyGenerator;

import javax.lang.model.element.VariableElement;

abstract class FieldReferencePropertyGeneratorFactory extends Plugin {

    public FieldReferencePropertyGeneratorFactory(ModelSpec<?> modelSpec, AptUtils utils) {
        super(modelSpec, utils);
    }

    @Override
    public boolean hasPropertyGeneratorForField(VariableElement field, DeclaredTypeName fieldType) {
        return TypeConstants.isPropertyType(fieldType);
    }

    @Override
    public PropertyGenerator getPropertyGenerator(VariableElement field, DeclaredTypeName fieldType) {
        return new ViewPropertyGenerator(modelSpec, field, fieldType, utils);
    }

}
