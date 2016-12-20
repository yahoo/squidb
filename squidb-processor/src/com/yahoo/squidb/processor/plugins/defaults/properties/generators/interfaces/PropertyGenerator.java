/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.plugins.defaults.properties.generators.interfaces;

import com.yahoo.aptutils.model.DeclaredTypeName;
import com.yahoo.aptutils.writer.JavaFileWriter;

import java.io.IOException;
import java.util.Set;

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
     * @param imports an accumulator set of type names to import
     */
    void registerRequiredImports(Set<DeclaredTypeName> imports);

    /**
     * @return the type name of the Property to generate (e.g. StringProperty, LongProperty, etc.)
     */
    DeclaredTypeName getPropertyType();

    /**
     * @return the name of the generated property
     */
    String getPropertyName();

    /**
     * @return the type used when setting or returning the value stored by this property
     */
    DeclaredTypeName getTypeForAccessors();

    /**
     * @return the name of the generated getter method
     */
    String getterMethodName();

    /**
     * Called to write the convenience getter the property itself
     *
     * @param writer a {@link JavaFileWriter} for writing to
     */
    void emitGetter(JavaFileWriter writer) throws IOException;

    /**
     * @return the name of the generated getter method
     */
    String setterMethodName();

    /**
     * Called to write the convenience setter the property itself
     *
     * @param writer a {@link JavaFileWriter} for writing to
     */
    void emitSetter(JavaFileWriter writer) throws IOException;

    /**
     * @return true if the property should be marked deprecated
     */
    boolean isDeprecated();
}
