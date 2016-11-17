/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.data;

import com.yahoo.aptutils.model.DeclaredTypeName;
import com.yahoo.aptutils.utils.AptUtils;
import com.yahoo.squidb.annotations.TableModelSpec;
import com.yahoo.squidb.processor.SqlKeywords;
import com.yahoo.squidb.processor.TypeConstants;
import com.yahoo.squidb.processor.plugins.PluginEnvironment;

import java.util.Set;

import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;

public class TableModelSpecWrapper extends ModelSpec<TableModelSpec> {

    public TableModelSpecWrapper(TypeElement modelSpecElement, PluginEnvironment pluginEnv, AptUtils utils) {
        super(modelSpecElement, TableModelSpec.class, pluginEnv, utils);
        checkTableName();
    }

    private void checkTableName() {
        String tableName = getSpecAnnotation().tableName();
        if (tableName.toLowerCase().startsWith("sqlite_")) {
            logError("Table names cannot start with 'sqlite_'; such names are reserved for internal use",
                    getModelSpecElement());
        } else if (SqlKeywords.isKeyword(tableName)) {
            if (SqlKeywords.isRestrictedKeyword(tableName)) {
                logError("Table name '" + tableName + "' is a reserved SQLite keyword that cannot be "
                        + "used as a table name", getModelSpecElement());
            } else {
                utils.getMessager().printMessage(Diagnostic.Kind.WARNING, "Table name '" + tableName + "' is a SQLite "
                        + "keyword. It is allowed as a table name but it is recommended you choose a non-keyword "
                        + "name instead", getModelSpecElement());
            }
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
