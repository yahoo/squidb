/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.plugins.defaults;

import com.yahoo.aptutils.model.DeclaredTypeName;
import com.yahoo.aptutils.writer.JavaFileWriter;
import com.yahoo.aptutils.writer.expressions.Expressions;
import com.yahoo.aptutils.writer.parameters.MethodDeclarationParameters;
import com.yahoo.squidb.processor.TypeConstants;
import com.yahoo.squidb.processor.data.InheritedModelSpecWrapper;
import com.yahoo.squidb.processor.data.ModelSpec;
import com.yahoo.squidb.processor.data.TableModelSpecWrapper;
import com.yahoo.squidb.processor.data.ViewModelSpecWrapper;
import com.yahoo.squidb.processor.plugins.Plugin;
import com.yahoo.squidb.processor.plugins.PluginEnvironment;
import com.yahoo.squidb.processor.writers.ModelFileWriter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.Modifier;

/**
 * A {@link Plugin} that alters the generated models to have Android-specific features. It is disabled by default but
 * can be enabled by passing {@link PluginEnvironment#OPTIONS_GENERATE_ANDROID_MODELS 'androidModels'} as one
 * of the values for the 'squidbOptions' key.
 */
public class AndroidModelPlugin extends Plugin {

    private static final ModelSpec.ModelSpecVisitor<DeclaredTypeName, Void> superclassVisitor
            = new ModelSpec.ModelSpecVisitor<DeclaredTypeName, Void>() {
        @Override
        public DeclaredTypeName visitTableModel(TableModelSpecWrapper modelSpec, Void data) {
            return TypeConstants.ANDROID_TABLE_MODEL;
        }

        @Override
        public DeclaredTypeName visitViewModel(ViewModelSpecWrapper modelSpec, Void data) {
            return TypeConstants.ANDROID_VIEW_MODEL;
        }

        @Override
        public DeclaredTypeName visitInheritedModel(InheritedModelSpecWrapper modelSpec, Void data) {
            return null;
        }
    };

    private final DeclaredTypeName modelSuperclass;
    private final boolean generateConstructors;

    public AndroidModelPlugin(ModelSpec<?> modelSpec, PluginEnvironment pluginEnv) {
        super(modelSpec, pluginEnv);
        modelSuperclass = modelSpec.accept(superclassVisitor, null);
        generateConstructors = !pluginEnv.hasOption(PluginEnvironment.OPTIONS_DISABLE_DEFAULT_CONSTRUCTORS);
    }

    @Override
    public DeclaredTypeName getModelSuperclass() {
        return modelSuperclass;
    }

    @Override
    public void addRequiredImports(Set<DeclaredTypeName> imports) {
        imports.add(TypeConstants.MODEL_CREATOR);
        if (generateConstructors) {
            imports.add(TypeConstants.CONTENT_VALUES);
        }
        if (modelSuperclass != null) {
            imports.add(modelSuperclass);
        }
    }

    @Override
    public void emitConstructors(JavaFileWriter writer) throws IOException {
        if (generateConstructors) {
            String valuesName = "contentValues";
            DeclaredTypeName valuesType = TypeConstants.CONTENT_VALUES;

            MethodDeclarationParameters params = new MethodDeclarationParameters()
                    .setModifiers(Modifier.PUBLIC)
                    .setConstructorName(modelSpec.getGeneratedClassName());

            params.setArgumentTypes(Collections.singletonList(valuesType))
                    .setArgumentNames(valuesName);
            writer.beginConstructorDeclaration(params)
                    .writeStatement(Expressions.callMethod("this", valuesName,
                            ModelFileWriter.PROPERTIES_ARRAY_NAME))
                    .finishMethodDefinition();

            String methodName = "readPropertiesFromContentValues";
            params.setArgumentTypes(Arrays.asList(valuesType, TypeConstants.PROPERTY_VARARGS))
                    .setArgumentNames(valuesName, "withProperties");
            writer.beginConstructorDeclaration(params)
                    .writeStringStatement("this()")
                    .writeStringStatement(methodName + "(" + valuesName + ", withProperties)")
                    .finishMethodDefinition();
        }
    }

    @Override
    public void afterEmitMethods(JavaFileWriter writer) throws IOException {
        // emit creator for parcelable
        writer.writeComment("--- parcelable helpers");
        List<DeclaredTypeName> genericList = Collections.singletonList(modelSpec.getGeneratedClassName());
        DeclaredTypeName creatorType = TypeConstants.CREATOR.clone();
        DeclaredTypeName modelCreatorType = TypeConstants.MODEL_CREATOR.clone();
        creatorType.setTypeArgs(genericList);
        modelCreatorType.setTypeArgs(genericList);

        writer.writeFieldDeclaration(creatorType,
                "CREATOR", Expressions.callConstructor(modelCreatorType,
                        Expressions.classObject(modelSpec.getGeneratedClassName())),
                TypeConstants.PUBLIC_STATIC_FINAL)
                .writeNewline();
    }
}
