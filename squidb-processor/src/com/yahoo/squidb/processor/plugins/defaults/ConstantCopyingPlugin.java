/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.plugins.defaults;

import com.yahoo.aptutils.model.DeclaredTypeName;
import com.yahoo.aptutils.utils.AptUtils;
import com.yahoo.squidb.processor.TypeConstants;
import com.yahoo.squidb.processor.data.ModelSpec;
import com.yahoo.squidb.processor.plugins.Plugin;

import java.util.Set;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;

public class ConstantCopyingPlugin extends Plugin {

    public ConstantCopyingPlugin(ModelSpec<?> modelSpec, AptUtils utils) {
        super(modelSpec, utils);
    }

    @Override
    public boolean processVariableElement(VariableElement field, DeclaredTypeName fieldType) {
        if (field.getAnnotation(Deprecated.class) != null) {
            return false;
        }
        Set<Modifier> modifiers = field.getModifiers();
        if (modifiers.containsAll(TypeConstants.PUBLIC_STATIC_FINAL)) {
            modelSpec.addConstantElement(field);
            return true;
        }
        return false;
    }
}
