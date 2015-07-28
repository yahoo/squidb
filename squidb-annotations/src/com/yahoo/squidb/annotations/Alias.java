/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Used to specify an alias for a Property in a ViewModel. Only relevant in model specs using
 * {@link com.yahoo.squidb.annotations.ViewModelSpec}. For example:
 *
 * <pre>
 *     &#064;Alias("myAlias")
 *     LongProperty ALIASED_ID = Model.ID;
 * </pre>
 * in a view model spec would yield:
 * <pre>
 *     select model._id as myAlias
 * </pre>
 * in the view definition.
 */
@Target(ElementType.FIELD)
public @interface Alias {

    String value();

}
