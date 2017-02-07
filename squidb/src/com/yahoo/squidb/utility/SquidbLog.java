/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.utility;

import java.io.PrintStream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * This class duplicates some of the concepts and interfaces of android.util.Log to facilitate logging in a
 * platform-independent way. The default logger logs to System.out or System.err depending on the log level,
 * but alternative loggers can be used using {@link #setLogger(Logger)}.
 */
public final class SquidbLog {

    public static final String LOG_TAG = "squidb";

    public enum Level {
        ASSERT,
        ERROR,
        WARN,
        DEBUG,
        INFO,
    }

    private SquidbLog() {
        // No instantiation
    }

    private static Level logLevel = Level.DEBUG;
    private static Logger logger = new DefaultLogger();

    public static synchronized void setLogger(@Nonnull Logger newLogger) {
        if (newLogger == null) {
            newLogger = new DefaultLogger();
        }
        logger = newLogger;
    }

    public static boolean isLoggable(@Nullable String tag, @Nonnull Level level) {
        return logLevel.ordinal() >= level.ordinal();
    }

    public static synchronized void setLogLevel(@Nonnull Level newLevel) {
        if (newLevel != null) {
            logLevel = newLevel;
        }
    }

    public static void i(@Nullable String tag, @Nullable String message) {
        i(tag, message, null);
    }

    public static void i(@Nullable String tag, @Nullable String message, @Nullable Throwable t) {
        logForLevel(Level.INFO, tag, message, t);
    }

    public static void d(@Nullable String tag, @Nullable String message) {
        d(tag, message, null);
    }

    public static void d(@Nullable String tag, @Nullable String message, @Nullable Throwable t) {
        logForLevel(Level.DEBUG, tag, message, t);
    }

    public static void w(@Nullable String tag, @Nullable String message) {
        w(tag, message, null);
    }

    public static void w(@Nullable String tag, @Nullable String message, @Nullable Throwable t) {
        logForLevel(Level.WARN, tag, message, t);
    }

    public static void e(@Nullable String tag, @Nullable String message) {
        e(tag, message, null);
    }

    public static void e(@Nullable String tag, @Nullable String message, @Nullable Throwable t) {
        logForLevel(Level.ERROR, tag, message, t);
    }

    public static void wtf(@Nullable String tag, @Nullable String message) {
        wtf(tag, message, null);
    }

    public static void wtf(@Nullable String tag, @Nullable String message, @Nullable Throwable t) {
        logForLevel(Level.ASSERT, tag, message, t);
    }

    private static void logForLevel(@Nonnull Level level, @Nullable String tag, @Nullable String message,
            @Nullable Throwable t) {
        if (isLoggable(tag, level)) {
            logger.log(level, tag, message, t);
        }
    }

    public static class DefaultLogger implements Logger {

        @Override
        public void log(@Nonnull Level level, @Nullable String tag, @Nullable String message, @Nullable Throwable t) {
            PrintStream stream;
            switch (level) {
                case INFO:
                case DEBUG:
                case WARN:
                    stream = System.out;
                    break;
                case ERROR:
                case ASSERT:
                default:
                    stream = System.err;
                    break;
            }
            if (tag != null) {
                stream.print(tag);
                stream.print(": ");
            }
            stream.println(message);
            if (t != null) {
                t.printStackTrace(stream);
            }
        }
    }

}
