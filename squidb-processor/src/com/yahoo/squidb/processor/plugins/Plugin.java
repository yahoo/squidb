/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.plugins;

import com.yahoo.aptutils.model.DeclaredTypeName;
import com.yahoo.aptutils.utils.AptUtils;
import com.yahoo.aptutils.writer.JavaFileWriter;
import com.yahoo.squidb.processor.data.ModelSpec;
import com.yahoo.squidb.processor.plugins.properties.generators.PropertyGenerator;

import java.io.IOException;
import java.util.Set;

import javax.lang.model.element.VariableElement;

public class Plugin {

    protected final ModelSpec<?> modelSpec;
    protected final AptUtils utils;

    public Plugin(ModelSpec<?> modelSpec, AptUtils utils) {
        this.modelSpec = modelSpec;
        this.utils = utils;
    }

    public boolean hasPropertyGeneratorForField(VariableElement field, DeclaredTypeName fieldType) {
        // Stub for subclasses to override
        return false;
    }

    public PropertyGenerator getPropertyGenerator(VariableElement field, DeclaredTypeName fieldType) {
        // Stub for subclasses to override
        return null;
    }

    public void addRequiredImports(Set<DeclaredTypeName> imports) {
        // Stub for subclasses to override
    }

    public void addInterfacesToImplement(Set<DeclaredTypeName> interfaces) {
        // Stub for subclasses to override
    }

    public void writeConstants(JavaFileWriter writer) throws IOException {
        // Stub for subclasses to override
    }

    public void writeConstructors(JavaFileWriter writer) throws IOException {
        // Stub for subclasses to override
    }

    public void writeMethods(JavaFileWriter writer) throws IOException {
        // Stub for subclasses to override
    }

    public void writeAdditionalCode(JavaFileWriter writer) throws IOException {
        // Stub for subclasses to override
    }

}
