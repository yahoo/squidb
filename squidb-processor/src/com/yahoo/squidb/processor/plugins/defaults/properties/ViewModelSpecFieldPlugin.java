/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.plugins.defaults.properties;

import com.squareup.javapoet.TypeName;
import com.yahoo.squidb.annotations.Alias;
import com.yahoo.squidb.annotations.ViewQuery;
import com.yahoo.squidb.processor.SqlUtils;
import com.yahoo.squidb.processor.TypeConstants;
import com.yahoo.squidb.processor.data.ModelSpec;
import com.yahoo.squidb.processor.data.ViewModelSpecWrapper;
import com.yahoo.squidb.processor.plugins.PluginEnvironment;
import com.yahoo.squidb.processor.plugins.defaults.properties.generators.PropertyReferencePropertyGenerator;
import com.yahoo.squidb.processor.plugins.defaults.properties.generators.interfaces.ViewModelPropertyGenerator;

import javax.lang.model.element.VariableElement;

/**
 * This plugin controls generating property declarations, getters, and setters for fields in a view model. It can
 * create instances of {@link ViewModelPropertyGenerator} for references to other Property subclasses (StringProperty,
 * LongProperty, etc.)
 */
public class ViewModelSpecFieldPlugin
        extends PropertyReferencePlugin<ViewModelSpecWrapper, ViewModelPropertyGenerator> {

    public ViewModelSpecFieldPlugin(ModelSpec<?, ?> modelSpec, PluginEnvironment pluginEnv) {
        super(modelSpec, pluginEnv);
    }

    @Override
    protected Class<ViewModelSpecWrapper> getHandledModelSpecClass() {
        return ViewModelSpecWrapper.class;
    }

    @Override
    public boolean processVariableElement(VariableElement field, TypeName fieldType) {
        boolean isViewProperty = TypeConstants.isBasicPropertyType(fieldType);
        ViewQuery isViewQuery = field.getAnnotation(ViewQuery.class);
        if (TypeConstants.isVisibleConstant(field)) {
            if (isViewQuery != null) {
                if (!TypeConstants.QUERY.equals(fieldType)) {
                    modelSpec.logError("ViewQuery must be an instance of " + TypeConstants.QUERY.toString(), field);
                } else if (modelSpec.hasMetadata(ViewModelSpecWrapper.METADATA_KEY_QUERY_ELEMENT)) {
                    modelSpec.logError("Only one ViewQuery can be declared per spec", field);
                } else {
                    modelSpec.putMetadata(ViewModelSpecWrapper.METADATA_KEY_VIEW_QUERY, isViewQuery);
                    modelSpec.putMetadata(ViewModelSpecWrapper.METADATA_KEY_QUERY_ELEMENT, field);
                }
                return true;
            } else {
                return super.processVariableElement(field, fieldType);
            }
        } else if (isViewProperty) {
            modelSpec.logError("View properties must be static final and non-private", field);
        }
        return false;
    }

    @Override
    protected ViewModelPropertyGenerator getPropertyGenerator(VariableElement field, TypeName fieldType) {
        Alias alias = field.getAnnotation(Alias.class);
        if (alias != null) {
            SqlUtils.checkIdentifier(alias.value().trim(), "view column name", modelSpec, field,
                    pluginEnv.getMessager());
        }
        return new PropertyReferencePropertyGenerator(modelSpec, field, fieldType, pluginEnv);
    }
}
