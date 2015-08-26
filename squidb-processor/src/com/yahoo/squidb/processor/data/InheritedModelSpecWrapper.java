/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.data;

import com.yahoo.aptutils.model.DeclaredTypeName;
import com.yahoo.aptutils.utils.AptUtils;
import com.yahoo.squidb.annotations.ColumnSpec;
import com.yahoo.squidb.annotations.InheritedModelSpec;
import com.yahoo.squidb.processor.TypeConstants;
import com.yahoo.squidb.processor.plugins.PluginManager;

import java.util.Set;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.tools.Diagnostic;

public class InheritedModelSpecWrapper extends ModelSpec<InheritedModelSpec> {

    private final DeclaredTypeName superclass;

    public InheritedModelSpecWrapper(TypeElement modelSpecElement, PluginManager pluginManager, AptUtils utils) {
        super(modelSpecElement, InheritedModelSpec.class, pluginManager, utils);
        this.superclass = new DeclaredTypeName(getSpecAnnotation().inheritsFrom());
    }

    @Override
    protected void processVariableElement(VariableElement field, DeclaredTypeName fieldType) {
        if (field.getAnnotation(Deprecated.class) != null) {
            return;
        }
        if (field.getAnnotation(ColumnSpec.class) != null) {
            utils.getMessager().printMessage(Diagnostic.Kind.WARNING,
                    "ColumnSpec is ignored outside of table models", field);
        }
        Set<Modifier> modifiers = field.getModifiers();
        if (modifiers.containsAll(TypeConstants.PUBLIC_STATIC_FINAL)) {
            if (!TypeConstants.isPropertyType(fieldType)) {
                addConstantField(field);
            } else {
                initializePropertyGenerator(field);
            }
        } else {
            utils.getMessager().printMessage(Diagnostic.Kind.WARNING, "Unused field in spec", field);
        }
    }

    @Override
    protected String getGeneratedClassNameString() {
        return modelSpecAnnotation.className();
    }

    @Override
    public DeclaredTypeName getModelSuperclass() {
        return superclass;
    }

    @Override
    protected void addModelSpecificImports(Set<DeclaredTypeName> imports) {
        imports.add(superclass);
    }
}
