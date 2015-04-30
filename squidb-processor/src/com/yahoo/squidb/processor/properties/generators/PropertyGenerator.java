/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.properties.generators;

import com.yahoo.aptutils.model.DeclaredTypeName;
import com.yahoo.aptutils.utils.AptUtils;
import com.yahoo.aptutils.writer.JavaFileWriter;

import java.io.IOException;
import java.util.Set;

import javax.lang.model.element.VariableElement;

public abstract class PropertyGenerator {

    protected final VariableElement element;
    protected final DeclaredTypeName modelName;
    protected final AptUtils utils;
    protected final boolean isDeprecated;

    public PropertyGenerator(VariableElement element, DeclaredTypeName modelName, AptUtils utils) {
        this.element = element;
        this.modelName = modelName;
        this.utils = utils;
        this.isDeprecated = element.getAnnotation(Deprecated.class) != null;
    }

    public VariableElement getElement() {
        return element;
    }

    public void registerRequiredImports(Set<DeclaredTypeName> imports) {
        imports.add(getPropertyType());
        registerAdditionalImports(imports);
    }

    protected abstract void registerAdditionalImports(Set<DeclaredTypeName> imports);

    public abstract DeclaredTypeName getPropertyType();

    public abstract String getPropertyName();

    public void beforeEmitPropertyDeclaration(JavaFileWriter writer) throws IOException {
        // Subclasses can override
    }

    public abstract void emitPropertyDeclaration(JavaFileWriter writer) throws IOException;

    public void afterEmitPropertyDeclaration(JavaFileWriter writer) throws IOException {
        // Subclasses can override
    }

    public void beforeEmitGetter(JavaFileWriter writer) throws IOException {
        // Subclasses can override
    }

    public abstract void emitGetter(JavaFileWriter writer) throws IOException;

    public void afterEmitGetter(JavaFileWriter writer) throws IOException {
        // Subclasses can override
    }

    public void beforeEmitSetter(JavaFileWriter writer) throws IOException {
        // Subclasses can override
    }

    public abstract void emitSetter(JavaFileWriter writer) throws IOException;

    public void afterEmitSetter(JavaFileWriter writer) throws IOException {
        // Subclasses can override
    }

    public abstract void emitPutDefault(JavaFileWriter writer, String contentValuesName) throws IOException;

    public boolean isDeprecated() {
        return isDeprecated;
    }

}
