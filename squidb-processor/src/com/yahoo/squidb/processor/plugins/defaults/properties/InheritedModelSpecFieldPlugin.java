/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.plugins.defaults.properties;

import com.yahoo.squidb.processor.data.InheritedModelSpecWrapper;
import com.yahoo.squidb.processor.data.ModelSpec;
import com.yahoo.squidb.processor.plugins.PluginEnvironment;
import com.yahoo.squidb.processor.plugins.defaults.properties.generators.PropertyGenerator;

/**
 * This plugin controls generating property declarations, getters, and setters for fields in an inherited model. It can
 * create instances of {@link PropertyGenerator} for references to other Property subclasses (StringProperty,
 * LongProperty, etc.)
 */
public class InheritedModelSpecFieldPlugin extends FieldReferencePlugin {

    public InheritedModelSpecFieldPlugin(ModelSpec<?> modelSpec, PluginEnvironment pluginEnv) {
        super(modelSpec, pluginEnv);
    }

    @Override
    public boolean hasChangesForModelSpec() {
        return modelSpec instanceof InheritedModelSpecWrapper;
    }
}
