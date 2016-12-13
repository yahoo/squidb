/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.plugins;

import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.yahoo.squidb.processor.data.ModelSpec;
import com.yahoo.squidb.processor.plugins.defaults.properties.generators.interfaces.PropertyGenerator;

import java.util.Set;

import javax.lang.model.element.VariableElement;

/**
 * Generated model classes are built using a {@link com.squareup.javapoet.TypeSpec.Builder JavaPoet TypeSpec.Builder}.
 * The Plugin class defines several hooks at various points during the building process, where new code can be
 * added to the generated model class. Generated model classes have several distinct sections of code, each of which
 * has one or more corresponding hooks that plugins can take advantage of:
 * <ol>
 * <li>The class declaration itself. Plugins can add javadocs or annotations using
 * {@link #beforeBeginClassDeclaration(TypeSpec.Builder)}, declare interfaces for the model to implement using
 * {@link #addInterfacesToImplement(Set)}, or declare a custom superclass for the model using
 * {@link #getModelSuperclass()}</li>
 * <li>The model schema declaration. This section consists of several static final fields declaring things like
 * the Table object and Property declarations, followed by initializer blocks to initialize these fields if
 * necessary and in the correct order. The corresponding plugin hooks are {@link #beforeDeclareSchema(TypeSpec.Builder)}
 * and {@link #afterDeclareSchema(TypeSpec.Builder)}. Static fields added in these hooks will appear before or after
 * the schema's static fields (depending on which hook is used). Static code blocks added in these hooks will
 * similarly appear before or after the schema initialization static blocks. Plugins can also use the
 * {@link #willDeclareProperty(TypeSpec.Builder, PropertyGenerator, FieldSpec.Builder)} and
 * {@link #didDeclareProperty(TypeSpec.Builder, PropertyGenerator, FieldSpec)} hooks to be notified immediately
 * before and after property declarations are added to the TypeSpec.Builder.</li>
 * <li>Constructor declaration. Plugins can add constructors using
 * {@link #declareMethodsOrConstructors(TypeSpec.Builder)} hook.</li>
 * <li>Accessors for the model properties. Plugins can use the
 * {@link #willDeclareGetter(TypeSpec.Builder, PropertyGenerator, MethodSpec.Builder)},
 * {@link #didDeclareGetter(TypeSpec.Builder, PropertyGenerator, MethodSpec)},
 * {@link #willDeclareSetter(TypeSpec.Builder, PropertyGenerator, MethodSpec.Builder)}, and
 * {@link #didDeclareSetter(TypeSpec.Builder, PropertyGenerator, MethodSpec)} hooks to be notified immediately before
 * and after accessors for a given Property are added to the TypeSpec.Builder.</li>
 * <li>Other methods. Plugins can add additional methods to the model using
 * {@link #declareMethodsOrConstructors(TypeSpec.Builder)}.</li>
 * <li>Any additional code. Plugins can add any code after all of these other things have been declared using
 * {@link #declareAdditionalJava(TypeSpec.Builder)}.</li>
 * </ol>
 */
public class Plugin {

    protected final ModelSpec<?, ?> modelSpec;
    protected final PluginEnvironment pluginEnv;

    /**
     * All Plugins are constructed for a particular instance of a {@link ModelSpec}. The plugin should parse the given
     * spec to determine what if any code it should run in its overridden methods
     *
     * @param modelSpec a {@link ModelSpec} representing the spec class
     * @param pluginEnv an object representing the current environment the plugin is running in. Plugins can call
     * methods like {@link PluginEnvironment#getProcessingEnvironment()}for annotation processing helpers or
     * {@link PluginEnvironment#getEnvOptions()} to read environment options
     */
    public Plugin(ModelSpec<?, ?> modelSpec, PluginEnvironment pluginEnv) {
        this.modelSpec = modelSpec;
        this.pluginEnv = pluginEnv;
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
    public boolean processVariableElement(VariableElement field, TypeName fieldType) {
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
    public TypeName getModelSuperclass() {
        // Stub for subclasses to override
        return null;
    }

    /**
     * Plugin subclasses can override this method to add any interfaces they want the generated model class to implement
     *
     * @param interfaces an accumulator set of type names for interfaces to implement
     */
    public void addInterfacesToImplement(Set<TypeName> interfaces) {
        // Stub for subclasses to override
    }

    /**
     * Called before adding any code to the generated model class. This hook is generally used for adding
     * javadocs or annotations to the generated class.
     *
     * @param builder the {@link TypeSpec.Builder} for the model class being built
     */
    public void beforeBeginClassDeclaration(TypeSpec.Builder builder) {
        // Stub for subclasses to override
    }

    /**
     * Called before adding the static declaration for the given property. This hook would most often be useful
     * to do things like add annotations to the property to be generated
     *
     * @param builder the {@link TypeSpec.Builder} for the model class being built
     * @param propertyGenerator contain metadata about the property to be written (name, type, etc.)
     * @param propertyDeclaration the {@link com.squareup.javapoet.FieldSpec.Builder} for the property to be generated.
     * This builder can be mutated to add annotations or javadocs
     */
    public void willDeclareProperty(TypeSpec.Builder builder,
            PropertyGenerator propertyGenerator, FieldSpec.Builder propertyDeclaration) {
        // Stub for subclasses to override
    }

    /**
     * Called after adding the static declaration for the given property
     *
     * @param builder the {@link TypeSpec.Builder} for the model class being built
     * @param propertyGenerator contain metadata about the property to be written (name, type, etc.)
     * @param propertyDeclaration the {@link com.squareup.javapoet.FieldSpec} for the property that was generated.
     */
    public void didDeclareProperty(TypeSpec.Builder builder,
            PropertyGenerator propertyGenerator, FieldSpec propertyDeclaration) {
        // Stub for subclasses to override
    }

    /**
     * Called before adding the table schema static fields and blocks (property declarations, the Table/View object,
     * static initializer blocks, etc.)
     *
     * @param builder the {@link TypeSpec.Builder} for the model class being built
     */
    public void beforeDeclareSchema(TypeSpec.Builder builder) {
        // Stub for subclasses to override
    }

    /**
     * Called after adding the table schema static fields and blocks (property declarations, the Table/View object,
     * static initializer blocks, etc.)
     *
     * @param builder the {@link TypeSpec.Builder} for the model class being built
     */
    public void afterDeclareSchema(TypeSpec.Builder builder) {
        // Stub for subclasses to override
    }

    /**
     * Called before adding a getter method for a property. Most often this hook would be useful for annotating the
     * generated method or altering the params in some way.
     *
     * @param builder the {@link TypeSpec.Builder} for the model class being built
     * @param propertyGenerator the {@link PropertyGenerator} causing this getter to be added
     * @param getterParams contains metadata about the method to be generated (name, return type, etc.)
     */
    public void willDeclareGetter(TypeSpec.Builder builder, PropertyGenerator propertyGenerator,
            MethodSpec.Builder getterParams) {
        // Stub for subclasses to override
    }

    /**
     * Called after adding a getter method for a property.
     *
     * @param builder the {@link TypeSpec.Builder} for the model class being built
     * @param propertyGenerator the {@link PropertyGenerator} causing this getter to be added
     * @param getterParams contains metadata about the method that was generated (name, return type, etc.)
     */
    public void didDeclareGetter(TypeSpec.Builder builder, PropertyGenerator propertyGenerator,
            MethodSpec getterParams) {
        // Stub for subclasses to override
    }

    /**
     * Called before adding a setter method for a property. Most often this hook would be useful for annotating the
     * generated method or altering the params in some way.
     *
     * @param builder the {@link TypeSpec.Builder} for the model class being built
     * @param propertyGenerator the {@link PropertyGenerator} causing this setter to be adding
     * @param setterParams contains metadata about the method to be generated (name, return type, etc.)
     */
    public void willDeclareSetter(TypeSpec.Builder builder, PropertyGenerator propertyGenerator,
            MethodSpec.Builder setterParams) {
        // Stub for subclasses to override
    }

    /**
     * Called after adding a setter method for a property.
     *
     * @param builder the {@link TypeSpec.Builder} for the model class being built
     * @param propertyGenerator the {@link PropertyGenerator} causing this setter to be adding
     * @param setterParams contains metadata about the method that was generated (name, return type, etc.)
     */
    public void didDeclareSetter(TypeSpec.Builder builder, PropertyGenerator propertyGenerator,
            MethodSpec setterParams) {
        // Stub for subclasses to override
    }

    /**
     * Called to add method or constructor definitions.
     *
     * @param builder the {@link TypeSpec.Builder} for the model class being built
     */
    public void declareMethodsOrConstructors(TypeSpec.Builder builder) {
        // Stub for subclasses to override
    }

    /**
     * Called before the generated class definition is finished. Plugin subclasses can override to write any additional
     * arbitrary code they want to.
     *
     * @param builder the {@link TypeSpec.Builder} for the model class being built
     */
    public void declareAdditionalJava(TypeSpec.Builder builder) {
        // Stub for subclasses to override
    }

}
