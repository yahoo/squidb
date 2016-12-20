/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.plugins.defaults.properties.generators;

import com.yahoo.aptutils.model.CoreTypes;
import com.yahoo.aptutils.model.DeclaredTypeName;
import com.yahoo.aptutils.utils.AptUtils;
import com.yahoo.squidb.processor.TypeConstants;
import com.yahoo.squidb.processor.data.ModelSpec;

import java.util.Arrays;
import java.util.List;

import javax.lang.model.element.VariableElement;

/**
 * An implementation of
 * {@link com.yahoo.squidb.processor.plugins.defaults.properties.generators.interfaces.TableModelPropertyGenerator}
 * for handling long fields
 */
public class BasicLongPropertyGenerator extends BasicTableModelPropertyGenerator {

    public static List<DeclaredTypeName> handledColumnTypes() {
        return Arrays.asList(CoreTypes.JAVA_LONG, CoreTypes.PRIMITIVE_LONG);
    }

    public BasicLongPropertyGenerator(ModelSpec<?, ?> modelSpec, String columnName, AptUtils utils) {
        super(modelSpec, columnName, utils);
    }

    public BasicLongPropertyGenerator(ModelSpec<?, ?> modelSpec, String columnName,
            String propertyName, AptUtils utils) {
        super(modelSpec, columnName, propertyName, utils);
    }

    public BasicLongPropertyGenerator(ModelSpec<?, ?> modelSpec, VariableElement field, AptUtils utils) {
        super(modelSpec, field, utils);
    }

    @Override
    public DeclaredTypeName getTypeForAccessors() {
        return CoreTypes.JAVA_LONG;
    }

    @Override
    public DeclaredTypeName getPropertyType() {
        return TypeConstants.LONG_PROPERTY;
    }

    @Override
    protected String columnSpecDefaultValueToSql() {
        String value = super.columnSpecDefaultValueToSql();
        if ("NULL".equalsIgnoreCase(value)) {
            return value;
        }
        char last = value.charAt(value.length() - 1);
        if (last == 'L' || last == 'l') {
            return value.substring(0, value.length() - 1);
        }
        return value;
    }

    @Override
    protected String getDefaultValueForContentValues() {
        String value = super.getDefaultValueForContentValues();
        if ("NULL".equalsIgnoreCase(value)) {
            return value;
        }
        char last = value.charAt(value.length() - 1);
        if (last == 'L' || last == 'l') {
            return value;
        }
        return value + "L";
    }
}
