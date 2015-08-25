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
import com.yahoo.squidb.processor.plugins.PluginContext;
import com.yahoo.squidb.processor.plugins.properties.generators.BasicLongPropertyGenerator;
import com.yahoo.squidb.processor.plugins.properties.generators.PropertyGenerator;

import java.util.Set;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.tools.Diagnostic;

public class TableModelSpecWrapper extends ModelSpec<TableModelSpec> {

    private PropertyGenerator idPropertyGenerator;
    private DeclaredTypeName tableType;

    public TableModelSpecWrapper(TypeElement modelSpecElement, PluginContext pluginContext, AptUtils utils) {
        super(modelSpecElement, TableModelSpec.class, pluginContext, utils);
        if (isVirtualTable()) {
            tableType = TypeConstants.VIRTUAL_TABLE;
        } else {
            tableType = TypeConstants.TABLE;
        }
    }

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
                constantElements.add(e);
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
                    idPropertyGenerator = propertyGeneratorForElement(e);
                }
            } else {
                initializePropertyGenerator(e);
            }
        }
    }

    public PropertyGenerator getIdPropertyGenerator() {
        return idPropertyGenerator;
    }

    public DeclaredTypeName getTableType() {
        return tableType;
    }
}
