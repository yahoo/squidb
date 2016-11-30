/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.plugins.defaults;

import com.yahoo.aptutils.model.DeclaredTypeName;
import com.yahoo.aptutils.utils.AptUtils;
import com.yahoo.aptutils.writer.JavaFileWriter;
import com.yahoo.aptutils.writer.expressions.Expression;
import com.yahoo.aptutils.writer.expressions.Expressions;
import com.yahoo.aptutils.writer.parameters.TypeDeclarationParameters;
import com.yahoo.squidb.processor.data.ErrorInfo;
import com.yahoo.squidb.processor.data.ModelSpec;
import com.yahoo.squidb.processor.plugins.Plugin;
import com.yahoo.squidb.processor.plugins.PluginEnvironment;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;

/**
 * A {@link Plugin} that controls writing any errors logged using {@link ModelSpec#logError(String, Element)} to the
 * generated file using the {@link com.yahoo.squidb.annotations.ModelGenErrors} annotation, to be subsequently
 * processed by the {@link com.yahoo.squidb.processor.ErrorLoggingProcessor}. It is enabled by default but can be
 * disabled by passing {@link PluginEnvironment#OPTIONS_USE_STANDARD_ERROR_LOGGING 'standardErrorLogging'} as one
 * of the values for the 'squidbOptions' key.
 */
public class ErrorLoggingPlugin extends Plugin {

    private static final String MODEL_GEN_ERRORS_CLASS = "com.yahoo.squidb.annotations.ModelGenErrors";
    private static final DeclaredTypeName MODEL_GEN_ERRORS = new DeclaredTypeName(MODEL_GEN_ERRORS_CLASS);
    private static final DeclaredTypeName MODEL_GEN_ERROR_INNER =
            new DeclaredTypeName(MODEL_GEN_ERRORS_CLASS, "ModelGenError");

    public ErrorLoggingPlugin(ModelSpec<?> modelSpec, PluginEnvironment pluginEnv) {
        super(modelSpec, pluginEnv);
    }

    @Override
    public void addRequiredImports(Set<DeclaredTypeName> imports) {
        if (modelSpec.getLoggedErrors().size() > 0) {
            imports.add(MODEL_GEN_ERRORS);
            imports.add(MODEL_GEN_ERROR_INNER);
        }
    }

    @Override
    public void emitAdditionalJava(JavaFileWriter writer) throws IOException {
        List<ErrorInfo> errors = modelSpec.getLoggedErrors();
        if (errors.size() > 0) {
            writer.writeExpression(new ModelGenErrorsExpression(errors))
                    .writeNewline();
            TypeDeclarationParameters dummyErrorClass = new TypeDeclarationParameters()
                    .setModifiers(Modifier.STATIC, Modifier.FINAL)
                    .setName(new DeclaredTypeName(modelSpec.getGeneratedClassName().toString(), "LoggedErrors"))
                    .setKind(JavaFileWriter.Type.CLASS);
            writer.beginTypeDefinition(dummyErrorClass);
            writer.writeComment("Dummy class for holding logged error annotations");
            writer.finishTypeDefinition();
        }
    }

    private static class ModelGenErrorsExpression extends Expression {

        private final List<ErrorInfo> errors;

        private ModelGenErrorsExpression(List<ErrorInfo> errors) {
            this.errors = errors;
        }

        @Override
        public boolean writeExpression(JavaFileWriter writer) throws IOException {
            writer.appendString("@")
                    .appendString(writer.shortenName(MODEL_GEN_ERRORS, false))
                    .appendString("({\n");
            writer.moveToScope(JavaFileWriter.Scope.METHOD_DEFINITION);
            boolean needsNewline = false;
            for (ErrorInfo errorInfo : errors) {
                if (needsNewline) {
                    writer.appendString(",").writeNewline();
                }
                needsNewline = true;
                writer.writeExpression(new ModelGenErrorSingle(errorInfo));
            }
            writer.writeNewline();
            writer.finishScope(JavaFileWriter.Scope.METHOD_DEFINITION);
            writer.writeString("})");
            return true;
        }
    }

    private static class ModelGenErrorSingle extends Expression {

        private final ErrorInfo errorInfo;

        private ModelGenErrorSingle(ErrorInfo errorInfo) {
            this.errorInfo = errorInfo;
        }

        @Override
        public boolean writeExpression(JavaFileWriter writer) throws IOException {
            writer.appendString("@")
                    .appendString(writer.shortenName(MODEL_GEN_ERROR_INNER, false))
                    .appendString("(specClass=")
                    .appendExpression(Expressions.classObject(errorInfo.errorClass))
                    .appendString(", ");
            if (!AptUtils.isEmpty(errorInfo.element)) {
                writer.appendString("element=\"")
                        .appendString(errorInfo.element)
                        .appendString("\", ");
            }
            writer.appendString("message=\"")
                    .appendString(errorInfo.message)
                    .appendString("\")");
            return true;
        }
    }
}
