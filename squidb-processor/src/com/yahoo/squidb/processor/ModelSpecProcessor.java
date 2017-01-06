/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor;

import com.yahoo.squidb.annotations.InheritedModelSpec;
import com.yahoo.squidb.annotations.TableModelSpec;
import com.yahoo.squidb.annotations.ViewModelSpec;
import com.yahoo.squidb.processor.data.InheritedModelSpecWrapper;
import com.yahoo.squidb.processor.data.ModelSpec;
import com.yahoo.squidb.processor.data.ModelSpecFactory;
import com.yahoo.squidb.processor.data.TableModelSpecWrapper;
import com.yahoo.squidb.processor.data.ViewModelSpecWrapper;
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
        pluginEnv = new PluginEnvironment(env);
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
                            processingEnv.getMessager().printMessage(Kind.ERROR,
                                    "Unable to write model file", element);
                        }
                    } else {
                        processingEnv.getMessager()
                                .printMessage(Kind.ERROR, "Unexpected element type " + element.getKind(), element);
                    }
                }
            } else {
                processingEnv.getMessager().printMessage(Kind.WARNING,
                        "Skipping unsupported annotation received by processor: " + annotationType);
            }
        }

        return true;
    }

    private ModelFileWriter<?> getFileWriter(TypeElement typeElement) {
        return ModelSpecFactory.getModelSpecForElement(typeElement, pluginEnv)
            .accept(new ModelSpec.ModelSpecVisitor<ModelFileWriter<?>, Void>() {
                @Override
                public ModelFileWriter<?> visitTableModel(TableModelSpecWrapper modelSpec, Void data) {
                    return new TableModelFileWriter(modelSpec, pluginEnv);
                }

                @Override
                public ModelFileWriter<?> visitViewModel(ViewModelSpecWrapper modelSpec, Void data) {
                    return new ViewModelFileWriter(modelSpec, pluginEnv);
                }

                @Override
                public ModelFileWriter<?> visitInheritedModel(InheritedModelSpecWrapper modelSpec, Void data) {
                    return new InheritedModelFileWriter(modelSpec, pluginEnv);
                }
            }, null);
    }

}
