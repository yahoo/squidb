/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.plugins.defaults.properties.generators.interfaces;

import com.yahoo.aptutils.writer.JavaFileWriter;

import java.io.IOException;

/**
 * Base interface for {@link PropertyGenerator}s specific to view models
 */
public interface ViewModelPropertyGenerator extends PropertyGenerator {

    /**
     * Emit a reference to the Property object that should be added to this view. Typically this would be a reference
     * to a constant field in the model spec, e.g. <code>MyViewModelSpec.SOME_PROPERTY</code>. This method must emit
     * the same code every time it is called.
     *
     * @param writer a {@link JavaFileWriter} for writing to
     */
    void emitViewPropertyReference(JavaFileWriter writer) throws IOException;

    /**
     * @return the alias to use for this property in the view model. If this returns null or empty string, a unique
     * alias may be generated at runtime to prevent name clashing with other properties in the view model.
     */
    String getAlias();

}
