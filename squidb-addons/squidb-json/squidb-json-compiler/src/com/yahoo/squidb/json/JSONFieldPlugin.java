/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.json;

import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.yahoo.squidb.json.annotations.JSONColumn;
import com.yahoo.squidb.processor.TypeConstants;
import com.yahoo.squidb.processor.data.ModelSpec;
import com.yahoo.squidb.processor.data.TableModelSpecWrapper;
import com.yahoo.squidb.processor.plugins.PluginEnvironment;
import com.yahoo.squidb.processor.plugins.defaults.properties.BaseFieldPlugin;
import com.yahoo.squidb.processor.plugins.defaults.properties.generators.interfaces.TableModelPropertyGenerator;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.lang.model.element.VariableElement;

/**
 * Plugin which handles fields annotated with @JSONField in a TableModelSpec file. Generates instances of JSONProperty
 * which helps support serializing objects to JSON strings.
 */
public class JSONFieldPlugin extends BaseFieldPlugin<TableModelSpecWrapper, TableModelPropertyGenerator> {

    public JSONFieldPlugin(ModelSpec<?, ?> modelSpec, PluginEnvironment pluginEnv) {
        super(modelSpec, pluginEnv);
    }

    @Override
    protected Class<TableModelSpecWrapper> getHandledModelSpecClass() {
        return TableModelSpecWrapper.class;
    }

    @Override
    protected boolean hasPropertyGeneratorForField(VariableElement field, TypeName fieldType) {
        if (field.getAnnotation(JSONColumn.class) == null) {
            return false;
        }
        if (TypeConstants.isVisibleConstant(field)) {
            // Looks like a constant, ignore
            return false;
        }

        // Check that all type args are concrete types
        return recursivelyCheckTypes(field, fieldType, new AtomicBoolean(false));
    }

    private boolean recursivelyCheckTypes(VariableElement field, TypeName rootType, AtomicBoolean showedError) {
        if (TypeConstants.isGenericType(rootType)) {
            return false;
        }

        if (rootType instanceof ParameterizedTypeName) {
            List<TypeName> typeArgs = ((ParameterizedTypeName) rootType).typeArguments;
            for (TypeName typeArg : typeArgs) {
                if (!recursivelyCheckTypes(field, typeArg, showedError)) {
                    if (!showedError.getAndSet(true)) {
                        modelSpec.logError("@JSONField fields with type arguments"
                                + " must not use generic or otherwise non-concrete types", field);
                    }
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    protected TableModelPropertyGenerator getPropertyGenerator(VariableElement field, TypeName fieldType) {
        return new JSONPropertyGenerator(modelSpec, field, fieldType, pluginEnv);
    }
}
