/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.data;

import com.yahoo.squidb.annotations.InheritedModelSpec;
import com.yahoo.squidb.annotations.TableModelSpec;
import com.yahoo.squidb.annotations.ViewModelSpec;
import com.yahoo.squidb.processor.plugins.PluginEnvironment;

import javax.lang.model.element.TypeElement;

public final class ModelSpecFactory {

    /**
     * Creates an initializes a new ModelSpec instance for the given TypeElement. The TypeElement must be annotated
     * with one of {@link TableModelSpec}, {@link ViewModelSpec}, or {@link InheritedModelSpec}.
     */
    public static ModelSpec<?, ?> getModelSpecForElement(TypeElement typeElement, PluginEnvironment pluginEnv) {
        ModelSpec<?, ?> result;
        if (typeElement.getAnnotation(TableModelSpec.class) != null) {
            result = new TableModelSpecWrapper(typeElement, pluginEnv);
        } else if (typeElement.getAnnotation(ViewModelSpec.class) != null) {
            result = new ViewModelSpecWrapper(typeElement, pluginEnv);
        } else if (typeElement.getAnnotation(InheritedModelSpec.class) != null) {
            result = new InheritedModelSpecWrapper(typeElement, pluginEnv);
        } else {
            throw new IllegalStateException("No model spec annotation found on type element " + typeElement);
        }
        result.initialize();
        return result;
    }

}
