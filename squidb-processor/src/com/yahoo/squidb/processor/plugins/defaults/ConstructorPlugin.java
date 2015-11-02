/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.plugins.defaults;

import com.yahoo.aptutils.model.CoreTypes;
import com.yahoo.aptutils.model.DeclaredTypeName;
import com.yahoo.aptutils.writer.JavaFileWriter;
import com.yahoo.aptutils.writer.expressions.Expression;
import com.yahoo.aptutils.writer.expressions.Expressions;
import com.yahoo.aptutils.writer.parameters.MethodDeclarationParameters;
import com.yahoo.squidb.processor.TypeConstants;
import com.yahoo.squidb.processor.data.ModelSpec;
import com.yahoo.squidb.processor.plugins.Plugin;
import com.yahoo.squidb.processor.plugins.PluginEnvironment;
import com.yahoo.squidb.processor.writers.ModelFileWriter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import javax.lang.model.element.Modifier;

/**
 * A {@link Plugin} that controls generating constructors in model classes. This plugin generates four distinct
 * constructors in each model class. It is enabled by default. It can be disabled by passing a bitmask with the
 * {@link PluginEnvironment#OPTIONS_DISABLE_DEFAULT_CONSTRUCTORS} flag set.
 */
public class ConstructorPlugin extends Plugin {

    private final boolean androidModels;

    public ConstructorPlugin(ModelSpec<?> modelSpec, PluginEnvironment pluginEnv) {
        super(modelSpec, pluginEnv);
        this.androidModels = pluginEnv.hasOption(PluginEnvironment.OPTIONS_GENERATE_ANDROID_MODELS);
    }

    @Override
    public void addRequiredImports(Set<DeclaredTypeName> imports) {
        imports.add(TypeConstants.SQUID_CURSOR);
        imports.add(androidModels ? TypeConstants.CONTENT_VALUES : TypeConstants.MAP);
    }

    @Override
    public void emitConstructors(JavaFileWriter writer) throws IOException {
        writer.writeComment("--- default constructors");
        MethodDeclarationParameters params = new MethodDeclarationParameters()
                .setModifiers(Modifier.PUBLIC)
                .setConstructorName(modelSpec.getGeneratedClassName());
        writer.beginConstructorDeclaration(params)
                .writeStringStatement("super()")
                .finishMethodDefinition();

        DeclaredTypeName squidCursorType = TypeConstants.SQUID_CURSOR.clone();
        squidCursorType.setTypeArgs(Collections.singletonList(modelSpec.getGeneratedClassName()));
        params.setArgumentTypes(squidCursorType).setArgumentNames("cursor");
        writer.beginConstructorDeclaration(params)
                .writeStringStatement("this()")
                .writeStringStatement("readPropertiesFromCursor(cursor)")
                .finishMethodDefinition();

        String valuesName = androidModels ? "contentValues" : "values";
        DeclaredTypeName valuesType = androidModels ? TypeConstants.CONTENT_VALUES : TypeConstants.MAP_VALUES;

        params.setArgumentTypes(Collections.singletonList(valuesType))
                .setArgumentNames(valuesName);
        writer.beginConstructorDeclaration(params)
                .writeStatement(Expressions.callMethod("this", valuesName,
                        ModelFileWriter.PROPERTIES_ARRAY_NAME))
                .finishMethodDefinition();

        String methodName = androidModels ? "readPropertiesFromContentValues" : "readPropertiesFromMap";
        params.setArgumentTypes(Arrays.asList(valuesType, TypeConstants.PROPERTY_VARARGS))
                .setArgumentNames(valuesName, "withProperties");
        writer.beginConstructorDeclaration(params)
                .writeStringStatement("this()")
                .writeStringStatement(methodName + "(" + valuesName + ", withProperties)")
                .finishMethodDefinition();

        MethodDeclarationParameters cloneParams = new MethodDeclarationParameters()
                .setModifiers(Modifier.PUBLIC)
                .setMethodName("clone")
                .setReturnType(modelSpec.getGeneratedClassName());

        Expression cloneBody = Expressions.callMethodOn("super", "clone")
                .cast(modelSpec.getGeneratedClassName()).returnExpr();

        writer.writeAnnotation(CoreTypes.OVERRIDE);
        writer.beginMethodDefinition(cloneParams)
                .writeStatement(cloneBody)
                .finishMethodDefinition();
    }
}
