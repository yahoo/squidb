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
    private final ArgumentResolver argumentResolver;
    private final Map<String, Object> extras;

    public static class Builder {

        private VersionCode versionCode;
        private ArgumentResolver argumentResolver = new DefaultArgumentResolver();
        private Map<String, Object> extras = new HashMap<>();

        public Builder(VersionCode versionCode) {
            if (versionCode == null) {
                throw new IllegalArgumentException("Can't construct a CompileContext with a null VersionCode");
            }
            this.versionCode = versionCode;
        }

        public CompileContext build() {
            return new CompileContext(this);
        }

        public Builder setArgumentResolver(ArgumentResolver argumentResolver) {
            this.argumentResolver = argumentResolver;
            return this;
        }

        public Builder setExtra(String key, Object value) {
            this.extras.put(key, value);
            return this;
        }

        public Builder clearExtra(String key) {
            this.extras.remove(key);
            return this;
        }

    }

    private CompileContext(Builder builder) {
        this.versionCode = builder.versionCode;
        this.argumentResolver = builder.argumentResolver;
        this.extras = new HashMap<>(builder.extras);
    }

    public static CompileContext defaultContextForVersionCode(VersionCode sqliteVersion) {
        return new CompileContext.Builder(sqliteVersion).build();
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
