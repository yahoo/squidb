/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.data;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import com.yahoo.squidb.annotations.Ignore;
import com.yahoo.squidb.processor.TypeConstants;
import com.yahoo.squidb.processor.plugins.PluginBundle;
import com.yahoo.squidb.processor.plugins.PluginEnvironment;
import com.yahoo.squidb.processor.plugins.defaults.properties.generators.interfaces.PropertyGenerator;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.tools.Diagnostic;

/**
 * Base class for data representing a model spec. This class holds the following pieces of information common to all
 * types of models (table models, view models, and inherited models):
 * <ul>
 * <li>The model spec annotation itself (see {@link #getSpecAnnotation()})</li>
 * <li>The {@link TypeElement} representing the model spec class (see {@link #getModelSpecElement()})</li>
 * <li>The name of the TypeElement (see {@link #getModelSpecName()})</li>
 * <li>The name of the class to be generated (see {@link #getGeneratedClassName()})</li>
 * <li>A list of {@link PropertyGenerator}s for the generated model's fields
 * (see {@link #getPropertyGenerators()})</li>
 * <li>A list of {@link PropertyGenerator}s for the generated model's deprecated fields
 * (see {@link #getDeprecatedPropertyGenerators()})</li>
 * </ul>
 * <p>
 * Plugins can also store arbitrary metadata in a model spec using {@link #putMetadata(String, Object)} and
 * {@link #getMetadata(String)}
 */
public abstract class ModelSpec<T extends Annotation, P extends PropertyGenerator> {

    protected final T modelSpecAnnotation;
    protected final ClassName generatedClassName;
    protected final ClassName modelSpecName;
    protected final TypeElement modelSpecElement;

    private final List<P> propertyGenerators = new ArrayList<>();
    private final List<P> deprecatedPropertyGenerators = new ArrayList<>();
    private final Map<String, Object> metadataMap = new HashMap<>();

    protected final PluginEnvironment pluginEnv;
    private PluginBundle pluginBundle;
    private TypeName modelSuperclass;

    private boolean isInitialized = false;

    private final List<ErrorInfo> loggedErrors = new ArrayList<>();

    public interface ModelSpecVisitor<RETURN, PARAMETER> {

        RETURN visitTableModel(TableModelSpecWrapper modelSpec, PARAMETER data);

        RETURN visitViewModel(ViewModelSpecWrapper modelSpec, PARAMETER data);

        RETURN visitInheritedModel(InheritedModelSpecWrapper modelSpec, PARAMETER data);
    }

    ModelSpec(TypeElement modelSpecElement, Class<T> modelSpecClass,
            PluginEnvironment pluginEnv) {
        this.modelSpecElement = modelSpecElement;
        this.modelSpecName = ClassName.get(modelSpecElement);
        this.modelSpecAnnotation = modelSpecElement.getAnnotation(modelSpecClass);
        this.generatedClassName = ClassName.get(modelSpecName.packageName(), getGeneratedClassNameString());
        this.pluginEnv = pluginEnv;
    }

    void initialize() {
        if (isInitialized) {
            throw new IllegalStateException("ModelSpec " + modelSpecElement + " has already been initialized");
        }
        pluginBundle = pluginEnv.getPluginBundleForModelSpec(this);
        modelSuperclass = initializeModelSuperclass();
        processVariableElements();
        pluginBundle.afterProcessVariableElements();
        isInitialized = true;
    }

    private TypeName initializeModelSuperclass() {
        TypeName pluginSuperclass = pluginBundle.getModelSuperclass();
        if (pluginSuperclass != null) {
            return pluginSuperclass;
        }
        return getDefaultModelSuperclass();
    }

    private void processVariableElements() {
        for (Element e : modelSpecElement.getEnclosedElements()) {
            if (e instanceof VariableElement && e.getAnnotation(Ignore.class) == null) {
                TypeName typeName = TypeName.get(e.asType());
                if (TypeConstants.isGenericType(typeName)) {
                    pluginEnv.getMessager().printMessage(Diagnostic.Kind.WARNING,
                            "Element type " + typeName + " is not a concrete type, will be ignored", e);
                } else if (!pluginBundle.processVariableElement((VariableElement) e, typeName)) {
                    // Deprecated things are generally ignored by plugins, so don't warn about them
                    // private static final fields are generally internal model spec constants, so don't warn about them
                    if (e.getAnnotation(Deprecated.class) == null &&
                            !e.getModifiers().containsAll(
                                    Arrays.asList(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL))) {
                        pluginEnv.getMessager().printMessage(Diagnostic.Kind.WARNING,
                                "No plugin found to handle field", e);
                    }
                }
            }
        }
    }

    public abstract <RETURN, PARAMETER> RETURN accept(ModelSpecVisitor<RETURN, PARAMETER> visitor, PARAMETER data);

    protected abstract String getGeneratedClassNameString();

    /**
     * @return true if this model spec has been initialized. If false, the fields in the model spec may not yet be
     * fully processed and the generated model superclass may not yet be determined.
     */
    public boolean isInitialized() {
        return isInitialized;
    }

    /**
     * @return the name of the default superclass for the generated model. This may be overridden by a plugin
     */
    protected abstract TypeName getDefaultModelSuperclass();

    /**
     * @return the name of the superclass for the generated model
     */
    public final TypeName getModelSuperclass() {
        return modelSuperclass;
    }

    /**
     * @return a {@link PluginBundle} for this model spec
     */
    public PluginBundle getPluginBundle() {
        return pluginBundle;
    }

    /**
     * @return the name of the model spec class
     */
    public ClassName getModelSpecName() {
        return modelSpecName;
    }

    /**
     * @return the name of the generated model class
     */
    public ClassName getGeneratedClassName() {
        return generatedClassName;
    }

    /**
     * @return the {@link TypeElement} for the model spec class
     */
    public TypeElement getModelSpecElement() {
        return modelSpecElement;
    }

    /**
     * @return the model spec annotation (e.g. an instance of {@link com.yahoo.squidb.annotations.TableModelSpec})
     */
    public T getSpecAnnotation() {
        return modelSpecAnnotation;
    }

    /**
     * @return a list of {@link PropertyGenerator}s for the fields in the generated model
     */
    public List<P> getPropertyGenerators() {
        return propertyGenerators;
    }

    /**
     * Add a {@link PropertyGenerator} to the model spec
     */
    public void addPropertyGenerator(P propertyGenerator) {
        propertyGenerators.add(propertyGenerator);
    }

    /**
     * @return a list of {@link PropertyGenerator}s for deprecated fields in the generated model
     */
    public List<P> getDeprecatedPropertyGenerators() {
        return deprecatedPropertyGenerators;
    }

    /**
     * Add a deprecated {@link PropertyGenerator} to the model spec
     */
    public void addDeprecatedPropertyGenerator(P propertyGenerator) {
        deprecatedPropertyGenerators.add(propertyGenerator);
    }

    /**
     * Attach arbitrary metadata to this model spec objects. Plugins can store metadata and then retrieve it later with
     * {@link #getMetadata(String)}
     *
     * @param metadataKey key for storing/retrieving the metadata
     * @param metadata the metadata to store
     * @see #hasMetadata(String)
     * @see #getMetadata(String)
     */
    public void putMetadata(String metadataKey, Object metadata) {
        metadataMap.put(metadataKey, metadata);
    }

    /**
     * @param metadataKey the metadata key to look up
     * @return true if there is metadata stored for the given key, false otherwise
     * @see #putMetadata(String, Object)
     * @see #getMetadata(String)
     */
    public boolean hasMetadata(String metadataKey) {
        return metadataMap.containsKey(metadataKey);
    }

    /**
     * Retrieve metadata that was previously attached with {@link #putMetadata(String, Object)}
     *
     * @param metadataKey key for storing/retrieving metadata
     * @return the metadata object for the given key if one was found, null otherwise
     * @see #putMetadata(String, Object)
     * @see #hasMetadata(String)
     */
    @SuppressWarnings("unchecked")
    public <TYPE> TYPE getMetadata(String metadataKey) {
        return (TYPE) metadataMap.get(metadataKey);
    }

    /**
     * Log an error to this model spec.
     *
     * This is generally intended for logging things like validation errors. Such errors do not stop the code
     * generation process (as logging an error using Messager and Kind.ERROR would), but instead generate
     * temporary code in the model class that will be picked up by a subsequent annotation processor and logged as
     * errors in a later round of annotation processing. This mechanism is designed to work around the fact that
     * logging Kind.ERROR messages during early rounds of annotation processing may suppress those errors, because
     * failing early during annotation processing can lead to a large number of "symbol not found" errors, which in
     * turn mask other validation errors.
     * <p>
     * If {@link PluginEnvironment#OPTIONS_USE_STANDARD_ERROR_LOGGING} is passed as an option to the code generator,
     * this SquiDB workaround is disabled and this method will log an error using a standard printMessage() call with
     * Kind.ERROR.
     *
     * @param message the error message to be logged
     * @param element the specific inner element in the model spec that is causing this error (e.g. a field or method),
     * or null for a general error
     */
    public void logError(String message, Element element) {
        if (pluginEnv.hasSquidbOption(PluginEnvironment.OPTIONS_USE_STANDARD_ERROR_LOGGING)) {
            pluginEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, message, element);
        } else {
            boolean isRootElement = element == null || element.equals(getModelSpecElement());
            loggedErrors.add(new ErrorInfo(getModelSpecName(),
                    isRootElement ? "" : element.getSimpleName().toString(), message));
        }
    }

    /**
     * @return the list of errors logged to this model spec
     */
    public List<ErrorInfo> getLoggedErrors() {
        return loggedErrors;
    }
}
