/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.utility;

import java.io.PrintStream;

public abstract class Logger {

    public static final String LOG_TAG = "squidb";

    public enum Level {
        ERROR,
        WARN,
        DEBUG,
        INFO,
    }

    private static Level logLevel = Level.DEBUG;
    private static Logger logger = new DefaultLogger();

    public static synchronized void setLogger(Logger newLogger) {
        if (newLogger == null) {
            newLogger = new DefaultLogger();
        }
        logger = newLogger;
    }

    public static boolean isLoggable(String tag, Level level) {
        return logLevel.ordinal() >= level.ordinal();
    }

    public static synchronized void setLogLevel(Level newLevel) {
        logLevel = newLevel;
    }

    public static void i(String tag, String message) {
        i(tag, message, null);
    }

    public static void i(String tag, String message, Throwable t) {
        if (isLoggable(tag, Level.INFO)) {
            logger.log(Level.INFO, tag, message, t);
        }
    }

    public static void d(String tag, String message) {
        d(tag, message, null);
    }

    public static void d(String tag, String message, Throwable t) {
        if (isLoggable(tag, Level.DEBUG)) {
            logger.log(Level.DEBUG, tag, message, t);
        }
    }

    public static void w(String tag, String message) {
        w(tag, message, null);
    }

    public static void w(String tag, String message, Throwable t) {
        if (isLoggable(tag, Level.WARN)) {
            logger.log(Level.WARN, tag, message, t);
        }
    }

    public static void e(String tag, String message) {
        e(tag, message, null);
    }

    public static void e(String tag, String message, Throwable t) {
        if (isLoggable(tag, Level.ERROR)) {
            logger.log(Level.ERROR, tag, message, t);
        }
    }

    public static class DefaultLogger extends Logger {

        @Override
        public void log(Level level, String tag, String message, Throwable t) {
            PrintStream stream;
            switch (level) {
                case INFO:
                case DEBUG:
                case WARN:
                    stream = System.out;
                    break;
                case ERROR:
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

    public abstract void log(Level level, String tag, String message, Throwable t);
}
