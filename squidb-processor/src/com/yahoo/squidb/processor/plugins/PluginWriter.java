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
        // Stub
    }

    public List<DeclaredTypeName> getInterfacesToImplement() {
        // Stub
        return null;
    }

    public void writeConstants(JavaFileWriter writer) throws IOException {
        // Stub
    }

    public void writeConstructors(JavaFileWriter writer) throws IOException {
        // Stub
    }

    public void writeMethods(JavaFileWriter writer) throws IOException {
        // Stub
    }

    public void writeAdditionalHelpers(JavaFileWriter writer) throws IOException {
        // Stub
        // Arbitrary code can go here--other initializers (static or otherwise), other helper objects, whatever
    }
}
