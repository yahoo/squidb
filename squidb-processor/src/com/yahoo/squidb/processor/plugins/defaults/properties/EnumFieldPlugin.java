package com.yahoo.squidb.processor.plugins.defaults.properties;

import com.yahoo.aptutils.model.DeclaredTypeName;
import com.yahoo.squidb.processor.TypeConstants;
import com.yahoo.squidb.processor.data.ModelSpec;
import com.yahoo.squidb.processor.data.TableModelSpecWrapper;
import com.yahoo.squidb.processor.plugins.PluginEnvironment;
import com.yahoo.squidb.processor.plugins.defaults.properties.generators.EnumPropertyGenerator;
import com.yahoo.squidb.processor.plugins.defaults.properties.generators.PropertyGenerator;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;

/**
 * Plugin which handles Enum fields in a model spec file. Currently only supports TableModelSpec.
 */
public class EnumFieldPlugin extends BaseFieldPlugin {

    public EnumFieldPlugin(ModelSpec<?> modelSpec, PluginEnvironment pluginEnv) {
        super(modelSpec, pluginEnv);
    }

    @Override
    public boolean hasChangesForModelSpec() {
        return modelSpec instanceof TableModelSpecWrapper;
    }

    @Override
    public boolean processVariableElement(VariableElement field, DeclaredTypeName fieldType) {
        return tryCreatePropertyGenerator(field, fieldType);
    }

    @Override
    protected boolean hasPropertyGeneratorForField(VariableElement field, DeclaredTypeName fieldType) {
        if (field.getModifiers().containsAll(TypeConstants.PUBLIC_STATIC_FINAL)) {
            // Might be a constant, ignore
            return false;
        }

        TypeElement typeElement = (TypeElement) utils.getTypes().asElement(field.asType());
        return typeElement.getKind() == ElementKind.ENUM;
    }

    @Override
    protected PropertyGenerator getPropertyGenerator(VariableElement field, DeclaredTypeName fieldType) {
        return new EnumPropertyGenerator(modelSpec, field, utils, fieldType);
    }
}

