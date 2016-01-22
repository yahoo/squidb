package com.yahoo.squidb.processor.plugins.defaults.properties;

import com.yahoo.squidb.processor.data.ModelSpec;
import com.yahoo.squidb.processor.plugins.PluginBundle;
import com.yahoo.squidb.processor.plugins.PluginEnvironment;

import java.util.Arrays;

/**
 * Plugin that bundles the default Enum property handling
 */
public class EnumPluginBundle extends PluginBundle {

    public EnumPluginBundle(ModelSpec<?> modelSpec, PluginEnvironment pluginEnv) {
        super(modelSpec, pluginEnv, Arrays.asList(new EnumFieldPlugin(modelSpec, pluginEnv),
                new EnumFieldReferencePlugin(modelSpec, pluginEnv)));
    }
}
