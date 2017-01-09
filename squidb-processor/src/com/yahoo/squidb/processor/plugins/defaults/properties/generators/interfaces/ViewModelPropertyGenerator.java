/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.plugins.defaults.properties.generators.interfaces;

import com.squareup.javapoet.CodeBlock;

/**
 * Base interface for {@link PropertyGenerator}s specific to view models
 */
public interface ViewModelPropertyGenerator extends PropertyGenerator {

    /**
     * Create a reference to the Property object that should be added to this view. Typically this would be a reference
     * to a constant field in the model spec, e.g. <code>MyViewModelSpec.SOME_PROPERTY</code>. This method must create
     * the same code every time it is called.
     *
     * @param alias true if the reference should include a <code>.as(getAlias())</code> call, false otherwise
     * @return a {@link CodeBlock} representing the property reference expression
     */
    CodeBlock buildViewPropertyReference(boolean alias);

    /**
     * @return the alias to use for this property in the view model. If this returns null or empty string, a unique
     * alias may be generated at runtime to prevent name clashing with other properties in the view model.
     */
    String getAlias();

}
