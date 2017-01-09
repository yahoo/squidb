/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.data;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import com.yahoo.squidb.annotations.InheritedModelSpec;
import com.yahoo.squidb.processor.plugins.PluginEnvironment;
import com.yahoo.squidb.processor.plugins.defaults.properties.generators.interfaces.InheritedModelPropertyGenerator;

import javax.lang.model.element.TypeElement;

public class InheritedModelSpecWrapper extends ModelSpec<InheritedModelSpec, InheritedModelPropertyGenerator> {

    private TypeName superclass;

    public InheritedModelSpecWrapper(TypeElement modelSpecElement, PluginEnvironment pluginEnv) {
        super(modelSpecElement, InheritedModelSpec.class, pluginEnv);
        this.superclass = ClassName.bestGuess(getSpecAnnotation().inheritsFrom());
    }

    @Override
    public <RETURN, PARAMETER> RETURN accept(ModelSpecVisitor<RETURN, PARAMETER> visitor, PARAMETER data) {
        return visitor.visitInheritedModel(this, data);
    }

    @Override
    protected String getGeneratedClassNameString() {
        return modelSpecAnnotation.className();
    }

    @Override
    protected TypeName getDefaultModelSuperclass() {
        if (superclass == null) {
            this.superclass = ClassName.bestGuess(getSpecAnnotation().inheritsFrom());
        }
        return superclass;
    }
}
