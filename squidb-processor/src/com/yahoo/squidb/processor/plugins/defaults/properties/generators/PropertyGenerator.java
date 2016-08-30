/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.plugins.defaults.properties.generators;

import com.yahoo.aptutils.model.DeclaredTypeName;
import com.yahoo.aptutils.utils.AptUtils;
import com.yahoo.aptutils.writer.JavaFileWriter;
import com.yahoo.squidb.processor.TypeConstants;
import com.yahoo.squidb.processor.data.ModelSpec;

import java.io.IOException;
import java.util.Set;

import javax.lang.model.element.VariableElement;

/**
 * Base class for controlling the generation of property declarations and the convenience getters/setters that go
 * with them. Plugins can create PropertyGenerators for handling fields of some type(s) in a particular way. Default
 * implementations of PropertyGenerators already exist for the basic field types (e.g.
 * {@link BasicStringPropertyGenerator}, {@link BasicLongPropertyGenerator}) so Plugins that just want to tweak the
 * existing behavior should probably override one of these existing implementations, or override
 * {@link BasicPropertyGenerator} which includes handling for the {@link com.yahoo.squidb.annotations.ColumnSpec}
 * annotation but no specific type handling.
 */
public abstract class PropertyGenerator {

    protected final ModelSpec<?> modelSpec;
    protected final VariableElement field;
    protected final AptUtils utils;
    protected final boolean isDeprecated;

    public PropertyGenerator(ModelSpec<?> modelSpec, VariableElement field, AptUtils utils) {
        this.modelSpec = modelSpec;
        this.field = field;
        this.utils = utils;
        this.isDeprecated = field != null && field.getAnnotation(Deprecated.class) != null;
    }

    /**
     * @return the {@link VariableElement} this PropertyGenerator was created from. This may be null if the
     * PropertyGenerator did not originate from a VariableElement in a model spec
     */
    public VariableElement getField() {
        return field;
    }

    /**
     * @param imports an accumulator set of type names to import
     */
    public void registerRequiredImports(Set<DeclaredTypeName> imports) {
        imports.add(getPropertyType());
        DeclaredTypeName accessorType = getTypeForAccessors();
        if (!TypeConstants.isPrimitiveType(accessorType)) {
            imports.add(accessorType);
        }
        registerAdditionalImports(imports);
    }

    /**
     * Overrideable hook to add additional imports required by this property
     *
     * @param imports set to add the required imports to
     */
    protected abstract void registerAdditionalImports(Set<DeclaredTypeName> imports);

    /**
     * @return the type name of the Property to generate (e.g. StringProperty, LongProperty, etc.)
     */
    public abstract DeclaredTypeName getPropertyType();

    /**
     * @return the name of the generated property
     */
    public abstract String getPropertyName();

    /**
     * @return the type used when setting or returning the value stored by this property
     */
    public abstract DeclaredTypeName getTypeForAccessors();

    /**
     * Called to write the declaration of the property itself
     *
     * @param writer a {@link JavaFileWriter} for writing to
     */
    public abstract void emitPropertyDeclaration(JavaFileWriter writer) throws IOException;

    /**
     * @return the name of the generated getter method
     */
    public abstract String getterMethodName();

    /**
     * Called to write the convenience getter the property itself
     *
     * @param writer a {@link JavaFileWriter} for writing to
     */
    public abstract void emitGetter(JavaFileWriter writer) throws IOException;

    /**
     * @return the name of the generated getter method
     */
    public abstract String setterMethodName();

    /**
     * Called to write the convenience setter the property itself
     *
     * @param writer a {@link JavaFileWriter} for writing to
     */
    public abstract void emitSetter(JavaFileWriter writer) throws IOException;

    /**
     * Called to emit a call to ContentValues.put for adding a property default to the model default values
     *
     * @param writer a {@link JavaFileWriter} for writing to
     * @param contentValuesName the name of the content values variable to call
     */
    public abstract void emitPutDefault(JavaFileWriter writer, String contentValuesName) throws IOException;

    /**
     * @return true if the property should be marked deprecated
     */
    public boolean isDeprecated() {
        return isDeprecated;
    }

}
