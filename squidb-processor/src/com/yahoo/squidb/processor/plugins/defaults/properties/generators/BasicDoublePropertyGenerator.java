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
 * An implementation of {@link PropertyGenerator} for handling double fields
 */
public class BasicDoublePropertyGenerator extends BasicPropertyGenerator {

    public static List<DeclaredTypeName> handledColumnTypes() {
        return Arrays.asList(CoreTypes.JAVA_FLOAT, CoreTypes.PRIMITIVE_FLOAT,
                CoreTypes.JAVA_DOUBLE, CoreTypes.PRIMITIVE_DOUBLE);
    }

    public BasicDoublePropertyGenerator(ModelSpec<?> modelSpec, String columnName, AptUtils utils) {
        super(modelSpec, columnName, utils);
    }

    public BasicDoublePropertyGenerator(ModelSpec<?> modelSpec, String columnName,
            String propertyName, AptUtils utils) {
        super(modelSpec, columnName, propertyName, utils);
    }

    public BasicDoublePropertyGenerator(ModelSpec<?> modelSpec, VariableElement field, AptUtils utils) {
        super(modelSpec, field, utils);
    }

    @Override
    public DeclaredTypeName getTypeForAccessors() {
        return CoreTypes.JAVA_DOUBLE;
    }

    @Override
    public DeclaredTypeName getPropertyType() {
        return TypeConstants.DOUBLE_PROPERTY;
    }

}
