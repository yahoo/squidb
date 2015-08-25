/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.plugins;

import com.yahoo.aptutils.model.DeclaredTypeName;
import com.yahoo.aptutils.model.TypeName;
import com.yahoo.aptutils.utils.AptUtils;
import com.yahoo.squidb.processor.plugins.defaults.ConstructorPlugin;
import com.yahoo.squidb.processor.plugins.defaults.ImplementsPlugin;
import com.yahoo.squidb.processor.plugins.defaults.ModelMethodPlugin;
import com.yahoo.squidb.processor.plugins.properties.factory.InheritedModelPropertyGeneratorFactory;
import com.yahoo.squidb.processor.plugins.properties.factory.TablePropertyGeneratorFactory;
import com.yahoo.squidb.processor.plugins.properties.factory.ViewPropertyGeneratorFactory;
import com.yahoo.squidb.processor.plugins.properties.generators.PropertyGenerator;

import java.util.ArrayList;
import java.util.List;

import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;

public class PluginContext {

    private final AptUtils utils;
    private final int optionFlags;
    private List<Plugin> plugins = new ArrayList<Plugin>();

    public static final int OPTIONS_DISABLE_DEFAULT_CONSTRUCTORS = 1;
    public static final int OPTIONS_DISABLE_IMPLEMENTS_HANDLING = 1 << 1;
    public static final int OPTIONS_DISABLE_METHOD_HANDLING = 1 << 2;
    public static final int OPTIONS_DISABLE_DEFAULT_CONTENT_VALUES = 1 << 3;

    public static final int OPTIONS_PREFER_USER_PLUGINS = 1 << 31;

    public PluginContext(AptUtils utils, int optionFlags) {
        this.utils = utils;
        this.optionFlags = optionFlags;
        initializeDefaultPlugins();
    }

    private void initializeDefaultPlugins() {
        if (!getFlag(OPTIONS_DISABLE_DEFAULT_CONSTRUCTORS)) {
            plugins.add(new ConstructorPlugin(utils));
        }
        if (!getFlag(OPTIONS_DISABLE_IMPLEMENTS_HANDLING)) {
            plugins.add(new ImplementsPlugin(utils));
        }
        if (!getFlag(OPTIONS_DISABLE_METHOD_HANDLING)) {
            plugins.add(new ModelMethodPlugin(utils));
        }

        // Can't disable these, but they can be overridden by user plugins if the OPTIONS_PREFER_USER_PLUGIN flag is set
        plugins.add(new TablePropertyGeneratorFactory(utils));
        plugins.add(new ViewPropertyGeneratorFactory(utils));
        plugins.add(new InheritedModelPropertyGeneratorFactory(utils));
    }

    public boolean getFlag(int mask) {
        return (optionFlags & mask) > 0;
    }

    public void addPlugin(Plugin plugin) {
        if (getFlag(OPTIONS_PREFER_USER_PLUGINS)) {
            this.plugins.add(0, plugin);
        } else {
            this.plugins.add(plugin);
        }
    }

    public List<PluginWriter> getWritersForElement(TypeElement modelSpecElement, DeclaredTypeName modelSpecName,
            DeclaredTypeName generatedModelName) {
        List<PluginWriter> writers = new ArrayList<PluginWriter>();
        for (Plugin plugin : plugins) {
            List<? extends PluginWriter> pluginWriters = plugin.getWritersForElement(modelSpecElement, modelSpecName,
                    generatedModelName);
            if (pluginWriters != null) {
                writers.addAll(pluginWriters);
            }
        }
        return writers;
    }

    public PropertyGenerator getPropertyGeneratorForVariableElement(TypeElement modelSpecElement,
            VariableElement element, DeclaredTypeName generatedModelName) {
        TypeName type = utils.getTypeNameFromTypeMirror(element.asType());
        PropertyGenerator generator = null;
        if (type instanceof DeclaredTypeName) {
            generator = searchPluginsForPropertyGenerator(element, (DeclaredTypeName) type, generatedModelName,
                    modelSpecElement);
        }
        return generator;
    }

    private PropertyGenerator searchPluginsForPropertyGenerator(VariableElement element, DeclaredTypeName elementType,
            DeclaredTypeName generatedModelName, TypeElement modelSpecElement) {
        for (Plugin plugin : plugins) {
            if (plugin.hasPropertyGeneratorForField(element, elementType, modelSpecElement)) {
                PropertyGenerator generator = plugin.getPropertyGenerator(element, elementType, generatedModelName);
                if (generator != null) {
                    return generator;
                }
            }
        }
        return null;
    }

}
