/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.plugins.defaults;

import com.yahoo.aptutils.model.DeclaredTypeName;
import com.yahoo.aptutils.writer.JavaFileWriter;
import com.yahoo.aptutils.writer.expressions.Expressions;
import com.yahoo.squidb.processor.TypeConstants;
import com.yahoo.squidb.processor.data.ModelSpec;
import com.yahoo.squidb.processor.plugins.Plugin;
import com.yahoo.squidb.processor.plugins.PluginEnvironment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;

/**
 * A plugin that controls the copying of otherwise unhandled public static final fields in model specs as constants in
 * the generated model. It is enabled by default. It can be disabled by passing a bitmask with the
 * {@link PluginEnvironment#OPTIONS_DISABLE_DEFAULT_CONSTANT_COPYING} flag set.
 */
public class ConstantCopyingPlugin extends Plugin {

    private final List<VariableElement> constantElements = new ArrayList<VariableElement>();

    public ConstantCopyingPlugin(ModelSpec<?> modelSpec, PluginEnvironment pluginEnv) {
        super(modelSpec, pluginEnv);
    }

    @Override
    public boolean processVariableElement(VariableElement field, DeclaredTypeName fieldType) {
        if (field.getAnnotation(Deprecated.class) != null) {
            return false;
        }
        Set<Modifier> modifiers = field.getModifiers();
        if (modifiers.containsAll(TypeConstants.PUBLIC_STATIC_FINAL)) {
            constantElements.add(field);
            return true;
        }
        return false;
    }

    @Override
    public void addRequiredImports(Set<DeclaredTypeName> imports) {
        utils.accumulateImportsFromElements(imports, constantElements);
    }

    @Override
    public void beforeEmitSchema(JavaFileWriter writer) throws IOException {
        writer.writeComment("--- constants");
        for (VariableElement constant : constantElements) {
            writer.writeFieldDeclaration(
                    utils.getTypeNameFromTypeMirror(constant.asType()),
                    constant.getSimpleName().toString(),
                    Expressions.staticReference(modelSpec.getModelSpecName(), constant.getSimpleName().toString()),
                    TypeConstants.PUBLIC_STATIC_FINAL);
        }
        writer.writeNewline();
    }
}
