/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.plugins;

import com.yahoo.aptutils.model.DeclaredTypeName;
import com.yahoo.aptutils.utils.AptUtils;
import com.yahoo.squidb.processor.plugins.defaults.ConstructorPlugin;
import com.yahoo.squidb.processor.plugins.defaults.ImplementsPlugin;
import com.yahoo.squidb.processor.plugins.defaults.ModelMethodPlugin;

import java.util.ArrayList;
import java.util.List;

import javax.lang.model.element.TypeElement;

public class PluginContext {

    private List<Plugin> plugins = new ArrayList<Plugin>();

    public PluginContext(AptUtils utils) {
        plugins.add(new ConstructorPlugin(utils));
        plugins.add(new ImplementsPlugin(utils));
        plugins.add(new ModelMethodPlugin(utils));
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

}
