/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.sql;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Default implementation of {@link ArgumentResolver} that unwraps AtomicReferences, AtomicBooleans, ThreadLocals, and
 * Enum values. Users can extend DefaultArgumentResolver by overriding {@link #canResolveCustomType(Object)} and
 * {@link #resolveCustomType(Object)} to handle resolving types that are not handled by DefaultArgumentResolver, or to
 * handle one of the default types in a different way (for example, to resolve Enums using ordinals instead of names).
 */
public class DefaultArgumentResolver implements ArgumentResolver {

    @Override
    public final Object resolveArgument(Object arg) {
        while (true) {
            if (canResolveCustomType(arg)) {
                arg = resolveCustomType(arg);
            } else if (arg instanceof AtomicReference) {
                arg = ((AtomicReference<?>) arg).get();
            } else if (arg instanceof AtomicBoolean) { // Not a subclass of Number so we need to unwrap it
                return ((AtomicBoolean) arg).get() ? 1 : 0;
            } else if (arg instanceof Enum<?>) {
                return ((Enum<?>) arg).name();
            } else if (arg instanceof ThreadLocal) {
                arg = ((ThreadLocal<?>) arg).get();
            } else {
                return arg;
            }
        }
    }

    /**
     * Users can override this method if they want to provide custom logic for resolving/unwrapping the given argument.
     *
     * @return true if the user wants to handle the argument using {@link #resolveCustomType(Object)}, false if the
     * default resolution logic should be used for this argument
     */
    protected boolean canResolveCustomType(Object arg) {
        return false;
    }

    /**
     * Users can override this method if they want to provide custom logic for resolving/unwrapping the given argument.
     * This method will only be called if {@link #canResolveCustomType(Object)} returns true for some argument.
     *
     * @return the result of resolving/unwrapping the given argument.
     */
    protected Object resolveCustomType(Object arg) {
        throw new UnsupportedOperationException("DefaultArgumentResolver#resolveCustomType unimplemented. This "
                + "instance of DefaultArgumentResolver declared it could handle a type by returning true in "
                + "canResolveCustomType, but did not override resolveCustomType to resolve it.");
    }
}
