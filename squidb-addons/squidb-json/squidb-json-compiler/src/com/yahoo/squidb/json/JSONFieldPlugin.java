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

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.lang.model.element.VariableElement;
import javax.tools.Diagnostic;

public class JSONFieldPlugin extends TableModelSpecFieldPlugin {

    public JSONFieldPlugin(ModelSpec<?> modelSpec, PluginEnvironment pluginEnv) {
        super(modelSpec, pluginEnv);
    }

    @Override
    protected boolean hasPropertyGeneratorForField(VariableElement field, DeclaredTypeName fieldType) {
        if (field.getAnnotation(JSONProperty.class) == null) {
            return false;
        }
        // Check that all type args are concrete types
        return recursivelyCheckTypes(field, fieldType, new AtomicBoolean(false));
    }

    @Override
    protected PropertyGenerator getPropertyGenerator(VariableElement field, DeclaredTypeName fieldType) {
        return new JSONPropertyGenerator(modelSpec, field, fieldType, utils);
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
                    utils.getMessager().printMessage(Diagnostic.Kind.ERROR, "JSONProperty fields with type arguments"
                            + " must not use generic or otherwise non-concrete types", field);
                }
                return false;
            }
        }
        return true;
    }

}
