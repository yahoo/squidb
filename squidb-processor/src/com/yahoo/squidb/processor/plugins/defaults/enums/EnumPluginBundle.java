/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.plugins.defaults.enums;

import com.yahoo.squidb.processor.plugins.Plugin;
import com.yahoo.squidb.processor.plugins.PluginBundle;

import java.util.Arrays;

/**
 * Plugin that bundles the default Enum property handling
 */
public class EnumPluginBundle extends PluginBundle {

    public EnumPluginBundle() {
        super(Arrays.<Class<? extends Plugin>>asList(
                EnumFieldPlugin.class,
                EnumViewPropertyPlugin.class,
                EnumInheritedPropertyPlugin.class));
    }
}
