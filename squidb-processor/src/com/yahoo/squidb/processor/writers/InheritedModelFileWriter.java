/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.writers;

import com.yahoo.aptutils.model.DeclaredTypeName;
import com.yahoo.aptutils.utils.AptUtils;
import com.yahoo.aptutils.writer.expressions.Expressions;
import com.yahoo.squidb.annotations.ColumnSpec;
import com.yahoo.squidb.annotations.InheritedModelSpec;
import com.yahoo.squidb.processor.TypeConstants;
import com.yahoo.squidb.processor.properties.factory.PropertyGeneratorFactory;
import com.yahoo.squidb.processor.properties.generators.PropertyGenerator;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.tools.Diagnostic.Kind;

public class InheritedModelFileWriter extends ModelFileWriter<InheritedModelSpec> {

    private final DeclaredTypeName superclass;

    public InheritedModelFileWriter(TypeElement element, PropertyGeneratorFactory propertyGeneratorFactory,
            AptUtils utils) {
        super(element, InheritedModelSpec.class, propertyGeneratorFactory, utils);
        this.superclass = new DeclaredTypeName(modelSpec.inheritsFrom());
    }

    @Override
    protected String getGeneratedClassName() {
        return modelSpec.className();
    }

    @Override
    protected void processVariableElement(VariableElement e, DeclaredTypeName typeName) {
        if (e.getAnnotation(Deprecated.class) != null) {
            return;
        }
        if (e.getAnnotation(ColumnSpec.class) != null) {
            utils.getMessager().printMessage(Kind.WARNING, "ColumnSpec is ignored outside of table models", e);
        }
        Set<Modifier> modifiers = e.getModifiers();
        if (modifiers.containsAll(TypeConstants.PUBLIC_STATIC_FINAL)) {
            if (!TypeConstants.isPropertyType(typeName)) {
                constantElements.add(e);
            } else {
                initializePropertyGenerator(e);
            }
        } else {
            utils.getMessager().printMessage(Kind.WARNING, "Unused field in spec", e);
        }
    }

    @Override
    protected DeclaredTypeName getModelSuperclass() {
        return superclass;
    }

    @Override
    protected void emitModelSpecificFields() throws IOException {
        // Nothing to do
    }

    @Override
    protected Collection<DeclaredTypeName> getModelSpecificImports() {
        return Arrays.asList(superclass);
    }

    @Override
    protected void emitAllProperties() throws IOException {
        for (PropertyGenerator e : propertyGenerators) {
            emitSinglePropertyDeclaration(e);
        }
    }

    private void emitSinglePropertyDeclaration(PropertyGenerator generator) throws IOException {
        generator.beforeEmitPropertyDeclaration(writer);
        writer.writeFieldDeclaration(generator.getPropertyType(), generator.getPropertyName(),
                Expressions.staticReference(sourceElementName, generator.getPropertyName()),
                TypeConstants.PUBLIC_STATIC_FINAL)
                .writeNewline();
        generator.afterEmitPropertyDeclaration(writer);
    }

    @Override
    protected void emitPropertiesArray() throws IOException {
        writer.writeFieldDeclaration(TypeConstants.PROPERTY_ARRAY, PROPERTIES_ARRAY_NAME,
                Expressions.staticReference(getModelSuperclass(), PROPERTIES_ARRAY_NAME),
                TypeConstants.PUBLIC_STATIC_FINAL);
    }

    @Override
    protected void writePropertiesInitializationBlock() throws IOException {
        // Not needed
    }

    @Override
    protected void emitPropertyArrayInitialization() throws IOException {
        // The superclass declares this
    }

    @Override
    protected void emitDefaultValues() throws IOException {
        // Override: do nothing, the superclass should take care of default values
    }

    @Override
    protected void emitDefaultValuesInitializationBlock() throws IOException {
        // Nothing to do, see above
    }

}
