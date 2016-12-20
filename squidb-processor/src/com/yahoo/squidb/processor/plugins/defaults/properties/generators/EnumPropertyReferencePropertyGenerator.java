/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.plugins.defaults.properties.generators;

import com.yahoo.aptutils.model.DeclaredTypeName;
import com.yahoo.aptutils.utils.AptUtils;
import com.yahoo.aptutils.writer.JavaFileWriter;
import com.yahoo.aptutils.writer.parameters.MethodDeclarationParameters;
import com.yahoo.squidb.processor.data.ModelSpec;

import java.io.IOException;
import java.util.Set;

import javax.lang.model.element.VariableElement;

/**
 * Extension of {@link PropertyReferencePropertyGenerator} that handles enum property references
 */
public class EnumPropertyReferencePropertyGenerator extends PropertyReferencePropertyGenerator {

    private final EnumPropertyGeneratorDelegate delegate;

    public EnumPropertyReferencePropertyGenerator(ModelSpec<?, ?> modelSpec, VariableElement field,
            DeclaredTypeName propertyType, AptUtils utils) {
        super(modelSpec, field, propertyType, utils);
        this.delegate = new EnumPropertyGeneratorDelegate(getPropertyName(), getTypeForAccessors());
    }

    @Override
    protected DeclaredTypeName initAccessorsType() {
        return (DeclaredTypeName) propertyType.getTypeArgs().get(0);
    }

    @Override
    public void registerRequiredImports(Set<DeclaredTypeName> imports) {
        super.registerRequiredImports(imports);
        delegate.registerRequiredImports(imports);
    }

    @Override
    protected void writeGetterBody(JavaFileWriter writer, MethodDeclarationParameters params) throws IOException {
        delegate.writeGetterBody(writer, params);
    }

    @Override
    protected void writeSetterBody(JavaFileWriter writer, MethodDeclarationParameters params) throws IOException {
        delegate.writeSetterBody(writer, params);
    }
}
