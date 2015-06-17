/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.properties.generators;

import com.yahoo.aptutils.model.DeclaredTypeName;
import com.yahoo.aptutils.utils.AptUtils;

import javax.lang.model.element.VariableElement;

public class BasicIdPropertyGenerator extends BasicLongPropertyGenerator {

    public BasicIdPropertyGenerator(VariableElement element, DeclaredTypeName modelName, AptUtils utils) {
        super(element, modelName, utils);
    }

    @Override
    protected String getColumnDefinition() {
        String columnDef = super.getColumnDefinition();
        if (AptUtils.isEmpty(columnDef) || !columnDef.contains("PRIMARY KEY")) {
            String newColumnDef = "INTEGER PRIMARY KEY AUTOINCREMENT";
            if (!AptUtils.isEmpty(columnDef)) {
                newColumnDef += ", " + columnDef;
            }
            columnDef = newColumnDef;
        }
        return columnDef;
    }
}
