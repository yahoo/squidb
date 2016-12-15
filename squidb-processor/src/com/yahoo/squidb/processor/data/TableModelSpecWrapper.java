/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.data;

import com.squareup.javapoet.TypeName;
import com.yahoo.squidb.annotations.TableModelSpec;
import com.yahoo.squidb.processor.SqlUtils;
import com.yahoo.squidb.processor.StringUtils;
import com.yahoo.squidb.processor.TypeConstants;
import com.yahoo.squidb.processor.plugins.PluginEnvironment;
import com.yahoo.squidb.processor.plugins.defaults.properties.generators.interfaces.TableModelPropertyGenerator;

import javax.lang.model.element.TypeElement;

public class TableModelSpecWrapper extends ModelSpec<TableModelSpec, TableModelPropertyGenerator> {

    public TableModelSpecWrapper(TypeElement modelSpecElement, PluginEnvironment pluginEnv) {
        super(modelSpecElement, TableModelSpec.class, pluginEnv);
        checkTableName();
    }

    private void checkTableName() {
        String tableName = getSpecAnnotation().tableName().trim();
        if (tableName.toLowerCase().startsWith("sqlite_")) {
            logError("Table names cannot start with 'sqlite_'; such names are reserved for internal use",
                    getModelSpecElement());
        } else {
            SqlUtils.checkIdentifier(tableName, "table", this, getModelSpecElement(), pluginEnv.getMessager());
        }
    }

    @Override
    public <RETURN, PARAMETER> RETURN accept(ModelSpecVisitor<RETURN, PARAMETER> visitor, PARAMETER data) {
        return visitor.visitTableModel(this, data);
    }

    /**
     * @return true if the table model is for a virtual table, false otherwise
     */
    public boolean isVirtualTable() {
        return !StringUtils.isEmpty(modelSpecAnnotation.virtualModule());
    }

    @Override
    protected String getGeneratedClassNameString() {
        return modelSpecAnnotation.className();
    }

    @Override
    protected TypeName getDefaultModelSuperclass() {
        return TypeConstants.TABLE_MODEL;
    }

    /**
     * @return the name of the table class (e.g. Table or VirtualTable)
     */
    public TypeName getTableType() {
        return isVirtualTable() ? TypeConstants.VIRTUAL_TABLE : TypeConstants.TABLE;
    }
}
