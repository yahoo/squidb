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

import java.util.Arrays;
import java.util.List;

import javax.lang.model.element.VariableElement;

/**
 * An implementation of {@link PropertyGenerator} for handling long fields
 */
public class BasicLongPropertyGenerator extends BasicPropertyGenerator {

    private String columnDefDefault;
    private String contentValuesDefault;

    public static List<DeclaredTypeName> handledColumnTypes() {
        return Arrays.asList(CoreTypes.JAVA_LONG, CoreTypes.PRIMITIVE_LONG);
    }

    public BasicLongPropertyGenerator(ModelSpec<?> modelSpec, String columnName, AptUtils utils) {
        super(modelSpec, columnName, utils);
    }

    public BasicLongPropertyGenerator(ModelSpec<?> modelSpec, String columnName,
            String propertyName, AptUtils utils) {
        super(modelSpec, columnName, propertyName, utils);
    }

    public BasicLongPropertyGenerator(ModelSpec<?> modelSpec, VariableElement field, AptUtils utils) {
        super(modelSpec, field, utils);

        String columnDefault = getColumnDefault();
        if (ColumnSpec.DEFAULT_NULL.equals(columnDefault)) {
            columnDefDefault = "NULL";
        } else {
            char last = columnDefault.charAt(columnDefault.length() - 1);
            if (last == 'L' || last == 'l') {
                contentValuesDefault = columnDefault;
                columnDefDefault = columnDefault.substring(0, columnDefault.length() - 1);
            } else {
                columnDefDefault = columnDefault;
                contentValuesDefault = columnDefault + "L";
            }
        }
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
    protected String getColumnDefinitionDefaultValue() {
        return columnDefDefault;
    }

    @Override
    protected String getContentValuesDefaultValue() {
        return contentValuesDefault;
    }
}
