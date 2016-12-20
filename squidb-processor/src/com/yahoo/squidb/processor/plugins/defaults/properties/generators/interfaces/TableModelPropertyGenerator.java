/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.plugins.defaults.properties.generators.interfaces;

import com.yahoo.aptutils.writer.JavaFileWriter;

import java.io.IOException;

/**
 * Base interface for {@link PropertyGenerator}s specific to table models
 */
public interface TableModelPropertyGenerator extends PropertyGenerator {

    /**
     * Called to write the declaration of the property itself
     *
     * @param writer a {@link JavaFileWriter} for writing to
     */
    void emitTablePropertyDeclaration(JavaFileWriter writer, String tableModelArgName) throws IOException;

    /**
     * @return the name of the underlying column in SQLite to use for this property
     */
    String getColumnName();

    /**
     * @return the constraints for this column as a SQL string
     */
    String getConstraintString();

    /**
     * Called to emit a call to ContentValues.put for adding a property default to the model default values
     *
     * @param writer a {@link JavaFileWriter} for writing to
     * @param contentValuesName the name of the content values variable to call
     */
    void emitPutDefault(JavaFileWriter writer, String contentValuesName) throws IOException;
}
