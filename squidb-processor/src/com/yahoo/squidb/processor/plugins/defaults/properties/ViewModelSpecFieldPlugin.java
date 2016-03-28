/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.plugins.defaults.properties;

import com.yahoo.aptutils.model.DeclaredTypeName;
import com.yahoo.squidb.annotations.ViewQuery;
import com.yahoo.squidb.processor.TypeConstants;
import com.yahoo.squidb.processor.data.ModelSpec;
import com.yahoo.squidb.processor.data.ViewModelSpecWrapper;
import com.yahoo.squidb.processor.plugins.PluginEnvironment;
import com.yahoo.squidb.processor.plugins.defaults.properties.generators.PropertyGenerator;

import javax.lang.model.element.VariableElement;
import javax.tools.Diagnostic;

/**
 * This plugin controls generating property declarations, getters, and setters for fields in a view model. It can
 * create instances of {@link PropertyGenerator} for references to other Property subclasses (StringProperty,
 * LongProperty, etc.)
 */
public class ViewModelSpecFieldPlugin extends FieldReferencePlugin {

    public ViewModelSpecFieldPlugin(ModelSpec<?> modelSpec, PluginEnvironment pluginEnv) {
        super(modelSpec, pluginEnv);
    }

    @Override
    public boolean hasChangesForModelSpec() {
        return modelSpec instanceof ViewModelSpecWrapper;
    }

    @Override
    public boolean processVariableElement(VariableElement field, DeclaredTypeName fieldType) {
        boolean isViewProperty = TypeConstants.isBasicPropertyType(fieldType);
        ViewQuery isViewQuery = field.getAnnotation(ViewQuery.class);
        if (TypeConstants.isVisibleConstant(field)) {
            if (isViewQuery != null) {
                if (!TypeConstants.QUERY.equals(fieldType)) {
                    utils.getMessager().printMessage(Diagnostic.Kind.ERROR,
                            "ViewQuery must be an instance of " + TypeConstants.QUERY.toString());
                } else if (modelSpec.hasMetadata(ViewModelSpecWrapper.METADATA_KEY_QUERY_ELEMENT)) {
                    utils.getMessager().printMessage(Diagnostic.Kind.ERROR,
                            "Only one ViewQuery can be declared per spec");
                } else {
                    modelSpec.putMetadata(ViewModelSpecWrapper.METADATA_KEY_VIEW_QUERY, isViewQuery);
                    modelSpec.putMetadata(ViewModelSpecWrapper.METADATA_KEY_QUERY_ELEMENT, field);
                }
                return true;
            } else {
                return super.processVariableElement(field, fieldType);
            }
        } else if (isViewProperty) {
            utils.getMessager().printMessage(Diagnostic.Kind.ERROR,
                    "View properties must be public static final", field);
        }
        return false;
    }
}
