/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.plugins;

import com.yahoo.aptutils.model.DeclaredTypeName;
import com.yahoo.aptutils.writer.JavaFileWriter;

import java.io.IOException;
import java.util.List;
import java.util.Set;

public class PluginWriter {

    public void addRequiredImports(Set<DeclaredTypeName> imports) {
        // Stub for subclasses to override
    }

    public List<DeclaredTypeName> getInterfacesToImplement() {
        // Stub for subclasses to override
        return null;
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
