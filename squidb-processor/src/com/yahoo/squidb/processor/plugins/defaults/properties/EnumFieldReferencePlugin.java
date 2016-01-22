package com.yahoo.squidb.processor.plugins.defaults.properties;

import com.yahoo.aptutils.model.DeclaredTypeName;
import com.yahoo.aptutils.model.TypeName;
import com.yahoo.squidb.processor.TypeConstants;
import com.yahoo.squidb.processor.data.ModelSpec;
import com.yahoo.squidb.processor.data.ViewModelSpecWrapper;
import com.yahoo.squidb.processor.plugins.PluginEnvironment;
import com.yahoo.squidb.processor.plugins.defaults.properties.generators.EnumPropertyGenerator;
import com.yahoo.squidb.processor.plugins.defaults.properties.generators.PropertyGenerator;

import java.util.List;

import javax.lang.model.element.VariableElement;

public class EnumFieldReferencePlugin extends FieldReferencePlugin {

    public EnumFieldReferencePlugin(ModelSpec<?> modelSpec, PluginEnvironment pluginEnv) {
        super(modelSpec, pluginEnv);
    }

    @Override
    public boolean hasChangesForModelSpec() {
        return modelSpec instanceof ViewModelSpecWrapper;
    }

    @Override
    protected boolean hasPropertyGeneratorForField(VariableElement field, DeclaredTypeName fieldType) {
        return field.getModifiers().containsAll(TypeConstants.PUBLIC_STATIC_FINAL)
                && fieldType.equals(TypeConstants.ENUM_PROPERTY);
    }

    @Override
    protected PropertyGenerator getPropertyGenerator(VariableElement field, DeclaredTypeName fieldType) {
        // We know it's an EnumProperty, so extract the type arg
        List<? extends TypeName> typeArgs = fieldType.getTypeArgs();
        if (typeArgs != null && typeArgs.size() == 1) {
            TypeName enumType = typeArgs.get(0);
            if (enumType instanceof DeclaredTypeName) {
                return new EnumPropertyGenerator(modelSpec, field, utils, (DeclaredTypeName) enumType);
            }
        }
        return null;
    }
}

