/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.properties.generators;

import com.yahoo.aptutils.model.CoreTypes;
import com.yahoo.aptutils.model.DeclaredTypeName;
import com.yahoo.aptutils.utils.AptUtils;
import com.yahoo.squidb.annotations.ColumnSpec;
import com.yahoo.squidb.processor.TypeConstants;

import java.util.Arrays;
import java.util.List;

import javax.lang.model.element.VariableElement;

public class BasicStringPropertyGenerator extends BasicPropertyGenerator {

    public static List<DeclaredTypeName> handledColumnTypes() {
        return Arrays.asList(CoreTypes.JAVA_STRING);
    }

    public BasicStringPropertyGenerator(VariableElement element, DeclaredTypeName modelName, AptUtils utils) {
        super(element, modelName, utils);
    }

    @Override
    protected DeclaredTypeName getTypeForGetAndSet() {
        return CoreTypes.JAVA_STRING;
    }

    @Override
    public DeclaredTypeName getPropertyType() {
        return TypeConstants.STRING_PROPERTY;
    }

    @Override
    protected String getColumnDefinitionDefaultValue() {
        String defaultValue = super.getColumnDefinitionDefaultValue();
        if (!ColumnSpec.DEFAULT_NONE.equals(defaultValue) && !"NULL".equals(defaultValue)) {
            return "'" + defaultValue + "'";
        }
        return defaultValue;
    }

    @Override
    protected String getContentValuesDefaultValue() {
        return "\"" + super.getContentValuesDefaultValue() + "\"";
    }

}
