/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.plugins;

import com.yahoo.aptutils.utils.AptUtils;
import com.yahoo.squidb.processor.data.ModelSpec;
import com.yahoo.squidb.processor.plugins.defaults.ConstructorPlugin;
import com.yahoo.squidb.processor.plugins.defaults.ImplementsPlugin;
import com.yahoo.squidb.processor.plugins.defaults.ModelMethodPlugin;
import com.yahoo.squidb.processor.plugins.properties.factory.InheritedModelPropertyGeneratorFactory;
import com.yahoo.squidb.processor.plugins.properties.factory.TablePropertyGeneratorFactory;
import com.yahoo.squidb.processor.plugins.properties.factory.ViewPropertyGeneratorFactory;

import java.util.ArrayList;
import java.util.List;

import javax.tools.Diagnostic;

public class PluginManager {

    private final AptUtils utils;
    private final int optionFlags;
    private List<Class<? extends Plugin>> pluginClasses = new ArrayList<Class<? extends Plugin>>();

    public static final int OPTIONS_DISABLE_DEFAULT_CONSTRUCTORS = 1;
    public static final int OPTIONS_DISABLE_IMPLEMENTS_HANDLING = 1 << 1;
    public static final int OPTIONS_DISABLE_METHOD_HANDLING = 1 << 2;
    public static final int OPTIONS_DISABLE_DEFAULT_CONTENT_VALUES = 1 << 3;
    public static final int OPTIONS_DISABLE_GETTERS_AND_SETTERS = 1 << 4;

    public static final int OPTIONS_PREFER_USER_PLUGINS = 1 << 31;

    public PluginManager(AptUtils utils, int optionFlags) {
        this.utils = utils;
        this.optionFlags = optionFlags;
        initializeDefaultPlugins();
    }

    private void initializeDefaultPlugins() {
        if (!getFlag(OPTIONS_DISABLE_DEFAULT_CONSTRUCTORS)) {
            pluginClasses.add(ConstructorPlugin.class);
        }
        if (!getFlag(OPTIONS_DISABLE_IMPLEMENTS_HANDLING)) {
            pluginClasses.add(ImplementsPlugin.class);
        }
        if (!getFlag(OPTIONS_DISABLE_METHOD_HANDLING)) {
            pluginClasses.add(ModelMethodPlugin.class);
        }

        // Can't disable these, but they can be overridden by user plugins if the OPTIONS_PREFER_USER_PLUGIN flag is set
        pluginClasses.add(TablePropertyGeneratorFactory.class);
        pluginClasses.add(ViewPropertyGeneratorFactory.class);
        pluginClasses.add(InheritedModelPropertyGeneratorFactory.class);
    }

    public boolean getFlag(int mask) {
        return (optionFlags & mask) > 0;
    }

    public void addPlugin(Class<? extends Plugin> plugin) {
        if (getFlag(OPTIONS_PREFER_USER_PLUGINS)) {
            this.pluginClasses.add(0, plugin);
        } else {
            this.pluginClasses.add(plugin);
        }
    }

    public PluginContext getPluginContextForModelSpec(ModelSpec<?> modelSpec) {
        List<Plugin> plugins = new ArrayList<Plugin>();
        for (Class<? extends Plugin> plugin : pluginClasses) {
            try {
                plugins.add(plugin.getConstructor(ModelSpec.class, AptUtils.class).newInstance(modelSpec, utils));
            } catch (Exception e) {
                utils.getMessager().printMessage(Diagnostic.Kind.WARNING,
                        "Unable to instantiate plugin " + plugin + ", reason: " + e);
            }
        }
        return new PluginContext(plugins, utils);
    }
}
