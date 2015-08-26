/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.data;

import com.yahoo.aptutils.model.DeclaredTypeName;
import com.yahoo.aptutils.utils.AptUtils;
import com.yahoo.squidb.annotations.ColumnSpec;
import com.yahoo.squidb.annotations.ViewModelSpec;
import com.yahoo.squidb.annotations.ViewQuery;
import com.yahoo.squidb.processor.TypeConstants;
import com.yahoo.squidb.processor.plugins.PluginManager;

import java.util.Set;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.tools.Diagnostic;

public class ViewModelSpecWrapper extends ModelSpec<ViewModelSpec> {

    private VariableElement queryElement;
    private ViewQuery viewQueryAnnotation;

    public ViewModelSpecWrapper(TypeElement modelSpecElement, PluginManager pluginManager, AptUtils utils) {
        super(modelSpecElement, ViewModelSpec.class, pluginManager, utils);
    }

    @Override
    protected String getGeneratedClassNameString() {
        return modelSpecAnnotation.className();
    }

    @Override
    public DeclaredTypeName getModelSuperclass() {
        return TypeConstants.VIEW_MODEL;
    }

    @Override
    protected void processVariableElement(VariableElement e, DeclaredTypeName typeName) {
        if (e.getAnnotation(Deprecated.class) != null) {
            return;
        }
        if (e.getAnnotation(ColumnSpec.class) != null) {
            utils.getMessager().printMessage(Diagnostic.Kind.WARNING, "ColumnSpec is ignored outside of table models", e);
        }
        boolean isViewProperty = TypeConstants.isPropertyType(typeName);
        ViewQuery isViewQuery = e.getAnnotation(ViewQuery.class);
        Set<Modifier> modifiers = e.getModifiers();
        if (modifiers.containsAll(TypeConstants.PUBLIC_STATIC_FINAL)) {
            if (isViewQuery != null) {
                if (!TypeConstants.QUERY.equals(typeName)) {
                    utils.getMessager().printMessage(Diagnostic.Kind.ERROR,
                            "ViewQuery must be an instance of " + TypeConstants.QUERY.toString());
                } else if (queryElement != null) {
                    utils.getMessager().printMessage(Diagnostic.Kind.ERROR, "Only one ViewQuery per spec allowedd");
                } else {
                    viewQueryAnnotation = isViewQuery;
                    queryElement = e;
                }
            } else if (!isViewProperty) {
                addConstantField(e);
            } else {
                initializePropertyGenerator(e);
            }
        } else if (isViewProperty) {
            utils.getMessager().printMessage(Diagnostic.Kind.ERROR, "View properties must be public static final", e);
        } else {
            utils.getMessager().printMessage(Diagnostic.Kind.WARNING, "Unused field in spec", e);
        }
    }

    /**
     * @return a {@link VariableElement} representing the query in the model spec that should define the view
     */
    public VariableElement getQueryElement() {
        return queryElement;
    }

    /**
     * @return the {@link ViewQuery} annotation for the query element
     * @see #getQueryElement()
     */
    public ViewQuery getViewQueryAnnotation() {
        return viewQueryAnnotation;
    }

    @Override
    protected void addModelSpecificImports(Set<DeclaredTypeName> imports) {
        if (queryElement != null) {
            if (modelSpecAnnotation.isSubquery()) {
                imports.add(TypeConstants.SUBQUERY_TABLE);
            } else {
                imports.add(TypeConstants.VIEW);
            }
            imports.add(TypeConstants.QUERY);
        }
    }
}
