/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.plugins;

import com.yahoo.aptutils.model.DeclaredTypeName;
import com.yahoo.aptutils.utils.AptUtils;
import com.yahoo.squidb.processor.data.ModelSpec;
import com.yahoo.squidb.processor.plugins.properties.generators.PropertyGenerator;

import java.util.Collections;
import java.util.List;

import javax.lang.model.element.VariableElement;

public class Plugin {

    protected final AptUtils utils;

    public Plugin(AptUtils utils) {
        this.utils = utils;
    }

    public boolean hasPropertyGeneratorForField(ModelSpec<?> modelSpec, VariableElement field,
            DeclaredTypeName fieldType) {
        return false;
    }

    public PropertyGenerator getPropertyGenerator(ModelSpec<?> modelSpec, VariableElement field,
            DeclaredTypeName fieldType) {
        return null;
    }

    public List<? extends PluginWriter> getWritersForModelSpec(ModelSpec<?> modelSpec) {
        return Collections.EMPTY_LIST;
    }

}
