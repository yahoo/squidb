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
import com.yahoo.squidb.processor.properties.factory.InheritedModelPropertyGeneratorFactory;
import com.yahoo.squidb.processor.properties.factory.TablePropertyGeneratorFactory;
import com.yahoo.squidb.processor.properties.factory.ViewPropertyGeneratorFactory;
import com.yahoo.squidb.processor.properties.generators.PropertyGenerator;

import java.util.ArrayList;
import java.util.List;

import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;

public class PluginContext {

    private final AptUtils utils;
    private List<Plugin> plugins = new ArrayList<Plugin>();

    public PluginContext(AptUtils utils) {
        this.utils = utils;
        plugins.add(new ConstructorPlugin(utils));
        plugins.add(new ImplementsPlugin(utils));
        plugins.add(new ModelMethodPlugin(utils));
        plugins.add(new TablePropertyGeneratorFactory(utils));
        plugins.add(new ViewPropertyGeneratorFactory(utils));
        plugins.add(new InheritedModelPropertyGeneratorFactory(utils));
    }

    public void addPlugin(Plugin plugin) {
        this.plugins.add(plugin);
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
