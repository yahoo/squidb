/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.plugins.defaults.properties.generators;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.TypeName;
import com.yahoo.squidb.annotations.Alias;
import com.yahoo.squidb.processor.StringUtils;
import com.yahoo.squidb.processor.TypeConstants;
import com.yahoo.squidb.processor.data.ModelSpec;
import com.yahoo.squidb.processor.data.ViewModelSpecWrapper;
import com.yahoo.squidb.processor.plugins.PluginEnvironment;
import com.yahoo.squidb.processor.plugins.defaults.properties.generators.interfaces.InheritedModelPropertyGenerator;
import com.yahoo.squidb.processor.plugins.defaults.properties.generators.interfaces.ViewModelPropertyGenerator;

import java.lang.annotation.Annotation;

import javax.annotation.Nonnull;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;

/**
 * An implementation of {@link ViewModelPropertyGenerator} and {@link InheritedModelPropertyGenerator} that processes
 * constant references to properties in other models. By default this only handles the primitive property types.
 * More advanced property types (e.g. as in enum properties or JSON properties) will need to override
 * {@link #initAccessorsType()} to specify what the type to use for getters and setters should be
 */
public class PropertyReferencePropertyGenerator extends BasicPropertyGeneratorImpl
        implements ViewModelPropertyGenerator, InheritedModelPropertyGenerator {

    protected final TypeName propertyType;
    protected final TypeName getAndSetType;

    public PropertyReferencePropertyGenerator(ModelSpec<?, ?> modelSpec, VariableElement field,
            TypeName propertyType, PluginEnvironment pluginEnv) {
        super(modelSpec, field, field.getSimpleName().toString(), pluginEnv);
        this.propertyType = propertyType;
        this.getAndSetType = initAccessorsType();
    }

    protected TypeName initAccessorsType() {
        String basicType = propertyType.toString().replace(TypeConstants.PROPERTY_UNPARAMETERIZED.toString(), "")
                .replace("Property", "")
                .replace(".", "");
        if ("Blob".equals(basicType)) {
            return TypeConstants.BYTE_ARRAY;
        } else {
            return ClassName.get("java.lang", basicType);
        }
    }

    @Override
    public TypeName getTypeForAccessors() {
        return getAndSetType;
    }

    @Override
    public TypeName getPropertyType() {
        return propertyType;
    }

    @Override
    public String getAlias() {
        if (modelSpec instanceof ViewModelSpecWrapper && field != null) {
            Alias alias = field.getAnnotation(Alias.class);
            return alias != null ? alias.value().trim() : null;
        }
        return null;
    }

    @Override
    public CodeBlock buildViewPropertyReference(boolean alias) {
        if (alias && !StringUtils.isEmpty(getAlias())) {
            return CodeBlock.of("$T.$L.as($S)", modelSpec.getModelSpecName(), getPropertyName(), getAlias());
        } else {
            return CodeBlock.of("$T.$L", modelSpec.getModelSpecName(), getPropertyName());
        }
    }

    @Override
    public FieldSpec.Builder buildInheritedPropertyDeclaration() {
        return FieldSpec.builder(getPropertyType(), getPropertyName(),
                Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                .initializer("$T.$L", modelSpec.getModelSpecName(), getPropertyName());
    }

    @Override
    protected Class<? extends Annotation> getAccessorNullabilityAnnotation() {
        if (field != null && field.getAnnotation(Nonnull.class) != null) {
            return Nonnull.class;
        }
        return super.getAccessorNullabilityAnnotation();
    }
}
