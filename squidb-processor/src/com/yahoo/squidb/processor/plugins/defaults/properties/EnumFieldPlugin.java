/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.plugins.defaults.properties;

import com.yahoo.aptutils.model.DeclaredTypeName;
import com.yahoo.squidb.processor.TypeConstants;
import com.yahoo.squidb.processor.data.ModelSpec;
import com.yahoo.squidb.processor.data.TableModelSpecWrapper;
import com.yahoo.squidb.processor.plugins.PluginEnvironment;
import com.yahoo.squidb.processor.plugins.defaults.properties.generators.EnumPropertyGenerator;
import com.yahoo.squidb.processor.plugins.defaults.properties.generators.PropertyGenerator;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;

/**
 * Plugin which handles Enum fields in a TableModelSpec file.
 */
public class EnumFieldPlugin extends BaseFieldPlugin {

    public EnumFieldPlugin(ModelSpec<?> modelSpec, PluginEnvironment pluginEnv) {
        super(modelSpec, pluginEnv);
    }

    @Override
    public boolean hasChangesForModelSpec() {
        return modelSpec instanceof TableModelSpecWrapper;
    }

    @Override
    protected boolean hasPropertyGeneratorForField(VariableElement field, DeclaredTypeName fieldType) {
        if (TypeConstants.isConstant(field)) {
            // Looks like a constant, ignore
            return false;
        }

        TypeElement typeElement = (TypeElement) utils.getTypes().asElement(field.asType());
        return typeElement != null && typeElement.getKind() == ElementKind.ENUM;
    }

    @Override
    protected PropertyGenerator getPropertyGenerator(VariableElement field, DeclaredTypeName fieldType) {
        return new EnumPropertyGenerator(modelSpec, field, utils, fieldType);
    }
}

