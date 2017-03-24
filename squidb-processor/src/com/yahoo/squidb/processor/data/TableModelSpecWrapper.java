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
import com.yahoo.squidb.processor.plugins.defaults.constraints.CheckAnnotationHandler;
import com.yahoo.squidb.processor.plugins.defaults.constraints.PrimaryKeyColumnsAnnotationHandler;
import com.yahoo.squidb.processor.plugins.defaults.constraints.TableConstraintAnnotationHandler;
import com.yahoo.squidb.processor.plugins.defaults.constraints.ConstraintSqlAnnotationHandler;
import com.yahoo.squidb.processor.plugins.defaults.constraints.TableConstraintsContainerAnnotationHandler;
import com.yahoo.squidb.processor.plugins.defaults.constraints.UniqueColumnsAnnotationHandler;
import com.yahoo.squidb.processor.plugins.defaults.properties.generators.interfaces.TableModelPropertyGenerator;

import java.util.LinkedHashSet;
import java.util.Set;

import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;

public class TableModelSpecWrapper extends ModelSpec<TableModelSpec, TableModelPropertyGenerator> {

    private String tableConstraintString;
    private final Set<TableConstraintAnnotationHandler<?>> annotationHandlers;

    TableModelSpecWrapper(TypeElement modelSpecElement, PluginEnvironment pluginEnv) {
        super(modelSpecElement, TableModelSpec.class, pluginEnv);
        annotationHandlers = initAnnotationHandlers();
    }

    private Set<TableConstraintAnnotationHandler<?>> initAnnotationHandlers() {
        Set<TableConstraintAnnotationHandler<?>> handlers = new LinkedHashSet<>();
        handlers.add(new TableConstraintsContainerAnnotationHandler());
        handlers.add(new ConstraintSqlAnnotationHandler.TableConstraintSqlAnnotationHandler());
        handlers.add(new PrimaryKeyColumnsAnnotationHandler());
        handlers.add(new UniqueColumnsAnnotationHandler());
        handlers.add(new CheckAnnotationHandler.TableCheckAnnotationHandler());
        return handlers;
    }

    @Override
    void initialize() {
        initializeTableConstraintString();
        super.initialize();
        doValidation();
    }

    private void doValidation() {
        validateTableName();
        for (TableConstraintAnnotationHandler<?> handler : annotationHandlers) {
            handler.validateAnnotationForTable(this, pluginEnv);
        }
    }

    private void validateTableName() {
        String tableName = getSpecAnnotation().tableName().trim();
        if (tableName.toLowerCase().startsWith("sqlite_")) {
            logError("Table names cannot start with 'sqlite_'; such names are reserved for internal use",
                    getModelSpecElement());
        } else {
            SqlUtils.checkIdentifier(tableName, "table", this, getModelSpecElement(), pluginEnv.getMessager());
        }
    }

    private void initializeTableConstraintString() {
        StringBuilder constraintBuilder = new StringBuilder();
        if (!StringUtils.isEmpty(modelSpecAnnotation.tableConstraint().trim())) {
            constraintBuilder.append(modelSpecAnnotation.tableConstraint().trim());
            pluginEnv.getMessager().printMessage(Diagnostic.Kind.WARNING, "tableConstraint is deprecated, use "
                    + "the dedicated constraint annotations instead", getModelSpecElement());
        }
        for (TableConstraintAnnotationHandler<?> handler : annotationHandlers) {
            handler.appendConstraintForTable(constraintBuilder, this, pluginEnv);
        }
        tableConstraintString = constraintBuilder.toString().trim();
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

    /**
     * @return the constraints for this table as a SQL string
     */
    public String getTableConstraintString() {
        return tableConstraintString;
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
