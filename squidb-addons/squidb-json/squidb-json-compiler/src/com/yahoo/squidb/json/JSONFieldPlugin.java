/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.json;

import com.yahoo.aptutils.model.DeclaredTypeName;
import com.yahoo.aptutils.model.TypeName;
import com.yahoo.aptutils.utils.AptUtils;
import com.yahoo.squidb.json.annotations.JSONColumn;
import com.yahoo.squidb.processor.TypeConstants;
import com.yahoo.squidb.processor.data.ModelSpec;
import com.yahoo.squidb.processor.data.TableModelSpecWrapper;
import com.yahoo.squidb.processor.plugins.PluginEnvironment;
import com.yahoo.squidb.processor.plugins.defaults.properties.BaseFieldPlugin;
import com.yahoo.squidb.processor.plugins.defaults.properties.generators.PropertyGenerator;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.lang.model.element.VariableElement;

/**
 * Plugin which handles fields annotated with @JSONField in a TableModelSpec file. Generates instances of JSONProperty
 * which helps support serializing objects to JSON strings.
 */
public class JSONFieldPlugin extends BaseFieldPlugin {

    public JSONFieldPlugin(ModelSpec<?> modelSpec, PluginEnvironment pluginEnv) {
        super(modelSpec, pluginEnv);
    }

    @Override
    public boolean hasChangesForModelSpec() {
        return modelSpec instanceof TableModelSpecWrapper;
    }

    @Override
    protected boolean hasPropertyGeneratorForField(VariableElement field, DeclaredTypeName fieldType) {
        if (field.getAnnotation(JSONColumn.class) == null) {
            return false;
        }
        if (field.getModifiers().containsAll(TypeConstants.PUBLIC_STATIC_FINAL)) {
            // Might be a constant, ignore
            return false;
        }

        // Check that all type args are concrete types
        return recursivelyCheckTypes(field, fieldType, new AtomicBoolean(false));
    }

    private boolean recursivelyCheckTypes(VariableElement field, TypeName rootType, AtomicBoolean showedError) {
        if (!(rootType instanceof DeclaredTypeName)) {
            return false;
        }
        List<? extends TypeName> typeArgs = ((DeclaredTypeName) rootType).getTypeArgs();
        if (AptUtils.isEmpty(typeArgs)) {
            return true;
        }
        for (TypeName typeArg : typeArgs) {
            if (!recursivelyCheckTypes(field, typeArg, showedError)) {
                if (!showedError.getAndSet(true)) {
                    modelSpec.logError( "@JSONField fields with type arguments"
                            + " must not use generic or otherwise non-concrete types", field);
                }
                return false;
            }
        }
        return true;
    }

    @Override
    protected PropertyGenerator getPropertyGenerator(VariableElement field, DeclaredTypeName fieldType) {
        return new JSONPropertyGenerator(modelSpec, field, fieldType, utils);
    }
}
