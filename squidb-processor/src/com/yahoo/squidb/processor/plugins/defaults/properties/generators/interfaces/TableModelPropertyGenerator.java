/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.plugins.defaults.properties.generators.interfaces;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;

/**
 * Base interface for {@link PropertyGenerator}s specific to table models
 */
public interface TableModelPropertyGenerator extends PropertyGenerator {

    /**
     * Called to create declaration of the property itself. Plugins will have a chance to modify the params
     *
     * @param tableModelArgName the name of the first argument to most property constructors (the TableModelName)
     */
    FieldSpec.Builder buildTablePropertyDeclaration(String tableModelArgName);

    /**
     * @return the name of the underlying column in SQLite to use for this property
     */
    String getColumnName();

    /**
     * @return the constraints for this column as a SQL string
     */
    String getConstraintString();

    /**
     * Creates a {@link CodeBlock} expression representing a call to ValuesStorage.put for adding a property default
     * to the model default values. May return null if the property has no default.
     *
     * @param contentValuesName the name of the content values variable to call
     * @return a {@link CodeBlock} representing the call to put a default value, or null for no expression
     */
    CodeBlock buildPutDefault(String contentValuesName);
}
