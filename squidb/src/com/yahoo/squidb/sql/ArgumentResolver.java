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
 * The default implementation of this interface that SquiDB uses can be found in {@link DefaultArgumentResolver}.
 * Users who wish to handle custom types should generally prefer to subclass DefaultArgumentResolver to handle said
 * types, which avoids changing SquiDB's default handling of other argument types. If you really want to override
 * SquiDB's default argument handling, you can implement this interface directly.
 */
public interface ArgumentResolver {

    /**
     * Resolve the given argument into a primitive suitable for binding to a low-level SQLite statement. Suitable
     * primitives are:
     * <ul>
     * <li>Floating-point numbers (float, double), which will be bound as double</li>
     * <li>Integer numbers (int, long, or any non-floating point subclass of Number),
     * which will be bound as long</li>
     * <li>Boolean, which will be bound as 1 for true or 0 for false</li>
     * <li>byte[], which will be bound as a blob</li>
     * <li>String</li>
     * <li>null</li>
     * </ul>
     * If the argument resolver does not map the given argument into one of these primitive types, it will eventually
     * be bound to the statement as a string using toString().
     */
    Object resolveArgument(Object arg);

}
