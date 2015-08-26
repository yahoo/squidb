/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.data;

import com.yahoo.aptutils.model.DeclaredTypeName;
import com.yahoo.aptutils.utils.AptUtils;
import com.yahoo.squidb.annotations.PrimaryKey;
import com.yahoo.squidb.annotations.TableModelSpec;
import com.yahoo.squidb.processor.TypeConstants;
import com.yahoo.squidb.processor.plugins.PluginManager;
import com.yahoo.squidb.processor.plugins.properties.generators.BasicLongPropertyGenerator;
import com.yahoo.squidb.processor.plugins.properties.generators.PropertyGenerator;

import java.util.Set;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.tools.Diagnostic;

public class TableModelSpecWrapper extends ModelSpec<TableModelSpec> {

    private PropertyGenerator idPropertyGenerator;
    private final DeclaredTypeName tableType;

    public TableModelSpecWrapper(TypeElement modelSpecElement, PluginManager pluginManager, AptUtils utils) {
        super(modelSpecElement, TableModelSpec.class, pluginManager, utils);
        if (isVirtualTable()) {
            tableType = TypeConstants.VIRTUAL_TABLE;
        } else {
            tableType = TypeConstants.TABLE;
        }
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
    public DeclaredTypeName getModelSuperclass() {
        return TypeConstants.TABLE_MODEL;
    }

    @Override
    protected void addModelSpecificImports(Set<DeclaredTypeName> imports) {
        imports.add(TypeConstants.LONG_PROPERTY);
        imports.add(TypeConstants.TABLE_MODEL);
        imports.add(tableType);
    }

    @Override
    protected void processVariableElement(VariableElement e, DeclaredTypeName typeName) {
        Set<Modifier> modifiers = e.getModifiers();
        if (modifiers.containsAll(TypeConstants.PUBLIC_STATIC_FINAL)) {
            if (e.getAnnotation(Deprecated.class) != null) {
                return;
            }
            if (TypeConstants.isPropertyType(typeName)) {
                utils.getMessager().printMessage(Diagnostic.Kind.WARNING, "Can't copy Property constants to model "
                        + "definition--they'd become part of the model", e);
            } else {
                addConstantField(e);
            }
        } else {
            if (e.getAnnotation(PrimaryKey.class) != null) {
                if (!BasicLongPropertyGenerator.handledColumnTypes().contains(typeName)) {
                    utils.getMessager().printMessage(Diagnostic.Kind.ERROR,
                            "Only long primary key columns are supported at this time.", e);
                } else if (idPropertyGenerator != null) {
                    utils.getMessager().printMessage(Diagnostic.Kind.ERROR,
                            "Only a single primary key column is supported at this time.", e);
                } else {
                    idPropertyGenerator = pluginContext.getPropertyGeneratorForVariableElement(e);
                }
            } else {
                initializePropertyGenerator(e);
            }
        }
    }

    /**
     * @return a {@link PropertyGenerator} for the model's id property
     */
    public PropertyGenerator getIdPropertyGenerator() {
        return idPropertyGenerator;
    }

    /**
     * @return the name of the table class (e.g. Table or VirtualTable)
     */
    public DeclaredTypeName getTableType() {
        return tableType;
    }
}
