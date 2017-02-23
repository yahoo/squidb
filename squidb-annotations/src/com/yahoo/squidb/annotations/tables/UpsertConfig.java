/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.annotations.tables;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Optional annotation to allow model specs declaring {@link UpsertKey}s to customize the behavior of the methods
 * implementing the Upsertable contract.
 */
@Target(ElementType.TYPE)
public @interface UpsertConfig {

    /**
     * Configures the behavior of the Upsertable.rowidHasPriority implementation for the generated model. See the
     * Upsertable interface in the squidb for more details. Default is true; users should consider very carefully
     * before changing this value to false.
     */
    boolean rowidHasPriority() default true;

    /**
     * Configures the behavior of the Upsertable.getUpsertKeyLookupCriterion() implementation for the generated model.
     * If the model instance passed to upsert is not populated with the values necessary to construct a matching
     * criterion, a value of true for this field will throw a runtime exception. A value of false will
     * make upsert fail quietly by simply returning false to indicate the upsert failed. Default is true.
     */
    boolean missingLookupValueThrows() default true;

}
