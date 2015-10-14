/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.android;

import android.util.Log;

import com.yahoo.squidb.utility.Logger;

public class AndroidLogger extends Logger {

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
