package com.yahoo.squidb.utility;

import android.util.Log;

public abstract class Logger {

    public static final String LOG_TAG = "squidb";

    private static Logger logger = new DefaultLogger();

    public static synchronized void setLogger(Logger newLogger) {
        if (newLogger == null) {
            newLogger = new DefaultLogger();
        }
        logger = newLogger;
    }

    public static void log(String message) {
        logger.logMessage(message);
    }

    static class DefaultLogger extends Logger {

        @Override
        public void logMessage(String message) {
            Log.w(LOG_TAG, message);
        }
    }

    public abstract void logMessage(String message);

}
