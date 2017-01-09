/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.json;

import com.yahoo.squidb.processor.plugins.Plugin;
import com.yahoo.squidb.processor.plugins.PluginBundle;

import java.util.Arrays;

/**
 * Plugin that bundles JSON field handling for all types of model specs
 */
public class JSONPlugin extends PluginBundle {

    public JSONPlugin() {
        super(Arrays.<Class<? extends Plugin>>asList(
                JSONFieldPlugin.class,
                JSONViewPropertyPlugin.class,
                JSONInheritedPropertyPlugin.class));
    }
}
