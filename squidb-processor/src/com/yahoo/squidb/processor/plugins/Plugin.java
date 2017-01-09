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
 * has one or more corresponding hooks that plugins can take advantage of.
 * <p>
 * Implementations of Plugin must declare a public, no-argument constructor that the SquiDB code generator will
 * instantiate an instance of the plugin. After creating an instance, the plugin's
 * {@link #init(ModelSpec, PluginEnvironment)} method will be called to initialize the plugin with a ModelSpec instance
 * it will be operating on. If a plugin does not wish to handle that particular model spec, it can return false from
 * the init() method, and that plugin instance will be discarded/ignored while processing that model spec.
 * <p>
 * The phases of code generation and the corresponding hooks Plugins can use to add to or alter the generated code
 * are as follows:
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
 * {@link #beforeDeclareProperty(TypeSpec.Builder, PropertyGenerator, FieldSpec.Builder)} and
 * {@link #afterDeclareProperty(TypeSpec.Builder, PropertyGenerator, FieldSpec)} hooks to be notified immediately
 * before and after property declarations are added to the TypeSpec.Builder.</li>
 * <li>Constructor declaration. Plugins can add constructors using
 * {@link #declareMethodsOrConstructors(TypeSpec.Builder)} hook.</li>
 * <li>Accessors for the model properties. Plugins can use the
 * {@link #beforeDeclareGetter(TypeSpec.Builder, PropertyGenerator, MethodSpec.Builder)},
 * {@link #afterDeclareGetter(TypeSpec.Builder, PropertyGenerator, MethodSpec)},
 * {@link #beforeDeclareSetter(TypeSpec.Builder, PropertyGenerator, MethodSpec.Builder)}, and
 * {@link #afterDeclareSetter(TypeSpec.Builder, PropertyGenerator, MethodSpec)} hooks to be notified immediately before
 * and after accessors for a given Property are added to the TypeSpec.Builder.</li>
 * <li>Other methods. Plugins can add additional methods to the model using
 * {@link #declareMethodsOrConstructors(TypeSpec.Builder)}.</li>
 * <li>Any additional code. Plugins can add any code after all of these other things have been declared using
 * {@link #declareAdditionalJava(TypeSpec.Builder)}.</li>
 * </ol>
 * Most plugins will only need a one or two of these hooks, rather than all of them.
 * In order to avoid having to implement all methods, user plugins can extend the {@link AbstractPlugin} class, which
 * stubs all these hooks with default no-op implementations, overriding only the ones they need.
 */
public interface Plugin {

    /**
     * Called by the SquiDB code generator to initialize this plugin for a given {@link ModelSpec}. By returning true,
     * a Plugin instance is indicating that it was successfully initialized and wants to receive further callbacks for
     * the given ModelSpec. A Plugin can return false if it does not want to process the given ModelSpec, and it will
     * be discarded and not receive any further callbacks for the code generation process for that ModelSpec.
     * @param modelSpec a {@link ModelSpec} instance for which code is being generated
     * @param pluginEnv the {@link PluginEnvironment} that is instantiating this plugin
     * @return false if the plugin did not want to process or make any changes for the given model spec and should be
     * discarded, true otherwise
     */
    boolean init(ModelSpec<?, ?> modelSpec, PluginEnvironment pluginEnv);

    /**
     * Called once for each VariableElement (i.e. field) in the model spec. Plugins can interpret fields in whatever
     * way they choose, although generally they would be interpreted as a column/field in the generated table/view
     * and processed by adding a {@link PropertyGenerator} to the model spec. If any plugin returns true from this
     * method, that field is considered "claimed", and no further plugin will receive this callback for that field.
     *
     * @param field a {@link VariableElement} field in a model spec
     * @param fieldType the type name of the field
     * @return true if the processing of the variable element was claimed by this plugin and should not be processed
     * by other plugins
     */
    boolean processVariableElement(VariableElement field, TypeName fieldType);

    /**
     * Called after processing all variable elements in the model spec. Subclasses that want to add additional property
     * generators for properties not corresponding to a field in the model spec should override this method and do so
     * here by calling {@link ModelSpec#addPropertyGenerator(PropertyGenerator)} or
     * {@link ModelSpec#addDeprecatedPropertyGenerator(PropertyGenerator)}
     */
    void afterProcessVariableElements();

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
    TypeName getModelSuperclass();

    /**
     * Plugin subclasses can override this method to add any interfaces they want the generated model class to implement
     *
     * @param interfaces an accumulator set of type names for interfaces to implement
     */
    void addInterfacesToImplement(Set<TypeName> interfaces);

    /**
     * Called before adding any code to the generated model class. This hook is generally used for adding
     * javadocs or annotations to the generated class.
     *
     * @param builder the {@link TypeSpec.Builder} for the model class being built
     */
    void beforeBeginClassDeclaration(TypeSpec.Builder builder);

    /**
     * Called immediately before adding the static declaration for the given property. This hook would most often be
     * useful to do things like add annotations to the property to be generated.
     *
     * @param builder the {@link TypeSpec.Builder} for the model class being built
     * @param propertyGenerator contain metadata about the property to be written (name, type, etc.)
     * @param propertyDeclaration the {@link com.squareup.javapoet.FieldSpec.Builder} for the property to be generated.
     * This builder can be mutated to add annotations or javadocs
     */
    void beforeDeclareProperty(TypeSpec.Builder builder,
            PropertyGenerator propertyGenerator, FieldSpec.Builder propertyDeclaration);

    /**
     * Called immediately after adding the static declaration for the given property.
     *
     * @param builder the {@link TypeSpec.Builder} for the model class being built
     * @param propertyGenerator contain metadata about the property to be written (name, type, etc.)
     * @param propertyDeclaration the {@link com.squareup.javapoet.FieldSpec} for the property that was generated.
     */
    void afterDeclareProperty(TypeSpec.Builder builder,
            PropertyGenerator propertyGenerator, FieldSpec propertyDeclaration);

    /**
     * Called before adding the table schema static fields and blocks (property declarations, the Table/View object,
     * static initializer blocks, etc.)
     *
     * @param builder the {@link TypeSpec.Builder} for the model class being built
     */
    void beforeDeclareSchema(TypeSpec.Builder builder);

    /**
     * Called after adding the table schema static fields and blocks (property declarations, the Table/View object,
     * static initializer blocks, etc.)
     *
     * @param builder the {@link TypeSpec.Builder} for the model class being built
     */
    void afterDeclareSchema(TypeSpec.Builder builder);

    /**
     * Called immediately before adding a getter method for a property. Most often this hook would be useful for
     * annotating the generated method or altering the params in some way.
     *
     * @param builder the {@link TypeSpec.Builder} for the model class being built
     * @param propertyGenerator the {@link PropertyGenerator} causing this getter to be added
     * @param getterParams contains metadata about the method to be generated (name, return type, etc.)
     */
    void beforeDeclareGetter(TypeSpec.Builder builder, PropertyGenerator propertyGenerator,
            MethodSpec.Builder getterParams);

    /**
     * Called immediately after adding a getter method for a property.
     *
     * @param builder the {@link TypeSpec.Builder} for the model class being built
     * @param propertyGenerator the {@link PropertyGenerator} causing this getter to be added
     * @param getterParams contains metadata about the method that was generated (name, return type, etc.)
     */
    void afterDeclareGetter(TypeSpec.Builder builder, PropertyGenerator propertyGenerator,
            MethodSpec getterParams);

    /**
     * Called immediately before adding a setter method for a property. Most often this hook would be useful for
     * annotating the generated method or altering the params in some way.
     *
     * @param builder the {@link TypeSpec.Builder} for the model class being built
     * @param propertyGenerator the {@link PropertyGenerator} causing this setter to be adding
     * @param setterParams contains metadata about the method to be generated (name, return type, etc.)
     */
    void beforeDeclareSetter(TypeSpec.Builder builder, PropertyGenerator propertyGenerator,
            MethodSpec.Builder setterParams);

    /**
     * Called immediately after adding a setter method for a property.
     *
     * @param builder the {@link TypeSpec.Builder} for the model class being built
     * @param propertyGenerator the {@link PropertyGenerator} causing this setter to be adding
     * @param setterParams contains metadata about the method that was generated (name, return type, etc.)
     */
    void afterDeclareSetter(TypeSpec.Builder builder, PropertyGenerator propertyGenerator,
            MethodSpec setterParams);

    /**
     * Called to add method or constructor definitions.
     *
     * @param builder the {@link TypeSpec.Builder} for the model class being built
     */
    void declareMethodsOrConstructors(TypeSpec.Builder builder);

    /**
     * Called before the generated class definition is finished. Plugin subclasses can override to write any additional
     * arbitrary code they want to.
     *
     * @param builder the {@link TypeSpec.Builder} for the model class being built
     */
    void declareAdditionalJava(TypeSpec.Builder builder);

}
