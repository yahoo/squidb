/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.plugins;

import com.yahoo.aptutils.model.DeclaredTypeName;
import com.yahoo.aptutils.utils.AptUtils;
import com.yahoo.aptutils.writer.JavaFileWriter;
import com.yahoo.squidb.processor.data.ModelSpec;
import com.yahoo.squidb.processor.plugins.properties.generators.PropertyGenerator;

import java.io.IOException;
import java.util.Set;

import javax.lang.model.element.VariableElement;

/**
 * Base class for all code generator plugins. All Plugins should extend from this base class, and can override any of
 * its methods to add code generation of that type. The types of code generation supported via plugins are:
 * <ul>
 *     <li>Generating Property declarations/getters/setters using {@link PropertyGenerator}. This can be used to
 *     declare properties of a custom field type (as in the squidb-jackson-plugin) or to override default behavior
 *     of the basic column types. Plugins implementing this functionality should override
 *     {@link #hasPropertyGeneratorForField(VariableElement, DeclaredTypeName)} and
 *     {@link #getPropertyGenerator(VariableElement, DeclaredTypeName)}</li>
 *     <li>Adding imports required by whatever code the plugin generates by overriding {@link #addRequiredImports(Set)}.
 *     Nearly all plugins will probably need to override this method</li>
 *     <li>Adding interfaces for the generated model to implement (see {@link #addInterfacesToImplement(Set)})</li>
 *     <li>Write constant fields (see {@link #writeConstants(JavaFileWriter)})</li>
 *     <li>Write additional constructors (see {@link #writeConstructors(JavaFileWriter)})</li>
 *     <li>Write any additional methods (see {@link #writeMethods(JavaFileWriter)})</li>
 *     <li>Write any additional arbitrary code at the bottom of the generated class. This could include static or
 *     non-static initializer blocks, fields, methods, or anything else (see
 *     {@link #writeAdditionalCode(JavaFileWriter)})</li>
 * </ul>
 */
public class Plugin {

    protected final ModelSpec<?> modelSpec;
    protected final AptUtils utils;

    /**
     * All Plugins are constructed for a particular instance of a {@link ModelSpec}. The plugin should parse the given
     * spec to determine what if any code it should run in its overridden methods
     * @param modelSpec a {@link ModelSpec} representing the spec class
     * @param utils annotation processing utilities class
     */
    public Plugin(ModelSpec<?> modelSpec, AptUtils utils) {
        this.modelSpec = modelSpec;
        this.utils = utils;
    }

    /**
     * @param field a {@link VariableElement} field in a model spec representing a Property to be generated
     * @param fieldType the type name of the field
     * @return true if this plugin can create a {@link PropertyGenerator} for the given field
     */
    public boolean hasPropertyGeneratorForField(VariableElement field, DeclaredTypeName fieldType) {
        // Stub for subclasses to override
        return false;
    }

    /**
     * @param field a {@link VariableElement} field in a model spec representing a Property to be generated
     * @param fieldType the type name of the field
     * @return a {@link PropertyGenerator} for handling the given field
     */
    public PropertyGenerator getPropertyGenerator(VariableElement field, DeclaredTypeName fieldType) {
        // Stub for subclasses to override
        return null;
    }

    /**
     * Plugin subclasses can override this method to add any imports required by the code they generate to the generated
     * model class
     * @param imports an accumulator set of type names to import
     */
    public void addRequiredImports(Set<DeclaredTypeName> imports) {
        // Stub for subclasses to override
    }

    /**
     * Plugin subclasses can override this method to add any interfaces they want the generated model class to implement
     * @param interfaces an accumulator set of type names for interfaces to implement
     */
    public void addInterfacesToImplement(Set<DeclaredTypeName> interfaces) {
        // Stub for subclasses to override
    }

    /**
     * Called when emitting constant declarations (public static final fields) at the top of the generated model class.
     * Plugin subclasses can override to add their own constants
     * @param writer a {@link JavaFileWriter} for writing to
     * @throws IOException
     */
    public void writeConstants(JavaFileWriter writer) throws IOException {
        // Stub for subclasses to override
    }

    /**
     * Called when emitting constructors for the generated model class. Plugin subclasses can override to add their own
     * constructors
     * @param writer a {@link JavaFileWriter} for writing to
     * @throws IOException
     */
    public void writeConstructors(JavaFileWriter writer) throws IOException {
        // Stub for subclasses to override
    }

    /**
     * Called after emitting getters and setters for each property. Plugin subclasses can override to add any additional
     * methods
     * @param writer a {@link JavaFileWriter} for writing to
     * @throws IOException
     */
    public void writeMethods(JavaFileWriter writer) throws IOException {
        // Stub for subclasses to override
    }

    /**
     * Called before the generated class definition is finished. Plugin subclasses can override to write any additional
     * arbitrary code they want to
     * @param writer a {@link JavaFileWriter} for writing to
     * @throws IOException
     */
    public void writeAdditionalCode(JavaFileWriter writer) throws IOException {
        // Stub for subclasses to override
    }

}
