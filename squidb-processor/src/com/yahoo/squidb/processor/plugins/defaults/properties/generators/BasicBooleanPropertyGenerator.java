/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.plugins.defaults.properties.generators;

import com.yahoo.aptutils.model.CoreTypes;
import com.yahoo.aptutils.model.DeclaredTypeName;
import com.yahoo.aptutils.utils.AptUtils;
import com.yahoo.squidb.processor.StringUtils;
import com.yahoo.squidb.processor.TypeConstants;
import com.yahoo.squidb.processor.data.ModelSpec;

import java.util.Arrays;
import java.util.List;

import javax.lang.model.element.VariableElement;

/**
 * An implementation of {@link PropertyGenerator} for handling boolean fields
 */
public class BasicBooleanPropertyGenerator extends BasicPropertyGenerator {

    public static List<DeclaredTypeName> handledColumnTypes() {
        return Arrays.asList(CoreTypes.JAVA_BOOLEAN, CoreTypes.PRIMITIVE_BOOLEAN);
    }

    public BasicBooleanPropertyGenerator(ModelSpec<?> modelSpec, String columnName, AptUtils utils) {
        super(modelSpec, columnName, utils);
    }

    public BasicBooleanPropertyGenerator(ModelSpec<?> modelSpec, String columnName,
            String propertyName, AptUtils utils) {
        super(modelSpec, columnName, propertyName, utils);
    }

    public BasicBooleanPropertyGenerator(ModelSpec<?> modelSpec, VariableElement field, AptUtils utils) {
        super(modelSpec, field, utils);
    }

    @Override
    public DeclaredTypeName getTypeForAccessors() {
        return CoreTypes.JAVA_BOOLEAN;
    }

    @Override
    public DeclaredTypeName getPropertyType() {
        return TypeConstants.BOOLEAN_PROPERTY;
    }

    @Override
    protected String getColumnDefinitionDefaultValue() {
        String defaultValue = super.getColumnDefinitionDefaultValue();
        if ("true".equalsIgnoreCase(defaultValue)) {
            return "1";
        } else if ("false".equalsIgnoreCase(defaultValue)) {
            return "0";
        }
        return defaultValue;
    }

    @Override
    public String getterMethodName() {
        if (camelCasePropertyName.startsWith("is") || camelCasePropertyName.startsWith("has")) {
            return camelCasePropertyName;
        } else {
            return "is" + StringUtils.capitalize(camelCasePropertyName);
        }
    }

    @Override
    public String setterMethodName() {
        if (!camelCasePropertyName.startsWith("is") && !camelCasePropertyName.startsWith("has")) {
            return "setIs" + StringUtils.capitalize(camelCasePropertyName);
        }
        return super.setterMethodName();
    }

}
