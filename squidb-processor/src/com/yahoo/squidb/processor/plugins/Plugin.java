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
import com.yahoo.squidb.processor.plugins.defaults.properties.generators.PropertyGenerator;

import java.io.IOException;
import java.util.Set;

import javax.lang.model.element.VariableElement;

/**
 * The Plugin class defines several hooks for plugins to add code to a generated model class. Code generation for a
 * model can be broken down into several distinct phases, each of which has one or more corresponding hooks available
 * to plugins:
 * <ol>
 * <li>Declare imports. Plugins can add imports using {@link #addRequiredImports(Set)}</li>
 * <li>Begin class declaration. Much of this is fixed, but plugins can add interfaces for the generated class to
 * implement using {@link #addInterfacesToImplement(Set)}</li>
 * <li>Emitting the table or view schema. This includes property declarations, the Table or View object for the
 * model, etc. Plugins can generate code before or after this phase using {@link #beforeEmitSchema(JavaFileWriter)}
 * and {@link #afterEmitSchema(JavaFileWriter)}</li>
 * <li>Emitting constructors. Plugins can add constructors using {@link #emitConstructors(JavaFileWriter)}. Note
 * that several default constructors and a clone() method are emitted by a built-in plugin.</li>
 * <li>Emitting methods. Plugins can emit code before methods are declared
 * ({@link #beforeEmitMethods(JavaFileWriter)}), emit methods ({@link #emitMethods(JavaFileWriter)}), or emit code
 * after methods are emitted ({@link #afterEmitMethods(JavaFileWriter)})</li>
 * <li>Emit any other helper code. This is the final phase of code generation before the class definition is closed.
 * Plugins can generate arbitrary code here using {@link #emitAdditionalJava(JavaFileWriter)}</li>
 * </ol>
 */
public class Plugin {

    protected final ModelSpec<?> modelSpec;
    protected final PluginEnvironment pluginEnv;
    protected final AptUtils utils;

    /**
     * All Plugins are constructed for a particular instance of a {@link ModelSpec}. The plugin should parse the given
     * spec to determine what if any code it should run in its overridden methods
     *
     * @param modelSpec a {@link ModelSpec} representing the spec class
     * @param pluginEnv an object representing the current environment the plugin is running in. Plugins can call
     * methods like {@link PluginEnvironment#getUtils()} for annotation processing helpers or
     * {@link PluginEnvironment#getEnvOptions()} to read environment options
     */
    public Plugin(ModelSpec<?> modelSpec, PluginEnvironment pluginEnv) {
        this.modelSpec = modelSpec;
        this.pluginEnv = pluginEnv;
        this.utils = pluginEnv.getUtils();
    }

    /**
     * @return true if this plugin may make any changes during code generation for this model spec. The default is true;
     * plugins like the default property generator plugins may return false if the model spec is of a different kind
     * than the kind they handle. If this method returns false, none of the other methods in this plugin will be called
     * during code generation for this model
     */
    public boolean hasChangesForModelSpec() {
        // Subclasses can override if they want to ignore this entire model spec
        return true;
    }

    /**
     * @param field a {@link VariableElement} field in a model spec
     * @param fieldType the type name of the field
     * @return true if the processing of the variable element was claimed by this plugin and should not be processed
     * by other plugins
     */
    public boolean processVariableElement(VariableElement field, DeclaredTypeName fieldType) {
        // Stub for subclasses to override
        return false;
    }

    /**
     * Called after processing all variable elements in the model spec. Subclasses that want to add additional property
     * generators for properties not corresponding to a field in the model spec should override this method and do so
     * here by calling {@link ModelSpec#addPropertyGenerator(PropertyGenerator)} or
     * {@link ModelSpec#addDeprecatedPropertyGenerator(PropertyGenerator)}
     */
    public void afterProcessVariableElements() {
        // Stub for subclasses to override
    }

    /**
     * Plugin subclasses can override this method to declare a custom superclass for the generated model to extend.
     * Only one plugin may specify a superclass for any given model, so the first plugin to return a non-null value will
     * take priority. If no plugins return a non-null value, the default superclass for that model type will be used.
     * <p>
     * Note that the returned superclass should be of an appropriate type for that kind of model -- e.g. for table
     * models, the superclass should itself be a subclass of TableModel, etc. Users can use
     * {@link com.yahoo.squidb.processor.data.ModelSpec.ModelSpecVisitor} as one way to determine what kind of model
     * spec the plugin is currently operating on.
     *
     * @return the name of a class to use as the model superclass, or null if N/A
     */
    public DeclaredTypeName getModelSuperclass() {
        // Stub for subclasses to override
        return null;
    }

    /**
     * Plugin subclasses can override this method to add any imports required by the code they generate to the generated
     * model class
     *
     * @param imports an accumulator set of type names to import
     */
    public void addRequiredImports(Set<DeclaredTypeName> imports) {
        // Stub for subclasses to override
    }

    /**
     * Plugin subclasses can override this method to add any interfaces they want the generated model class to implement
     *
     * @param interfaces an accumulator set of type names for interfaces to implement
     */
    public void addInterfacesToImplement(Set<DeclaredTypeName> interfaces) {
        // Stub for subclasses to override
    }

    /**
     * Called before emitting the table schema (property declarations, the Table/View object, etc.). This is essentially
     * the top of the file before anything model-specific exists.
     *
     * @param writer a {@link JavaFileWriter} for writing to
     */
    public void beforeEmitSchema(JavaFileWriter writer) throws IOException {
        // Stub for subclasses to override
    }

    /**
     * Called after emitting the table schema (property declarations, the Table/View object, etc.)
     *
     * @param writer a {@link JavaFileWriter} for writing to
     */
    public void afterEmitSchema(JavaFileWriter writer) throws IOException {
        // Stub for subclasses to override
    }

    /**
     * Called to emit constructors for the generated model class. Plugin subclasses can override to add their own
     * constructors
     *
     * @param writer a {@link JavaFileWriter} for writing to
     */
    public void emitConstructors(JavaFileWriter writer) throws IOException {
        // Stub for subclasses to override
    }

    /**
     * Called before emitting methods. Plugin subclasses can generate arbitrary code here. This is called after
     * emitting constructors
     *
     * @param writer a {@link JavaFileWriter} for writing to
     */
    public void beforeEmitMethods(JavaFileWriter writer) throws IOException {
        // Stub for subclasses to override
    }

    /**
     * Called to emit methods. Plugin subclasses can override to add methods to the model definition
     *
     * @param writer a {@link JavaFileWriter} for writing to
     */
    public void emitMethods(JavaFileWriter writer) throws IOException {
        // Stub for subclasses to override
    }

    /**
     * Called after emitting methods. Plugin subclasses can generate arbitrary code here. This is called after
     * emitting methods
     *
     * @param writer a {@link JavaFileWriter} for writing to
     */
    public void afterEmitMethods(JavaFileWriter writer) throws IOException {
        // Stub for subclasses to override
    }

    /**
     * Called before the generated class definition is finished. Plugin subclasses can override to write any additional
     * arbitrary code they want to
     *
     * @param writer a {@link JavaFileWriter} for writing to
     */
    public void emitAdditionalJava(JavaFileWriter writer) throws IOException {
        // Stub for subclasses to override
    }

}
