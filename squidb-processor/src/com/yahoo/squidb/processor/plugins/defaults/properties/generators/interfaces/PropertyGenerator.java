/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.plugins.defaults.properties.generators.interfaces;

import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import javax.lang.model.element.VariableElement;

/**
 * Base interface for components that generate a property declaration along with getters and setters for that property
 */
public interface PropertyGenerator {

    /**
     * @return the {@link VariableElement} this PropertyGenerator was created from. This may be null if the
     * PropertyGenerator did not originate from a VariableElement in a model spec
     */
    VariableElement getField();

    /**
     * @return the type name of the Property to generate (e.g. StringProperty, LongProperty, etc.)
     */
    TypeName getPropertyType();

    /**
     * @return the name of the generated property
     */
    String getPropertyName();

    /**
     * @return the type used when setting or returning the value stored by this property
     */
    TypeName getTypeForAccessors();

    /**
     * @return the name of the generated getter method
     */
    String getterMethodName();

    /**
     * Declares the getter method for this property in the given TypeSpec.Builder
     *
     * @param builder the {@link TypeSpec.Builder} for the model class being built
     */
    void declareGetter(TypeSpec.Builder builder);

    /**
     * @return the name of the generated getter method
     */
    String setterMethodName();

    /**
     * Declares the setter method for this property in the given TypeSpec.Builder
     *
     * @param builder the {@link TypeSpec.Builder} for the model class being built
     */
    void declareSetter(TypeSpec.Builder builder);

    /**
     * @return true if the property should be marked deprecated
     */
    boolean isDeprecated();
}
