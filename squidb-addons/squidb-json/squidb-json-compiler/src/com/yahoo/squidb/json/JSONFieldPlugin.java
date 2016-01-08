/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.json;

import com.yahoo.aptutils.model.DeclaredTypeName;
import com.yahoo.aptutils.model.TypeName;
import com.yahoo.aptutils.utils.AptUtils;
import com.yahoo.squidb.processor.data.ModelSpec;
import com.yahoo.squidb.processor.plugins.PluginEnvironment;
import com.yahoo.squidb.processor.plugins.defaults.properties.TableModelSpecFieldPlugin;
import com.yahoo.squidb.processor.plugins.defaults.properties.generators.PropertyGenerator;

import javax.lang.model.element.VariableElement;

public class JSONFieldPlugin extends TableModelSpecFieldPlugin {

    public JSONFieldPlugin(ModelSpec<?> modelSpec, PluginEnvironment pluginEnv) {
        super(modelSpec, pluginEnv);
    }

    @Override
    protected boolean hasPropertyGeneratorForField(VariableElement field, DeclaredTypeName fieldType) {
        if (field.getAnnotation(JSONProperty.class) == null) {
            return false;
        }
        // Check that all generic types are declared types
        if (!AptUtils.isEmpty(fieldType.getTypeArgs())) {
            for (TypeName typeName : fieldType.getTypeArgs()) {
                if (!(typeName instanceof DeclaredTypeName)) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    protected PropertyGenerator getPropertyGenerator(VariableElement field, DeclaredTypeName fieldType) {
        return new JSONPropertyGenerator(modelSpec, field, fieldType, utils);
    }

}
