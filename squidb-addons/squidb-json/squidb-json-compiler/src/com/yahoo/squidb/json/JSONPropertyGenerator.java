/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.json;

import com.yahoo.aptutils.model.DeclaredTypeName;
import com.yahoo.aptutils.model.TypeName;
import com.yahoo.aptutils.utils.AptUtils;
import com.yahoo.aptutils.visitors.ImportGatheringTypeNameVisitor;
import com.yahoo.aptutils.writer.JavaFileWriter;
import com.yahoo.aptutils.writer.expressions.Expressions;
import com.yahoo.squidb.processor.data.ModelSpec;
import com.yahoo.squidb.processor.plugins.defaults.properties.generators.BasicStringPropertyGenerator;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.VariableElement;

public class JSONPropertyGenerator extends BasicStringPropertyGenerator {

    protected final DeclaredTypeName fieldType;

    public JSONPropertyGenerator(ModelSpec<?> modelSpec, VariableElement field, DeclaredTypeName fieldType,
            AptUtils utils) {
        super(modelSpec, field, utils);
        this.fieldType = fieldType;
    }

    @Override
    protected void registerAdditionalImports(Set<DeclaredTypeName> imports) {
        super.registerAdditionalImports(imports);
        imports.add(JSONTypeConstants.SQUIDB_JSON_SUPPORT);
        fieldType.accept(new ImportGatheringTypeNameVisitor(), imports);
    }

    @Override
    public DeclaredTypeName getTypeForAccessors() {
        return fieldType;
    }

    @Override
    protected void writeGetterBody(JavaFileWriter writer) throws IOException {
        List<? extends TypeName> typeArgs = fieldType.getTypeArgs();
        Object[] methodArgs = new Object[3 + (typeArgs == null ? 0 : typeArgs.size())];
        methodArgs[0] = "this";
        methodArgs[1] = propertyName;
        methodArgs[2] = Expressions.classObject(fieldType);
        if (typeArgs != null) {
            for (int i = 3; i < methodArgs.length; i++) {
                methodArgs[i] = Expressions.classObject((DeclaredTypeName) typeArgs.get(i - 3));
            }
        }

        writer.writeStatement(Expressions.staticMethod(JSONTypeConstants.SQUIDB_JSON_SUPPORT, "getObjectValue",
                methodArgs).returnExpr());
    }

    @Override
    protected void writeSetterBody(JavaFileWriter writer, String argName) throws IOException {
        writer.writeStatement(Expressions.staticMethod(JSONTypeConstants.SQUIDB_JSON_SUPPORT, "setObjectProperty",
                "this", propertyName, argName));
        writer.writeStringStatement("return this");
    }

}
