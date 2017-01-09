/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.plugins.defaults.enums;

import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
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
    protected boolean isSupportedPropertyType(TypeName fieldType) {
        return fieldType instanceof ParameterizedTypeName &&
                TypeConstants.ENUM_PROPERTY.equals(((ParameterizedTypeName) fieldType).rawType);
    }

    @Override
    protected P getPropertyGenerator(VariableElement field, TypeName fieldType) {
        // We know it's an EnumProperty, so extract the type arg
        List<TypeName> typeArgs = ((ParameterizedTypeName) fieldType).typeArguments;
        if (typeArgs != null && typeArgs.size() == 1 && !TypeConstants.isGenericType(typeArgs.get(0))) {
            return getTypedEnumPropertyGenerator(field, (ParameterizedTypeName) fieldType);
        }
        pluginEnv.getMessager().printMessage(Kind.WARNING,
                "EnumProperty must use a declared type argument; it cannot be raw or use a generic type argument",
                field);
        return null;
    }

    protected abstract P getTypedEnumPropertyGenerator(VariableElement field, ParameterizedTypeName propertyType);
}

