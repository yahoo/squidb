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
import com.yahoo.squidb.processor.plugins.PluginWriter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.TypeElement;

/**
 * A {@link Plugin} that controls declaring that model classes implement interfaces. This plugin looks for and parses
 * the value of any {@link Implements} annotations on the given model spec to determine which interfaces to add. It is
 * enabled by default. It can be disabled by passing a bitmask with the
 * {@link com.yahoo.squidb.processor.plugins.PluginContext#OPTIONS_DISABLE_IMPLEMENTS_HANDLING} flag set.
 */
public class ImplementsPlugin extends Plugin {

    public ImplementsPlugin(AptUtils utils) {
        super(utils);
    }

    @Override
    public List<? extends PluginWriter> getWritersForModelSpec(ModelSpec<?> modelSpec) {
        final List<DeclaredTypeName> interfaces = new ArrayList<DeclaredTypeName>();
        TypeElement modelSpecElement = modelSpec.getModelSpecElement();
        if (modelSpecElement.getAnnotation(Implements.class) != null) {
            List<DeclaredTypeName> typeNames = utils.getTypeNamesFromAnnotationValue(
                    utils.getAnnotationValue(modelSpecElement, Implements.class, "interfaceClasses"));
            if (!AptUtils.isEmpty(typeNames)) {
                interfaces.addAll(typeNames);
            }

            AnnotationValue value = utils.getAnnotationValue(modelSpecElement, Implements.class, "interfaceDefinitions");
            List<AnnotationMirror> interfaceSpecs = utils.getValuesFromAnnotationValue(value,
                    AnnotationMirror.class);

            for (AnnotationMirror spec : interfaceSpecs) {
                AnnotationValue interfaceClassValue = utils.getAnnotationValueFromMirror(spec, "interfaceClass");
                List<DeclaredTypeName> interfaceClassList = utils.getTypeNamesFromAnnotationValue(interfaceClassValue);
                if (!AptUtils.isEmpty(interfaceClassList)) {
                    DeclaredTypeName interfaceClass = interfaceClassList.get(0);

                    AnnotationValue interfaceTypeArgsValue = utils.getAnnotationValueFromMirror(spec, "interfaceTypeArgs");
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
        if (!AptUtils.isEmpty(interfaces)) {
            return Collections.singletonList(new ImplementsWriter(interfaces));
        }
        return null;
    }

    private class ImplementsWriter extends PluginWriter {

        private final List<DeclaredTypeName> interfaces;

        private ImplementsWriter(List<DeclaredTypeName> interfaces) {
            this.interfaces = interfaces;
        }

        @Override
        public void addRequiredImports(Set<DeclaredTypeName> imports) {
            utils.accumulateImportsFromTypeNames(imports, interfaces);
        }

        @Override
        public void addInterfacesToImplement(Set<DeclaredTypeName> interfaces) {
            interfaces.addAll(this.interfaces);
        }
    }
}
