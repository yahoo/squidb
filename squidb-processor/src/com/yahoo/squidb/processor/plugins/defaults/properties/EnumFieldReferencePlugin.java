/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.plugins.defaults.properties;

import com.yahoo.aptutils.model.DeclaredTypeName;
import com.yahoo.aptutils.model.TypeName;
import com.yahoo.squidb.processor.TypeConstants;
import com.yahoo.squidb.processor.data.InheritedModelSpecWrapper;
import com.yahoo.squidb.processor.data.ModelSpec;
import com.yahoo.squidb.processor.data.ViewModelSpecWrapper;
import com.yahoo.squidb.processor.plugins.PluginEnvironment;
import com.yahoo.squidb.processor.plugins.defaults.properties.generators.EnumPropertyGenerator;
import com.yahoo.squidb.processor.plugins.defaults.properties.generators.PropertyGenerator;

import java.util.List;

import javax.lang.model.element.VariableElement;
import javax.tools.Diagnostic.Kind;

/**
 * Plugin which handles EnumProperty references in a ViewModelSpec or an InheritedModelSpec file.
 */
public class EnumFieldReferencePlugin extends FieldReferencePlugin {

    public EnumFieldReferencePlugin(ModelSpec<?> modelSpec, PluginEnvironment pluginEnv) {
        super(modelSpec, pluginEnv);
    }

    @Override
    public boolean hasChangesForModelSpec() {
        return modelSpec instanceof ViewModelSpecWrapper || modelSpec instanceof InheritedModelSpecWrapper;
    }

    @Override
    protected boolean hasPropertyGeneratorForField(VariableElement field, DeclaredTypeName fieldType) {
        return TypeConstants.isVisibleConstant(field)
                && TypeConstants.ENUM_PROPERTY.equals(fieldType);
    }

    @Override
    protected PropertyGenerator getPropertyGenerator(VariableElement field, DeclaredTypeName fieldType) {
        // We know it's an EnumProperty, so extract the type arg
        List<? extends TypeName> typeArgs = fieldType.getTypeArgs();
        if (typeArgs != null && typeArgs.size() == 1 && typeArgs.get(0) instanceof DeclaredTypeName) {
            return new EnumPropertyGenerator(modelSpec, field, utils, (DeclaredTypeName) typeArgs.get(0));
        }
        utils.getMessager().printMessage(Kind.WARNING,
                "EnumProperty must use a declared type argument; it cannot be raw or use a generic type argument",
                field);
        return null;
    }
}

