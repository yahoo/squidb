/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.json;

import com.yahoo.aptutils.model.DeclaredTypeName;
import com.yahoo.aptutils.utils.AptUtils;
import com.yahoo.aptutils.writer.JavaFileWriter;
import com.yahoo.aptutils.writer.parameters.MethodDeclarationParameters;
import com.yahoo.squidb.processor.data.ModelSpec;
import com.yahoo.squidb.processor.plugins.defaults.properties.generators.PropertyReferencePropertyGenerator;

import java.io.IOException;
import java.util.Set;

import javax.lang.model.element.VariableElement;

/**
 * Extension of {@link PropertyReferencePropertyGenerator} that handles JSON property references
 */
public class JSONPropertyReferencePropertyGenerator extends PropertyReferencePropertyGenerator {

    private final JSONPropertyGeneratorDelegate delegate;

    public JSONPropertyReferencePropertyGenerator(ModelSpec<?, ?> modelSpec, VariableElement field,
            DeclaredTypeName propertyType, AptUtils utils) {
        super(modelSpec, field, propertyType, utils);
        this.delegate = new JSONPropertyGeneratorDelegate(getPropertyName(), getTypeForAccessors());
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
