/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.plugins.properties.generators;

import com.yahoo.aptutils.model.DeclaredTypeName;
import com.yahoo.aptutils.utils.AptUtils;
import com.yahoo.squidb.annotations.PrimaryKey;

import javax.lang.model.element.VariableElement;

public class BasicIdPropertyGenerator extends BasicLongPropertyGenerator {

    private final PrimaryKey annotation;

    public BasicIdPropertyGenerator(VariableElement field, DeclaredTypeName generatedClassName, AptUtils utils) {
        super(field, generatedClassName, utils);
        annotation = field.getAnnotation(PrimaryKey.class);
    }

    @Override
    protected String getColumnDefinition() {
        String newColumnDef = "PRIMARY KEY";
        if (annotation.autoincrement()) {
            newColumnDef += " AUTOINCREMENT";
        }

        String columnDef = super.getColumnDefinition();
        if (AptUtils.isEmpty(columnDef)) {
            return "\"" + newColumnDef + "\"";
        } else if (columnDef.toUpperCase().contains("PRIMARY KEY")) {
            return columnDef;
        } else {
            return columnDef.replaceFirst("\"", "\"" + newColumnDef + ", ");
        }
    }
}
