/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.json;

import com.yahoo.squidb.processor.data.ModelSpec;
import com.yahoo.squidb.processor.plugins.PluginBundle;
import com.yahoo.squidb.processor.plugins.PluginEnvironment;

import java.util.Arrays;

/**
 * Plugin that bundles JSON field handling for all types of model specs
 */
public class JSONPlugin extends PluginBundle {

    public JSONPlugin(ModelSpec<?> modelSpec, PluginEnvironment pluginEnv) {
        super(modelSpec, pluginEnv, Arrays.asList(new JSONFieldPlugin(modelSpec, pluginEnv),
                new JSONFieldReferencePlugin(modelSpec, pluginEnv)));
    }
}
