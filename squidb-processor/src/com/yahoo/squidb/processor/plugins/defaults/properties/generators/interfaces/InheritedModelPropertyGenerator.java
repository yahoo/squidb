/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.plugins.defaults.properties.generators.interfaces;

import com.squareup.javapoet.FieldSpec;

/**
 * Base interface for {@link PropertyGenerator}s specific to inherited models
 */
public interface InheritedModelPropertyGenerator extends PropertyGenerator {

    /**
     * Called to build the declaration of the inherited model property. This should be an expression of the form:
     * <pre>PropertyType PROPERTY_NAME = Superclass.SOME_PROPERTY;</pre>
     * e.g.
     * <pre>public static final StringProperty ADDRESS = Data.DATA_1;</pre>
     */
    FieldSpec.Builder buildInheritedPropertyDeclaration();

}
