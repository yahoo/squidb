/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.sql;

/**
 * This interface defines methods for resolving higher-level objects found in SQL builders like Query into the
 * lower-level primitives that are bound to the SQLite query at execution time. For example, an implementation of
 * this interface might unwrap the contents of an AtomicReference, or convert an AtomicBoolean into an integer.
 * <p>
 * The default implementation of this interface that SquiDB uses can be found in {@link DefaultArgumentResolver}
 */
public interface ArgumentResolver {

    /**
     * Resolve the given argument into a primitive suitable for binding to a low-level SQLite statement. If the
     * argument resolver does not map the given argument into a number, string, boolean, blob, etc., it will eventually
     * be bound to the statement as a string using toString().
     */
    Object resolveArgument(Object arg);

}
