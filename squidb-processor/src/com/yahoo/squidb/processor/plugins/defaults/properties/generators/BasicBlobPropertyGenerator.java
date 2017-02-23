/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.plugins.defaults.properties.generators;

import com.squareup.javapoet.TypeName;
import com.yahoo.squidb.annotations.tables.defaults.DefaultBlob;
import com.yahoo.squidb.processor.TypeConstants;
import com.yahoo.squidb.processor.data.ModelSpec;
import com.yahoo.squidb.processor.plugins.PluginEnvironment;
import com.yahoo.squidb.processor.plugins.defaults.constraints.DefaultValueAnnotationHandler;

import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import javax.lang.model.element.VariableElement;

/**
 * An implementation of
 * {@link com.yahoo.squidb.processor.plugins.defaults.properties.generators.interfaces.TableModelPropertyGenerator}
 * for handling blob (byte[]) fields
 */
public class BasicBlobPropertyGenerator extends BasicTableModelPropertyGenerator {

    private static final Pattern BLOB_LITERAL = Pattern.compile("X'([0-9a-f]{2})*'", Pattern.CASE_INSENSITIVE);

    public static List<TypeName> handledColumnTypes() {
        return Collections.singletonList(TypeConstants.BYTE_ARRAY);
    }

    public BasicBlobPropertyGenerator(ModelSpec<?, ?> modelSpec, String columnName, PluginEnvironment pluginEnv) {
        super(modelSpec, columnName, pluginEnv);
    }

    public BasicBlobPropertyGenerator(ModelSpec<?, ?> modelSpec, String columnName,
            String propertyName, PluginEnvironment pluginEnv) {
        super(modelSpec, columnName, propertyName, pluginEnv);
    }

    public BasicBlobPropertyGenerator(ModelSpec<?, ?> modelSpec, VariableElement field, PluginEnvironment pluginEnv) {
        super(modelSpec, field, pluginEnv);
    }

    @Override
    public TypeName getTypeForAccessors() {
        return TypeConstants.BYTE_ARRAY;
    }

    @Override
    public TypeName getPropertyType() {
        return TypeConstants.BLOB_PROPERTY;
    }

    @Override
    protected DefaultValueAnnotationHandler<?, ?> getDefaultValueAnnotationHandler() {
        return new DefaultValueAnnotationHandler<DefaultBlob, String>() {
            @Override
            public Class<DefaultBlob> getAnnotationClass() {
                return DefaultBlob.class;
            }

            @Override
            protected String getPrimitiveDefaultValueFromAnnotation(DefaultBlob annotation) {
                return null;
            }

            @Override
            protected String getPrimitiveDefaultValueAsSql(DefaultBlob annotation) {
                String blobLiteral = annotation.value();
                if (!BLOB_LITERAL.matcher(blobLiteral).matches()) {
                    modelSpec.logError("Blob literal is not a hexadecimal string", field);
                }
                return blobLiteral;
            }
        };
    }
}
