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
import com.yahoo.squidb.processor.plugins.PluginEnvironment;
import com.yahoo.squidb.processor.writers.InheritedModelFileWriter;
import com.yahoo.squidb.processor.writers.ModelFileWriter;
import com.yahoo.squidb.processor.writers.TableModelFileWriter;
import com.yahoo.squidb.processor.writers.ViewModelFileWriter;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic.Kind;

/**
 * Annotation processor for generating boilerplate for subclasses of AbstractModel
 * from simple template classes. Only processes classes annotated with one of the
 * model spec annotations ({@link TableModelSpec}, {@link ViewModelSpec}, or {@link InheritedModelSpec})
 *
 * Example template class:
 * <pre>
 * &#064;TableModelSpec(className="Person", tableName="people")
 * public class PersonSpec {
 *     String firstName;
 *
 *     String lastName;
 *
 *     &#064;ColumnSpec(name="creationDate")
 *     long birthday;
 *
 *     &#064;ColumnSpec(defaultValue="true")
 *     boolean isHappy;
 * }
 * </pre>
 */
public final class ModelSpecProcessor extends AbstractProcessor {

    private Set<String> supportedAnnotationTypes = new HashSet<>();

    private AptUtils utils;
    private PluginEnvironment pluginEnv;

    public ModelSpecProcessor() {
        supportedAnnotationTypes.add(TableModelSpec.class.getName());
        supportedAnnotationTypes.add(ViewModelSpec.class.getName());
        supportedAnnotationTypes.add(InheritedModelSpec.class.getName());
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return supportedAnnotationTypes;
    }

    @Override
    public Set<String> getSupportedOptions() {
        Set<String> supportedOptions = new HashSet<>();
        supportedOptions.add(PluginEnvironment.PLUGINS_KEY);
        supportedOptions.add(PluginEnvironment.OPTIONS_KEY);
        supportedOptions.addAll(pluginEnv.getPluginSupportedOptions());
        return Collections.unmodifiableSet(supportedOptions);
    }

    @Override
    public synchronized void init(ProcessingEnvironment env) {
        super.init(env);
        utils = new AptUtils(env);
        pluginEnv = new PluginEnvironment(utils, env.getOptions());
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment env) {
        for (TypeElement annotationType : annotations) {
            if (supportedAnnotationTypes.contains(annotationType.getQualifiedName().toString())) {
                for (Element element : env.getElementsAnnotatedWith(annotationType)) {
                    if (element.getKind() == ElementKind.CLASS) {
                        TypeElement typeElement = (TypeElement) element;
                        try {
                            getFileWriter(typeElement).writeJava();
                        } catch (IOException e) {
                            utils.getMessager().printMessage(Kind.ERROR, "Unable to write model file", element);
                        }
                    } else {
                        utils.getMessager()
                                .printMessage(Kind.ERROR, "Unexpected element type " + element.getKind(), element);
                    }
                }
            } else {
                utils.getMessager().printMessage(Kind.WARNING,
                        "Skipping unsupported annotation received by processor: " + annotationType);
            }
        }

        return true;
    }

    private ModelFileWriter<?> getFileWriter(TypeElement typeElement) {
        if (typeElement.getAnnotation(TableModelSpec.class) != null) {
            return new TableModelFileWriter(typeElement, pluginEnv, utils);
        } else if (typeElement.getAnnotation(ViewModelSpec.class) != null) {
            return new ViewModelFileWriter(typeElement, pluginEnv, utils);
        } else if (typeElement.getAnnotation(InheritedModelSpec.class) != null) {
            return new InheritedModelFileWriter(typeElement, pluginEnv, utils);
        } else {
            throw new IllegalStateException("No model spec annotation found on type element " + typeElement);
        }
    }

}
