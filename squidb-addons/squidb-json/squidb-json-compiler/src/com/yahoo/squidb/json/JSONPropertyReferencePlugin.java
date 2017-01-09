/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.json;

import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.yahoo.squidb.processor.TypeConstants;
import com.yahoo.squidb.processor.data.ModelSpec;
import com.yahoo.squidb.processor.plugins.defaults.properties.PropertyReferencePlugin;
import com.yahoo.squidb.processor.plugins.defaults.properties.generators.interfaces.PropertyGenerator;

import java.util.List;

import javax.lang.model.element.VariableElement;
import javax.tools.Diagnostic;

/**
 * Plugin which handles JSONProperty references in a ViewModelSpec or an InheritedModelSpec file.
 */
public abstract class JSONPropertyReferencePlugin<T extends ModelSpec<?, P>, P extends PropertyGenerator>
        extends PropertyReferencePlugin<T, P> {

    @Override
    protected boolean isSupportedPropertyType(TypeName fieldType) {
        return fieldType instanceof ParameterizedTypeName &&
                JSONTypes.JSON_PROPERTY.equals(((ParameterizedTypeName) fieldType).rawType);
    }

    @Override
    protected P getPropertyGenerator(VariableElement field, TypeName fieldType) {
        // We know it's a JSONProperty, so extract the type arg
        List<TypeName> typeArgs = ((ParameterizedTypeName) fieldType).typeArguments;
        if (typeArgs != null && typeArgs.size() == 1 && !TypeConstants.isGenericType(typeArgs.get(0))) {
            return getTypedJsonPropertyGenerator(field, fieldType);
        }
        pluginEnv.getMessager().printMessage(Diagnostic.Kind.WARNING,
                "JSONProperty must use a declared type argument; it cannot be raw or use a generic type argument",
                field);
        return null;
    }

    protected abstract P getTypedJsonPropertyGenerator(VariableElement field, TypeName propertyType);
}
