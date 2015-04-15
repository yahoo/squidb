/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.properties.generators;

import com.yahoo.aptutils.model.DeclaredTypeName;
import com.yahoo.aptutils.utils.AptUtils;
import com.yahoo.squidb.processor.TypeConstants;

import java.util.Arrays;
import java.util.List;

import javax.lang.model.element.VariableElement;

public class BasicBlobPropertyGenerator extends BasicPropertyGenerator {

    public static List<DeclaredTypeName> handledColumnTypes() {
        return Arrays.asList(TypeConstants.BYTE_ARRAY);
    }

    public BasicBlobPropertyGenerator(VariableElement element, DeclaredTypeName modelName, AptUtils utils) {
        super(element, modelName, utils);
    }

    @Override
    protected DeclaredTypeName getTypeForGetAndSet() {
        return TypeConstants.BYTE_ARRAY;
    }

    @Override
    public DeclaredTypeName getPropertyType() {
        return TypeConstants.BLOB_PROPERTY;
    }

}
