/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.android;

import android.util.Log;

import com.yahoo.squidb.utility.Logger;

/**
 * Logger implementation that logs using {@link android.util.Log}
 */
public class AndroidLogger extends Logger {

    @Override
    public void log(Level level, String tag, String message, Throwable t) {
        switch (level) {
            case INFO:
                Log.i(tag, message, t);
                break;
            case DEBUG:
                Log.d(tag, message, t);
                break;
            case WARN:
                Log.w(tag, message, t);
                break;
            case ERROR:
                Log.e(tag, message, t);
                break;
            case ASSERT:
                Log.wtf(tag, message, t);
                break;
        }
    }
}
