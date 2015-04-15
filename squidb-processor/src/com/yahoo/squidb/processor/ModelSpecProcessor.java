/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor;

import com.yahoo.aptutils.utils.AptUtils;
import com.yahoo.squidb.annotations.InheritedModelSpec;
import com.yahoo.squidb.annotations.TableModelSpec;
import com.yahoo.squidb.annotations.ViewModelSpec;
import com.yahoo.squidb.processor.properties.factory.PluggablePropertyGeneratorFactory;
import com.yahoo.squidb.processor.properties.factory.PropertyGeneratorFactory;
import com.yahoo.squidb.processor.writers.InheritedModelFileWriter;
import com.yahoo.squidb.processor.writers.ModelFileWriter;
import com.yahoo.squidb.processor.writers.TableModelFileWriter;
import com.yahoo.squidb.processor.writers.ViewModelFileWriter;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic.Kind;

/**
 * Annotation processor for generating boilerplate for subclasses of {@link com.yahoo.squidb.data.AbstractModel}
 * from simple template classes. Only processes classes annotated with one of the
 * model spec annotations ({@link TableModelSpec}, {@link ViewModelSpec}, or {@link InheritedModelSpec})
 *
 * Example template class:
 * <pre>
 * {@literal @}TableModelSpec(className="Person", tableName="people")
 * public class PersonSpec {
 *     String firstName;
 *
 *     String lastName;
 *
 *     {@literal @}PropertyExtras(columnName="creationDate")
 *     long birthday;
 *
 *     {@literal @}PropertyExtras(defaultValue="true")
 *     boolean isHappy;
 * }
 * </pre>
 */
@SupportedSourceVersion(SourceVersion.RELEASE_6)
public final class ModelSpecProcessor extends AbstractProcessor {

    private static final String PLUGINS_KEY = "squidbPlugins";
    private static final String SEPARATOR = ";";

    private AptUtils utils;
    private Filer filer;

    private PropertyGeneratorFactory propertyGeneratorFactory;

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> result = new HashSet<String>();
        result.add(TableModelSpec.class.getName());
        result.add(ViewModelSpec.class.getName());
        result.add(InheritedModelSpec.class.getName());
        return result;
    }

    @Override
    public synchronized void init(ProcessingEnvironment env) {
        super.init(env);

        utils = new AptUtils(env.getMessager(), env.getTypeUtils());
        filer = env.getFiler();

        propertyGeneratorFactory = new PropertyGeneratorFactory(utils);
        processOptionsForPlugins(env.getOptions());
    }

    private void processOptionsForPlugins(Map<String, String> options) {
        if (options != null) {
            String plugins = options.get(PLUGINS_KEY);
            if (!AptUtils.isEmpty(plugins)) {
                processPluginsString(plugins);
            }
        }
    }

    private void processPluginsString(String pluginsString) {
        String[] allPlugins = pluginsString.split(SEPARATOR);
        for (String plugin : allPlugins) {
            processSinglePlugin(plugin);
        }
    }

    private void processSinglePlugin(String plugin) {
        try {
            Class<?> pluggableFactory = Class.forName(plugin);
            if (PluggablePropertyGeneratorFactory.class.isAssignableFrom(pluggableFactory)) {
                PluggablePropertyGeneratorFactory factory = (PluggablePropertyGeneratorFactory) pluggableFactory
                        .getConstructor(AptUtils.class).newInstance(utils);
                propertyGeneratorFactory.registerPluggablePropertyGeneratorFactory(factory);
            } else {
                utils.getMessager().printMessage(Kind.WARNING,
                        "Plugin " + plugin + " is not a subclass of PluggablePropertyGeneratorFactory");
            }
        } catch (Exception e) {
            utils.getMessager().printMessage(Kind.WARNING, "Unable to instantiate plugin " + plugin + ", reason: " + e);
        }
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment env) {
        for (TypeElement annotationType : annotations) {
            for (Element element : env.getElementsAnnotatedWith(annotationType)) {
                if (element.getKind() == ElementKind.CLASS) {
                    TypeElement typeElement = (TypeElement) element;
                    try {
                        getFileWriter(typeElement).writeJava(filer);
                    } catch (IOException e) {
                        utils.getMessager().printMessage(Kind.ERROR, "Unable to write model file", element);
                    }
                } else {
                    utils.getMessager()
                            .printMessage(Kind.ERROR, "Unexpected element type " + element.getKind(), element);
                }
            }
        }

        return true;
    }

    private ModelFileWriter<?> getFileWriter(TypeElement typeElement) {
        if (typeElement.getAnnotation(TableModelSpec.class) != null) {
            return new TableModelFileWriter(typeElement, propertyGeneratorFactory, utils);
        } else if (typeElement.getAnnotation(ViewModelSpec.class) != null) {
            return new ViewModelFileWriter(typeElement, propertyGeneratorFactory, utils);
        } else if (typeElement.getAnnotation(InheritedModelSpec.class) != null) {
            return new InheritedModelFileWriter(typeElement, propertyGeneratorFactory, utils);
        } else {
            throw new IllegalStateException("No model spec annotation found on type element " + typeElement);
        }
    }

}
