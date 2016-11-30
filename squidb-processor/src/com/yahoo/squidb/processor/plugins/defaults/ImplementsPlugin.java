/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.plugins.defaults;

import com.yahoo.aptutils.model.DeclaredTypeName;
import com.yahoo.aptutils.utils.AptUtils;
import com.yahoo.squidb.annotations.Implements;
import com.yahoo.squidb.processor.data.ModelSpec;
import com.yahoo.squidb.processor.plugins.Plugin;
import com.yahoo.squidb.processor.plugins.PluginEnvironment;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.TypeElement;

/**
 * A {@link Plugin} that controls declaring that model classes implement interfaces. This plugin looks for and parses
 * the value of any {@link Implements} annotations on the given model spec to determine which interfaces to add. It is
 * enabled by default but can be disabled by passing
 * {@link PluginEnvironment#OPTIONS_DISABLE_DEFAULT_IMPLEMENTS_HANDLING 'disableImplements'} as one of the
 * values for the 'squidbOptions' key.
 */
public class ImplementsPlugin extends Plugin {

    private final List<DeclaredTypeName> interfaces = new ArrayList<>();

    public ImplementsPlugin(ModelSpec<?> modelSpec, PluginEnvironment pluginEnv) {
        super(modelSpec, pluginEnv);
        parseInterfaces();
    }

    @Override
    public void addRequiredImports(Set<DeclaredTypeName> imports) {
        utils.accumulateImportsFromTypeNames(imports, interfaces);
    }

    @Override
    public void addInterfacesToImplement(Set<DeclaredTypeName> interfaces) {
        interfaces.addAll(this.interfaces);
    }

    private void parseInterfaces() {
        TypeElement modelSpecElement = modelSpec.getModelSpecElement();
        if (modelSpecElement.getAnnotation(Implements.class) != null) {
            List<DeclaredTypeName> typeNames = utils.getTypeNamesFromAnnotationValue(
                    utils.getAnnotationValue(modelSpecElement, Implements.class, "interfaceClasses"));
            if (!AptUtils.isEmpty(typeNames)) {
                interfaces.addAll(typeNames);
            }

            AnnotationValue value = utils
                    .getAnnotationValue(modelSpecElement, Implements.class, "interfaceDefinitions");
            List<AnnotationMirror> interfaceSpecs = utils.getValuesFromAnnotationValue(value,
                    AnnotationMirror.class);

            for (AnnotationMirror spec : interfaceSpecs) {
                AnnotationValue interfaceClassValue = utils.getAnnotationValueFromMirror(spec, "interfaceClass");
                List<DeclaredTypeName> interfaceClassList = utils.getTypeNamesFromAnnotationValue(interfaceClassValue);
                if (!AptUtils.isEmpty(interfaceClassList)) {
                    DeclaredTypeName interfaceClass = interfaceClassList.get(0);

                    AnnotationValue interfaceTypeArgsValue = utils
                            .getAnnotationValueFromMirror(spec, "interfaceTypeArgs");
                    List<DeclaredTypeName> typeArgs = utils.getTypeNamesFromAnnotationValue(interfaceTypeArgsValue);
                    if (AptUtils.isEmpty(typeArgs)) {
                        List<String> typeArgNames = utils.getValuesFromAnnotationValue(
                                utils.getAnnotationValueFromMirror(spec, "interfaceTypeArgNames"), String.class);
                        for (String typeArgName : typeArgNames) {
                            typeArgs.add(new DeclaredTypeName(typeArgName));
                        }
                    }
                    interfaceClass.setTypeArgs(typeArgs);
                    interfaces.add(interfaceClass);
                }
            }
        }
    }
}
