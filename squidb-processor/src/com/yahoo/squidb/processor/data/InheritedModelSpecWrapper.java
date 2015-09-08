/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.data;

import com.yahoo.aptutils.model.DeclaredTypeName;
import com.yahoo.aptutils.utils.AptUtils;
import com.yahoo.squidb.annotations.InheritedModelSpec;
import com.yahoo.squidb.processor.plugins.PluginEnvironment;

import java.util.Set;

import javax.lang.model.element.TypeElement;

public class InheritedModelSpecWrapper extends ModelSpec<InheritedModelSpec> {

    private final DeclaredTypeName superclass;

    public InheritedModelSpecWrapper(TypeElement modelSpecElement, PluginEnvironment pluginEnv, AptUtils utils) {
        super(modelSpecElement, InheritedModelSpec.class, pluginEnv, utils);
        this.superclass = new DeclaredTypeName(getSpecAnnotation().inheritsFrom());
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
