/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.sql;

import com.yahoo.squidb.utility.VersionCode;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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

        public Builder(@Nonnull VersionCode versionCode) {
            if (versionCode == null) {
                throw new IllegalArgumentException("Can't construct a CompileContext with a null VersionCode");
            }
            this.versionCode = versionCode;
        }

        @Nonnull
        public CompileContext build() {
            return new CompileContext(this);
        }

        @Nonnull
        public Builder setArgumentResolver(@Nonnull ArgumentResolver argumentResolver) {
            if (argumentResolver == null) {
                throw new IllegalArgumentException("Can't construct a CompileContext with a null ArgumentResolver");
            }
            this.argumentResolver = argumentResolver;
            return this;
        }

        @Nonnull
        public Builder setExtra(@Nullable String key, @Nullable Object value) {
            this.extras.put(key, value);
            return this;
        }

        @Nonnull
        public Builder clearExtra(@Nullable String key) {
            this.extras.remove(key);
            return this;
        }

    }

    private CompileContext(@Nonnull Builder builder) {
        this.versionCode = builder.versionCode;
        this.argumentResolver = builder.argumentResolver;
        this.extras = new HashMap<>(builder.extras);
    }

    @Nonnull
    public static CompileContext defaultContextForVersionCode(@Nonnull VersionCode sqliteVersion) {
        return new CompileContext.Builder(sqliteVersion).build();
    }

    /**
     * @return The SQLite version for this CompileContext
     */
    @Nonnull
    public VersionCode getVersionCode() {
        return versionCode;
    }

    /**
     * @return the {@link ArgumentResolver} for this CompileContext
     */
    @Nonnull
    public ArgumentResolver getArgumentResolver() {
        return argumentResolver;
    }

    /**
     * @return the extra value set for the given key, or null if one does not exist
     */
    @Nullable
    public Object getExtra(@Nullable String key) {
        return extras.get(key);
    }

    /**
     * Sets the extra value for the given key
     *
     * @return this CompileContext, to chain builder-style calls
     */
    @Nonnull
    public CompileContext setExtra(@Nullable String key, @Nullable Object value) {
        extras.put(key, value);
        return this;
    }

    /**
     * Removes the extra value for the given key if it exists
     *
     * @return this CompileContext, to chain builder-style calls
     */
    @Nonnull
    public CompileContext clearExtra(@Nullable String key) {
        extras.remove(key);
        return this;
    }

    /**
     * @return true if there is a value stored for the given key, false otherwise
     */
    public boolean hasExtra(@Nullable String key) {
        return extras.containsKey(key);
    }

}
