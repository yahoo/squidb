/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.data;

import com.yahoo.aptutils.model.DeclaredTypeName;
import com.yahoo.aptutils.utils.AptUtils;
import com.yahoo.squidb.annotations.TableModelSpec;
import com.yahoo.squidb.processor.TypeConstants;
import com.yahoo.squidb.processor.plugins.PluginEnvironment;
import com.yahoo.squidb.processor.plugins.defaults.properties.generators.PropertyGenerator;

import java.util.Set;

import javax.lang.model.element.TypeElement;

public class TableModelSpecWrapper extends ModelSpec<TableModelSpec> {

    public static final String DEFAULT_ID_PROPERTY_NAME = "ID";
    public static final String METADATA_KEY_ID_PROPERTY_GENERATOR = "idPropertyGenerator";

    private final DeclaredTypeName tableType;

    public TableModelSpecWrapper(TypeElement modelSpecElement, PluginEnvironment pluginEnv, AptUtils utils) {
        super(modelSpecElement, TableModelSpec.class, pluginEnv, utils);
        if (isVirtualTable()) {
            tableType = TypeConstants.VIRTUAL_TABLE;
        } else {
            tableType = TypeConstants.TABLE;
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
        imports.add(tableType);
    }

    /**
     * @return a {@link PropertyGenerator} for the model's id property
     */
    public PropertyGenerator getIdPropertyGenerator() {
        return getMetadata(METADATA_KEY_ID_PROPERTY_GENERATOR);
    }

    public String getIdPropertyName() {
        PropertyGenerator idPropertyGenerator = getIdPropertyGenerator();
        if (idPropertyGenerator != null) {
            return idPropertyGenerator.getPropertyName();
        } else {
            return DEFAULT_ID_PROPERTY_NAME;
        }
    }

    /**
     * @return the name of the table class (e.g. Table or VirtualTable)
     */
    public DeclaredTypeName getTableType() {
        return tableType;
    }
}
