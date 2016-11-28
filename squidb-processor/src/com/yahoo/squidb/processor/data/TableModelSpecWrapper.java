/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.data;

import com.yahoo.aptutils.model.DeclaredTypeName;
import com.yahoo.aptutils.utils.AptUtils;
import com.yahoo.squidb.annotations.TableModelSpec;
import com.yahoo.squidb.processor.SqlUtils;
import com.yahoo.squidb.processor.TypeConstants;
import com.yahoo.squidb.processor.plugins.PluginEnvironment;

import java.util.Set;

import javax.lang.model.element.TypeElement;

public class TableModelSpecWrapper extends ModelSpec<TableModelSpec> {

    public TableModelSpecWrapper(TypeElement modelSpecElement, PluginEnvironment pluginEnv, AptUtils utils) {
        super(modelSpecElement, TableModelSpec.class, pluginEnv, utils);
        checkTableName();
    }

    private void checkTableName() {
        String tableName = getSpecAnnotation().tableName().trim();
        if (tableName.toLowerCase().startsWith("sqlite_")) {
            logError("Table names cannot start with 'sqlite_'; such names are reserved for internal use",
                    getModelSpecElement());
        } else {
            SqlUtils.checkIdentifier(tableName, "table", this, getModelSpecElement(), utils);
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
        return !AptUtils.isEmpty(modelSpecAnnotation.virtualModule());
    }

    @Override
    protected String getGeneratedClassNameString() {
        return modelSpecAnnotation.className();
    }

    @Override
    protected DeclaredTypeName getDefaultModelSuperclass() {
        return TypeConstants.TABLE_MODEL;
    }

    @Override
    protected void addModelSpecificImports(Set<DeclaredTypeName> imports) {
        imports.add(TypeConstants.LONG_PROPERTY);
        imports.add(TypeConstants.TABLE_MODEL);
        imports.add(TypeConstants.TABLE_MODEL_NAME);
        imports.add(getTableType());
    }

    /**
     * @return the name of the table class (e.g. Table or VirtualTable)
     */
    public DeclaredTypeName getTableType() {
        return isVirtualTable() ? TypeConstants.VIRTUAL_TABLE : TypeConstants.TABLE;
    }
}
