/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.utility;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Interface for logging used in conjunction with {@link SquidbLog}
 */
public interface Logger {

    void log(@Nonnull SquidbLog.Level level, @Nullable String tag, @Nullable String message, @Nullable Throwable t);
}
