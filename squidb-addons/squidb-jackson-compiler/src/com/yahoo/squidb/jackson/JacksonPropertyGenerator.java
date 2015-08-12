/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.jackson;

import com.yahoo.aptutils.model.DeclaredTypeName;
import com.yahoo.aptutils.utils.AptUtils;
import com.yahoo.aptutils.writer.JavaFileWriter;
import com.yahoo.aptutils.writer.expressions.Expression;
import com.yahoo.aptutils.writer.expressions.Expressions;
import com.yahoo.squidb.processor.properties.generators.BasicStringPropertyGenerator;

import java.io.IOException;
import java.util.Set;

import javax.lang.model.element.VariableElement;

public abstract class JacksonPropertyGenerator extends BasicStringPropertyGenerator {

    protected final DeclaredTypeName elementType;

    public JacksonPropertyGenerator(VariableElement element, DeclaredTypeName elementType, DeclaredTypeName modelName,
            AptUtils utils) {
        super(element, modelName, utils);
        this.elementType = elementType;
    }

    @Override
    protected void registerAdditionalImports(Set<DeclaredTypeName> imports) {
        super.registerAdditionalImports(imports);
        imports.add(JacksonTypeConstants.SQUIDB_JACKSON_SUPPORT);
    }

    @Override
    protected DeclaredTypeName getTypeForGetAndSet() {
        return elementType;
    }

    @Override
    protected void writeGetterBody(JavaFileWriter writer) throws IOException {
        writer.writeFieldDeclaration(getJavaTypeForGetter(), "type", getJavaTypeInitializer());
        writer.writeStatement(Expressions.staticMethod(JacksonTypeConstants.SQUIDB_JACKSON_SUPPORT, "getObjectValue",
                "this", propertyName, "type").returnExpr());
    }

    protected abstract DeclaredTypeName getJavaTypeForGetter();

    protected abstract Expression getJavaTypeInitializer();

    @Override
    protected void writeSetterBody(JavaFileWriter writer, String argName) throws IOException {
        writer.writeStatement(Expressions.staticMethod(JacksonTypeConstants.SQUIDB_JACKSON_SUPPORT, "setObjectProperty",
                "this", propertyName, argName));
        writer.writeStringStatement("return this");
    }

}
