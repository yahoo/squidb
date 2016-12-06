/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.plugins.defaults.properties.generators;

import com.yahoo.aptutils.model.DeclaredTypeName;
import com.yahoo.aptutils.utils.AptUtils;
import com.yahoo.aptutils.writer.JavaFileWriter;
import com.yahoo.aptutils.writer.expressions.Expressions;
import com.yahoo.squidb.annotations.Alias;
import com.yahoo.squidb.processor.TypeConstants;
import com.yahoo.squidb.processor.data.ModelSpec;
import com.yahoo.squidb.processor.data.ViewModelSpecWrapper;
import com.yahoo.squidb.processor.plugins.defaults.properties.generators.interfaces.InheritedModelPropertyGenerator;
import com.yahoo.squidb.processor.plugins.defaults.properties.generators.interfaces.ViewModelPropertyGenerator;

import java.io.IOException;

import javax.lang.model.element.VariableElement;

/**
 * An implementation of {@link ViewModelPropertyGenerator} and {@link InheritedModelPropertyGenerator} that processes
 * constant references to properties in other models. By default this only handles the primitive property types.
 * More advanced property types (e.g. as in enum properties or JSON properties) will need to override
 * {@link #initAccessorsType()} to specify what the type to use for getters and setters should be
 */
public class PropertyReferencePropertyGenerator extends BasicPropertyGeneratorImpl
        implements ViewModelPropertyGenerator, InheritedModelPropertyGenerator {

    protected final DeclaredTypeName propertyType;
    protected final DeclaredTypeName getAndSetType;

    public PropertyReferencePropertyGenerator(ModelSpec<?, ?> modelSpec, VariableElement field,
            DeclaredTypeName propertyType, AptUtils utils) {
        super(modelSpec, field, field.getSimpleName().toString(), utils);
        this.propertyType = propertyType;
        this.getAndSetType = initAccessorsType();
    }

    protected DeclaredTypeName initAccessorsType() {
        String basicType = propertyType.toString().replace(TypeConstants.PROPERTY.toString(), "")
                .replace("Property", "")
                .replace(".", "");
        if ("Blob".equals(basicType)) {
            return TypeConstants.BYTE_ARRAY;
        } else {
            return new DeclaredTypeName("java.lang", basicType);
        }
    }

    @Override
    public DeclaredTypeName getTypeForAccessors() {
        return getAndSetType;
    }

    @Override
    public DeclaredTypeName getPropertyType() {
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
    public void emitViewPropertyReference(JavaFileWriter writer) throws IOException {
        writer.writeExpression(Expressions.staticReference(modelSpec.getModelSpecName(), getPropertyName()));
    }

    @Override
    public void emitInheritedPropertyDeclaration(JavaFileWriter writer) throws IOException {
        writer.writeFieldDeclaration(getPropertyType(), getPropertyName(),
                Expressions.staticReference(modelSpec.getModelSpecName(), getPropertyName()),
                TypeConstants.PUBLIC_STATIC_FINAL)
                .writeNewline();
    }
}
