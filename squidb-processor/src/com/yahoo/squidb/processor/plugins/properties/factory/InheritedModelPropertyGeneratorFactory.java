/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.plugins.properties.factory;

import com.yahoo.aptutils.model.DeclaredTypeName;
import com.yahoo.aptutils.utils.AptUtils;
import com.yahoo.squidb.annotations.ColumnSpec;
import com.yahoo.squidb.annotations.InheritedModelSpec;
import com.yahoo.squidb.processor.TypeConstants;
import com.yahoo.squidb.processor.data.InheritedModelSpecWrapper;
import com.yahoo.squidb.processor.data.ModelSpec;
import com.yahoo.squidb.processor.plugins.properties.generators.PropertyGenerator;

import java.util.Set;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;
import javax.tools.Diagnostic;

/**
 * This plugin controls generating property declarations, getters, and setters for fields in an inherited model. It can
 * create instances of {@link PropertyGenerator} for references to other Property subclasses (StringProperty,
 * LongProperty, etc.)
 */
public class InheritedModelPropertyGeneratorFactory extends FieldReferencePropertyGeneratorFactory<InheritedModelSpec> {

    public InheritedModelPropertyGeneratorFactory(ModelSpec<?> modelSpec, AptUtils utils) {
        super(modelSpec, utils);
    }

    @Override
    public boolean canProcessModelSpec() {
        return modelSpec instanceof InheritedModelSpecWrapper;
    }

    @Override
    public boolean processVariableElement(VariableElement field, DeclaredTypeName fieldType) {
        if (field.getAnnotation(Deprecated.class) != null) {
            return true;
        }
        if (field.getAnnotation(ColumnSpec.class) != null) {
            utils.getMessager().printMessage(Diagnostic.Kind.WARNING,
                    "ColumnSpec is ignored outside of table models", field);
        }
        Set<Modifier> modifiers = field.getModifiers();
        if (modifiers.containsAll(TypeConstants.PUBLIC_STATIC_FINAL)) {
            if (!TypeConstants.isPropertyType(fieldType)) {
                modelSpec.addConstantElement(field);
                return true;
            } else {
                return createPropertyGenerator(field, fieldType);
            }
        } else {
            utils.getMessager().printMessage(Diagnostic.Kind.WARNING, "Unused field in spec", field);
            return false;
        }
    }
}
