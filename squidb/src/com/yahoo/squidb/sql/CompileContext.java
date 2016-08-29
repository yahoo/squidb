/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.sql;

import com.yahoo.squidb.utility.VersionCode;

import java.util.HashMap;
import java.util.Map;

/**
 * Holds various information relevant to compiling SQL statements, including SQLite
 * {@link com.yahoo.squidb.utility.VersionCode} and an {@link ArgumentResolver} strategy for resolving higher-level
 * arguments. Users can attach arbitrary metadata to the context with key-value storage using
 * {@link #getExtra(String)}, {@link #setExtra(String, Object)}, and {@link #clearExtra(String)}.
 */
public class CompileContext {

    private final VersionCode versionCode;
    private ArgumentResolver argumentResolver = new DefaultArgumentResolver();
    private final Map<String, Object> extras = new HashMap<>();

    public CompileContext(VersionCode versionCode) {
        this.versionCode = versionCode;
    }

    /**
     * @return The SQLite version for this CompileContext
     */
    public VersionCode getVersionCode() {
        return versionCode;
    }

    /**
     * @return the {@link ArgumentResolver} for this CompileContext
     */
    public ArgumentResolver getArgumentResolver() {
        return argumentResolver;
    }

    /**
     * @param argumentResolver new argument resolver to use. Attempting to pass null is a no-op.
     * @return this CompileContext, to chain builder-style calls
     */
    public CompileContext setArgumentResolver(ArgumentResolver argumentResolver) {
        if (argumentResolver != null) {
            this.argumentResolver = argumentResolver;
        }
        return this;
    }

    /**
     * @return the extra value set for the given key, or null if one does not exist
     */
    public Object getExtra(String key) {
        return extras.get(key);
    }

    /**
     * Sets the extra value for the given key
     *
     * @return this CompileContext, to chain builder-style calls
     */
    public CompileContext setExtra(String key, Object value) {
        extras.put(key, value);
        return this;
    }

    /**
     * Removes the extra value for the given key if it exists
     *
     * @return this CompileContext, to chain builder-style calls
     */
    public CompileContext clearExtra(String key) {
        extras.remove(key);
        return this;
    }

    /**
     * @return true if there is a value stored for the given key, false otherwise
     */
    public boolean hasExtra(String key) {
        return extras.containsKey(key);
    }

}
