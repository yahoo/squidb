/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.plugins.properties.factory;

import com.yahoo.aptutils.model.DeclaredTypeName;
import com.yahoo.aptutils.utils.AptUtils;
import com.yahoo.squidb.annotations.ColumnSpec;
import com.yahoo.squidb.annotations.ViewModelSpec;
import com.yahoo.squidb.annotations.ViewQuery;
import com.yahoo.squidb.processor.TypeConstants;
import com.yahoo.squidb.processor.data.ModelSpec;
import com.yahoo.squidb.processor.data.ViewModelSpecWrapper;
import com.yahoo.squidb.processor.plugins.properties.generators.PropertyGenerator;

import java.util.Set;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;
import javax.tools.Diagnostic;

/**
 * This plugin controls generating property declarations, getters, and setters for fields in a view model. It can
 * create instances of {@link PropertyGenerator} for references to other Property subclasses (StringProperty,
 * LongProperty, etc.)
 */
public class ViewPropertyGeneratorFactory extends FieldReferencePropertyGeneratorFactory<ViewModelSpec> {

    public ViewPropertyGeneratorFactory(ModelSpec<?> modelSpec, AptUtils utils) {
        super(modelSpec, utils);
    }

    @Override
    public boolean canProcessModelSpec() {
        return modelSpec instanceof ViewModelSpecWrapper;
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
        boolean isViewProperty = TypeConstants.isPropertyType(fieldType);
        ViewQuery isViewQuery = field.getAnnotation(ViewQuery.class);
        Set<Modifier> modifiers = field.getModifiers();
        if (modifiers.containsAll(TypeConstants.PUBLIC_STATIC_FINAL)) {
            if (isViewQuery != null) {
                if (!TypeConstants.QUERY.equals(fieldType)) {
                    utils.getMessager().printMessage(Diagnostic.Kind.ERROR,
                            "ViewQuery must be an instance of " + TypeConstants.QUERY.toString());
                } else if (modelSpec.hasMetadata(ViewModelSpecWrapper.METADATA_KEY_QUERY_ELEMENT)) {
                    utils.getMessager().printMessage(Diagnostic.Kind.ERROR,
                            "Only one ViewQuery can be declared per spec");
                } else {
                    modelSpec.attachMetadata(ViewModelSpecWrapper.METADATA_KEY_VIEW_QUERY, isViewQuery);
                    modelSpec.attachMetadata(ViewModelSpecWrapper.METADATA_KEY_QUERY_ELEMENT, field);
                }
            } else if (!isViewProperty) {
                modelSpec.addConstantElement(field);
            } else {
                return createPropertyGenerator(field, fieldType);
            }
            return true;
        } else if (isViewProperty) {
            utils.getMessager().printMessage(Diagnostic.Kind.ERROR,
                    "View properties must be public static final", field);
            return false;
        } else {
            utils.getMessager().printMessage(Diagnostic.Kind.WARNING, "Unused field in spec", field);
            return false;
        }
    }
}