/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.plugins.defaults.enums;

import com.yahoo.aptutils.model.DeclaredTypeName;
import com.yahoo.aptutils.model.TypeName;
import com.yahoo.squidb.processor.TypeConstants;
import com.yahoo.squidb.processor.data.ModelSpec;
import com.yahoo.squidb.processor.plugins.PluginEnvironment;
import com.yahoo.squidb.processor.plugins.defaults.properties.PropertyReferencePlugin;
import com.yahoo.squidb.processor.plugins.defaults.properties.generators.interfaces.PropertyGenerator;

import java.util.List;

import javax.lang.model.element.VariableElement;
import javax.tools.Diagnostic.Kind;

/**
 * Plugin which handles EnumProperty references in a ViewModelSpec or an InheritedModelSpec file.
 */
public abstract class EnumPropertyReferencePlugin<T extends ModelSpec<?, P>, P extends PropertyGenerator>
        extends PropertyReferencePlugin<T, P> {

    public EnumPropertyReferencePlugin(ModelSpec<?, ?> modelSpec, PluginEnvironment pluginEnv) {
        super(modelSpec, pluginEnv);
    }

    @Override
    protected boolean isSupportedPropertyType(DeclaredTypeName fieldType) {
        return TypeConstants.ENUM_PROPERTY.equals(fieldType);
    }

    @Override
    protected P getPropertyGenerator(VariableElement field, DeclaredTypeName fieldType) {
        // We know it's an EnumProperty, so extract the type arg
        List<? extends TypeName> typeArgs = fieldType.getTypeArgs();
        if (typeArgs != null && typeArgs.size() == 1 && typeArgs.get(0) instanceof DeclaredTypeName) {
            return getTypedEnumPropertyGenerator(field, fieldType);
        }
        utils.getMessager().printMessage(Kind.WARNING,
                "EnumProperty must use a declared type argument; it cannot be raw or use a generic type argument",
                field);
        return null;
    }

    protected abstract P getTypedEnumPropertyGenerator(VariableElement field, DeclaredTypeName propertyType);
}

