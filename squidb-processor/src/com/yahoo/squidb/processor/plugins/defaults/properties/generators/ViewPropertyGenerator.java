/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.plugins.defaults.properties.generators;

import com.yahoo.aptutils.model.DeclaredTypeName;
import com.yahoo.aptutils.utils.AptUtils;
import com.yahoo.squidb.processor.TypeConstants;
import com.yahoo.squidb.processor.data.ModelSpec;

import javax.lang.model.element.VariableElement;

/**
 * An implementation of {@link PropertyGenerator} for handling properties in view models
 */
public class ViewPropertyGenerator extends BasicPropertyGenerator {

    private final DeclaredTypeName propertyType;
    private final DeclaredTypeName getAndSetType;

    public ViewPropertyGenerator(ModelSpec<?> modelSpec, VariableElement field, DeclaredTypeName propertyType,
            AptUtils utils) {
        super(modelSpec, field, utils);
        this.propertyType = propertyType;
        this.getAndSetType = extractGetAndSetType();
    }

    private DeclaredTypeName extractGetAndSetType() {
        String basicType = propertyType.toString().replace(TypeConstants.PROPERTY.toString(), "")
                .replace("Property", "")
                .replace(".", "");
        if ("Blob".equals(basicType)) {
            return TypeConstants.BYTE_ARRAY;
        } else {
            return new DeclaredTypeName("java.lang", basicType);
        }
    }

    @Override
    public DeclaredTypeName getTypeForAccessors() {
        return getAndSetType;
    }

    @Override
    public DeclaredTypeName getPropertyType() {
        return propertyType;
    }

}
