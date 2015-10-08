/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.utility;

import android.util.Log;

import java.io.PrintStream;

public abstract class Logger {

    public static final String LOG_TAG = "squidb";

    public enum Level {
        INFO,
        DEBUG,
        WARN,
        ERROR
    }

    private static Logger logger = new DefaultLogger();

    public static synchronized void setLogger(Logger newLogger) {
        if (newLogger == null) {
            newLogger = new DefaultLogger();
        }
        logger = newLogger;
    }

    public static void i(String message) {
        i(message, null);
    }

    public static void i(String message, Throwable t) {
        logger.log(Level.INFO, message, t);
    }

    public static void d(String message) {
        d(message, null);
    }

    public static void d(String message, Throwable t) {
        logger.log(Level.DEBUG, message, t);
    }

    public static void w(String message) {
        w(message, null);
    }

    public static void w(String message, Throwable t) {
        logger.log(Level.WARN, message, t);
    }

    public static void e(String message) {
        e(message, null);
    }

    public static void e(String message, Throwable t) {
        logger.log(Level.ERROR, message, t);
    }

    public static class DefaultLogger extends Logger {

        @Override
        public void log(Level level, String message, Throwable t) {
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
            stream.println(LOG_TAG + ": " + message);
            if (t != null) {
                t.printStackTrace(stream);
            }
        }
    }

    public static class AndroidLogger extends Logger {

        @Override
        public void log(Level level, String message, Throwable t) {
            switch (level) {
                case INFO:
                    Log.i(LOG_TAG, message, t);
                    break;
                case DEBUG:
                    Log.d(LOG_TAG, message, t);
                    break;
                case WARN:
                    Log.w(LOG_TAG, message, t);
                    break;
                case ERROR:
                    Log.e(LOG_TAG, message, t);
                    break;
            }
        }
    }

    public abstract void log(Level level, String message, Throwable t);
}
