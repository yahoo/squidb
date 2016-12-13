/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.plugins.defaults;

import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.yahoo.squidb.processor.TypeConstants;
import com.yahoo.squidb.processor.data.InheritedModelSpecWrapper;
import com.yahoo.squidb.processor.data.ModelSpec;
import com.yahoo.squidb.processor.data.TableModelSpecWrapper;
import com.yahoo.squidb.processor.data.ViewModelSpecWrapper;
import com.yahoo.squidb.processor.plugins.Plugin;
import com.yahoo.squidb.processor.plugins.PluginEnvironment;
import com.yahoo.squidb.processor.writers.ModelFileWriter;

import javax.lang.model.element.Modifier;

/**
 * A {@link Plugin} that alters the generated models to have Android-specific features. It is disabled by default but
 * can be enabled by passing {@link PluginEnvironment#OPTIONS_GENERATE_ANDROID_MODELS 'androidModels'} as one
 * of the values for the 'squidbOptions' key.
 */
public class AndroidModelPlugin extends Plugin {

    private static final ModelSpec.ModelSpecVisitor<TypeName, Void> superclassVisitor
            = new ModelSpec.ModelSpecVisitor<TypeName, Void>() {
        @Override
        public TypeName visitTableModel(TableModelSpecWrapper modelSpec, Void data) {
            return TypeConstants.ANDROID_TABLE_MODEL;
        }

        @Override
        public TypeName visitViewModel(ViewModelSpecWrapper modelSpec, Void data) {
            return TypeConstants.ANDROID_VIEW_MODEL;
        }

        @Override
        public TypeName visitInheritedModel(InheritedModelSpecWrapper modelSpec, Void data) {
            return null;
        }
    };

    private final TypeName modelSuperclass;
    private final boolean generateConstructors;

    public AndroidModelPlugin(ModelSpec<?, ?> modelSpec, PluginEnvironment pluginEnv) {
        super(modelSpec, pluginEnv);
        modelSuperclass = modelSpec.accept(superclassVisitor, null);
        generateConstructors = !pluginEnv.hasSquidbOption(PluginEnvironment.OPTIONS_DISABLE_DEFAULT_CONSTRUCTORS);
    }

    @Override
    public TypeName getModelSuperclass() {
        return modelSuperclass;
    }

    @Override
    public void declareMethodsOrConstructors(TypeSpec.Builder builder) {
        if (generateConstructors) {
            String valuesName = "contentValues";
            TypeName valuesType = TypeConstants.CONTENT_VALUES;

            MethodSpec.Builder params = MethodSpec.constructorBuilder()
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(valuesType, valuesName)
                    .addStatement("this($L, $L)", valuesName, ModelFileWriter.PROPERTIES_ARRAY_NAME);
            builder.addMethod(params.build());

            params = MethodSpec.constructorBuilder()
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(valuesType, valuesName)
                    .addParameter(TypeConstants.PROPERTY_ARRAY, "withProperties")
                    .varargs()
                    .addStatement("this()")
                    .addStatement("readPropertiesFromContentValues($L, withProperties)", valuesName);
            builder.addMethod(params.build());
        }
    }

    @Override
    public void afterDeclareSchema(TypeSpec.Builder builder) {
        // declare creator for parcelable
        TypeName creatorType = ParameterizedTypeName.get(TypeConstants.CREATOR, modelSpec.getGeneratedClassName());
        TypeName modelCreatorType = ParameterizedTypeName.get(TypeConstants.MODEL_CREATOR,
                modelSpec.getGeneratedClassName());

        FieldSpec.Builder creator = FieldSpec.builder(creatorType, "CREATOR",
                Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                .initializer("new $T($T.class)", modelCreatorType, modelSpec.getGeneratedClassName());
        builder.addField(creator.build());
    }
}
