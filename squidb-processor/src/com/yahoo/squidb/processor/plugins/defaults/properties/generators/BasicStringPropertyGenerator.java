/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.plugins.defaults.properties.generators;

import com.yahoo.aptutils.model.CoreTypes;
import com.yahoo.aptutils.model.DeclaredTypeName;
import com.yahoo.aptutils.utils.AptUtils;
import com.yahoo.squidb.annotations.ColumnSpec;
import com.yahoo.squidb.processor.TypeConstants;
import com.yahoo.squidb.processor.data.ModelSpec;

import java.util.Collections;
import java.util.List;

import javax.lang.model.element.VariableElement;

/**
 * An implementation of {@link PropertyGenerator} for handling String fields
 */
public class BasicStringPropertyGenerator extends BasicPropertyGenerator {

    public static List<DeclaredTypeName> handledColumnTypes() {
        return Collections.singletonList(CoreTypes.JAVA_STRING);
    }

    public BasicStringPropertyGenerator(ModelSpec<?> modelSpec, String columnName, AptUtils utils) {
        super(modelSpec, columnName, utils);
    }

    public BasicStringPropertyGenerator(ModelSpec<?> modelSpec, String columnName,
            String propertyName, AptUtils utils) {
        super(modelSpec, columnName, propertyName, utils);
    }

    public BasicStringPropertyGenerator(ModelSpec<?> modelSpec, VariableElement field, AptUtils utils) {
        super(modelSpec, field, utils);
    }

    @Override
    public DeclaredTypeName getTypeForAccessors() {
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
