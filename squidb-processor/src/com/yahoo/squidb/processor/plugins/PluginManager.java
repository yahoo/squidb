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
import com.yahoo.squidb.processor.plugins.defaults.properties.InheritedModelSpecFieldPlugin;
import com.yahoo.squidb.processor.plugins.defaults.properties.TableModelSpecFieldPlugin;
import com.yahoo.squidb.processor.plugins.defaults.properties.ViewModelSpecFieldPlugin;
import com.yahoo.squidb.processor.plugins.defaults.properties.generators.PropertyGenerator;

import java.util.ArrayList;
import java.util.List;

import javax.tools.Diagnostic;

/**
 * This class maintains a list of known/enabled {@link Plugin} classes. Plugins available by default include
 * {@link ConstructorPlugin} for generating model constructors, {@link ImplementsPlugin} for allowing models to
 * implement interfaces, {@link ModelMethodPlugin} for copying methods from the model spec to the model, and the three
 * property generator plugins ({@link TableModelSpecFieldPlugin}, {@link ViewModelSpecFieldPlugin}, and
 * {@link InheritedModelSpecFieldPlugin}).
 * <p>
 * This class also manages option flags for plugins. The default plugins for constructors, interfaces, and model
 * methods can all be disabled using a flag. Other flags allow disabling default content values, disabling the
 * convenience getters and setters that accompany each property, and preferring user-defined plugins to the default
 * plugins. Options are passed as a bitmask to the processor using the key "squidbOptions".
 */
public class PluginManager {

    private final AptUtils utils;
    private final int optionFlags;
    private List<Class<? extends Plugin>> pluginClasses = new ArrayList<Class<? extends Plugin>>();

    /**
     * Flag for preferring user plugins to the default plugins. This applies only to plugins that create
     * {@link PropertyGenerator}s. Set this flag if for example
     * if you want to provide a property generator that does custom StringProperty handling or otherwise override
     * the default property generation.
     */
    public static final int OPTIONS_PREFER_USER_PLUGINS = 1;

    /**
     * Flag for disabling the default constructors generated in each model class
     */
    public static final int OPTIONS_DISABLE_DEFAULT_CONSTRUCTORS = 1 << 1;

    /**
     * Flag for disabling processing of the {@link com.yahoo.squidb.annotations.Implements} annotation for declaring
     * that models implement interfaces
     */
    public static final int OPTIONS_DISABLE_IMPLEMENTS_HANDLING = 1 << 2;

    /**
     * Flag for disabling the copying of static methods and model methods from the spec to the model class
     */
    public static final int OPTIONS_DISABLE_METHOD_HANDLING = 1 << 3;

    /**
     * Flag for disabling the in-memory default content values used as for fallback values in empty models
     */
    public static final int OPTIONS_DISABLE_DEFAULT_CONTENT_VALUES = 1 << 4;

    /**
     * Flag for disabling the convenience getters and setters generated with each property
     */
    public static final int OPTIONS_DISABLE_GETTERS_AND_SETTERS = 1 << 5;

    /**
     * @param utils annotation processing utilities class
     * @param optionFlags option flags for disabling default plugins
     */
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
        pluginClasses.add(TableModelSpecFieldPlugin.class);
        pluginClasses.add(ViewModelSpecFieldPlugin.class);
        pluginClasses.add(InheritedModelSpecFieldPlugin.class);
    }

    /**
     * @param flag the flag to check
     * @return true if the flag is set in the options bitmask, false otherwise
     */
    public boolean getFlag(int flag) {
        return (optionFlags & flag) > 0;
    }

    /**
     * Add a {@link Plugin} class to the list of known plugins
     *
     * @param plugin the plugin class
     */
    public void addPlugin(Class<? extends Plugin> plugin) {
        if (getFlag(OPTIONS_PREFER_USER_PLUGINS)) {
            this.pluginClasses.add(0, plugin);
        } else {
            this.pluginClasses.add(plugin);
        }
    }

    /**
     * @param modelSpec the model spec the Plugins will be instantiated for
     * @return a new {@link PluginBundle} containing Plugins initialized to handle the given model spec
     */
    public PluginBundle getPluginBundleForModelSpec(ModelSpec<?> modelSpec) {
        List<Plugin> plugins = new ArrayList<Plugin>();
        for (Class<? extends Plugin> plugin : pluginClasses) {
            try {
                plugins.add(plugin.getConstructor(ModelSpec.class, AptUtils.class).newInstance(modelSpec, utils));
            } catch (Exception e) {
                utils.getMessager().printMessage(Diagnostic.Kind.WARNING,
                        "Unable to instantiate plugin " + plugin + ", reason: " + e);
            }
        }
        return new PluginBundle(plugins, utils);
    }
}
