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

import java.util.Collections;
import java.util.List;

import javax.lang.model.element.VariableElement;

/**
 * An implementation of {@link PropertyGenerator} for handling blob (byte[]) fields
 */
public class BasicBlobPropertyGenerator extends BasicPropertyGenerator {

    public static List<DeclaredTypeName> handledColumnTypes() {
        return Collections.singletonList(TypeConstants.BYTE_ARRAY);
    }

    public BasicBlobPropertyGenerator(ModelSpec<?> modelSpec, String columnName, AptUtils utils) {
        super(modelSpec, columnName, utils);
    }

    public BasicBlobPropertyGenerator(ModelSpec<?> modelSpec, String columnName,
            String propertyName, AptUtils utils) {
        super(modelSpec, columnName, propertyName, utils);
    }

    public BasicBlobPropertyGenerator(ModelSpec<?> modelSpec, VariableElement field, AptUtils utils) {
        super(modelSpec, field, utils);
    }

    @Override
    public DeclaredTypeName getTypeForAccessors() {
        return TypeConstants.BYTE_ARRAY;
    }

    @Override
    public DeclaredTypeName getPropertyType() {
        return TypeConstants.BLOB_PROPERTY;
    }

}
