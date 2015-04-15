/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.properties.generators;

import com.yahoo.aptutils.model.CoreTypes;
import com.yahoo.aptutils.model.DeclaredTypeName;
import com.yahoo.aptutils.utils.AptUtils;
import com.yahoo.squidb.processor.TypeConstants;

import java.util.Arrays;
import java.util.List;

import javax.lang.model.element.VariableElement;

public class BasicIntegerPropertyGenerator extends BasicPropertyGenerator {

    public static List<DeclaredTypeName> handledColumnTypes() {
        return Arrays.asList(CoreTypes.JAVA_BYTE, CoreTypes.JAVA_SHORT, CoreTypes.JAVA_INTEGER,
                CoreTypes.PRIMITIVE_BYTE, CoreTypes.PRIMITIVE_SHORT, CoreTypes.PRIMITIVE_INT);
    }

    public BasicIntegerPropertyGenerator(VariableElement element, DeclaredTypeName modelName, AptUtils utils) {
        super(element, modelName, utils);
    }

    @Override
    protected DeclaredTypeName getTypeForGetAndSet() {
        return CoreTypes.JAVA_INTEGER;
    }

    @Override
    public DeclaredTypeName getPropertyType() {
        return TypeConstants.INTEGER_PROPERTY;
    }

}
