/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.utility;

import android.content.ContentValues;
import android.content.Context;
import android.util.Log;

import com.yahoo.squidb.data.SquidCursor;
import com.yahoo.squidb.data.SquidDatabase;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Collections;

/**
 * Various utility functions for SquiDB
 */
public class SquidUtilities {

    public static final String LOG_TAG = "squidb";

    /**
     * Put an arbitrary object into a {@link ContentValues}
     *
     * @param target the ContentValues store
     * @param key the key to use
     * @param value the value to store
     */
    public static void putInto(ContentValues target, String key, Object value, boolean errorOnFail) {
        if (value == null) {
            target.putNull(key);
        } else if (value instanceof Boolean) {
            target.put(key, (Boolean) value);
        } else if (value instanceof Byte) {
            target.put(key, (Byte) value);
        } else if (value instanceof Double) {
            target.put(key, (Double) value);
        } else if (value instanceof Float) {
            target.put(key, (Float) value);
        } else if (value instanceof Integer) {
            target.put(key, (Integer) value);
        } else if (value instanceof Long) {
            target.put(key, (Long) value);
        } else if (value instanceof Short) {
            target.put(key, (Short) value);
        } else if (value instanceof String) {
            target.put(key, (String) value);
        } else if (value instanceof byte[]) {
            target.put(key, (byte[]) value);
        } else if (errorOnFail) {
            throw new UnsupportedOperationException("Could not handle type " + value.getClass());
        }
    }

    /**
     * Dump the contents of the cursor to the system log, formatted in a readable way
     *
     * @param cursor the cursor to dump
     */
    public static void dumpCursor(SquidCursor<?> cursor) {
        dumpCursor(cursor, 20);
    }

    /**
     * Dump the contents of the cursor to the system log, formatted in a readable way
     *
     * @param cursor the cursor to dump
     * @param maxColumnWidth maximum width for each column
     */
    public static void dumpCursor(SquidCursor<?> cursor, int maxColumnWidth) {
        StringBuilder builder = new StringBuilder("\n");
        dumpCursor(cursor, maxColumnWidth, builder);
        Log.d(LOG_TAG, builder.toString());
    }

    /**
     * Dump the contents of the cursor to the provided builder, formatted in a readable way
     *
     * @param cursor the cursor to dump
     * @param builder the builder to append to
     */
    public static void dumpCursor(SquidCursor<?> cursor, StringBuilder builder) {
        dumpCursor(cursor, 20, builder);
    }

    /**
     * Dump the contents of the cursor to the provided builder, formatted in a readable way
     *
     * @param cursor the cursor to dump
     * @param maxColumnWidth maximum width for each column
     * @param builder the builder to append to
     */
    public static void dumpCursor(SquidCursor<?> cursor, int maxColumnWidth, StringBuilder builder) {
        if (cursor == null) {
            builder.append("Cursor is null");
            return;
        }

        String[] columnNames = cursor.getColumnNames();
        for (String col : columnNames) {
            addColumnToRowBuilder(builder, col, maxColumnWidth);
        }
        builder.append('\n');
        for (int i = 0; i < (maxColumnWidth + 1) * columnNames.length; i++) {
            builder.append('=');
        }
        builder.append('\n');

        int position = cursor.getPosition();
        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
            dumpCurrentRow(cursor, maxColumnWidth, builder);
            builder.append('\n');
        }
        cursor.moveToPosition(position); // reset
    }

    /**
     * Dump the contents of the current row to the system log
     *
     * @param cursor the cursor, with its position already moved to the desired row
     */
    public static void dumpCurrentRow(SquidCursor<?> cursor) {
        dumpCurrentRow(cursor, 20);
    }

    /**
     * Dump the contents of the current row to the system log
     *
     * @param cursor the cursor, with its position already moved to the desired row
     * @param maxColumnWidth maximum width for each column
     */
    public static void dumpCurrentRow(SquidCursor<?> cursor, int maxColumnWidth) {
        StringBuilder builder = new StringBuilder("\n");
        dumpCurrentRow(cursor, maxColumnWidth, builder);
        Log.d(LOG_TAG, builder.toString());
    }

    /**
     * Dump the contents of the current row to the provided builder
     *
     * @param cursor the cursor, with its position already moved to the desired row
     * @param builder the builder to append to
     */
    public static void dumpCurrentRow(SquidCursor<?> cursor, StringBuilder builder) {
        dumpCurrentRow(cursor, 20, builder);
    }

    /**
     * Dump the contents of the current row to the provided builder
     *
     * @param cursor the cursor, with its position already moved to the desired row
     * @param maxColumnWidth maximum width for each column
     * @param builder the builder to append to
     */
    public static void dumpCurrentRow(SquidCursor<?> cursor, int maxColumnWidth, StringBuilder builder) {
        for (int i = 0, count = cursor.getColumnCount(); i < count; i++) {
            addColumnToRowBuilder(builder, cursor.getString(i), maxColumnWidth);
        }
    }

    private static void addColumnToRowBuilder(StringBuilder builder, String value, int maxColumnWidth) {
        if (value == null) {
            value = "null";
        }
        if (value.length() > maxColumnWidth) {
            builder.append(value.substring(0, maxColumnWidth - 3)).append("...");
        } else {
            builder.append(value);
            for (int i = 0; i < maxColumnWidth - value.length(); i++) {
                builder.append(' ');
            }
        }
        builder.append('|');
    }

    /**
     * A version of {@link Collection#addAll(Collection)} that works on varargs without calling Arrays.asList, which is
     * a performance and memory boost
     */
    public static <T> void addAll(Collection<T> collection, T[] objects) {
        if (objects != null) {
            Collections.addAll(collection, objects);
        }
    }

    // --- serialization

    /**
     * Copy database files to the given folder. Useful for debugging.
     *
     * @param context a Context
     * @param database the SquidDatabase to copy
     * @param toFolder the directory to copy files into
     */
    public static void copyDatabase(Context context, SquidDatabase database, String toFolder) {
        File folderFile = new File(toFolder);
        if (!(folderFile.mkdirs() || folderFile.isDirectory())) {
            Log.e(LOG_TAG, "Error creating directories for database copy");
            return;
        }
        File dbFile = context.getDatabasePath(database.getName());
        try {
            copyFile(dbFile, new File(folderFile.getAbsolutePath() + File.separator + database.getName()));
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error copying database " + database.getName(), e);
        }
    }

    /**
     * Copy a file from one place to another
     */
    private static void copyFile(File in, File out) throws Exception {
        FileInputStream fis = new FileInputStream(in);
        FileOutputStream fos = new FileOutputStream(out);
        try {
            copyStream(fis, fos);
        } finally {
            fis.close();
            fos.close();
        }
    }

    /**
     * Copy a stream from source to destination
     */
    private static void copyStream(InputStream source, OutputStream dest) throws IOException {
        int bytes;
        byte[] buffer;
        int BUFFER_SIZE = 1024;
        buffer = new byte[BUFFER_SIZE];
        while ((bytes = source.read(buffer)) != -1) {
            if (bytes == 0) {
                bytes = source.read();
                if (bytes < 0) {
                    break;
                }
                dest.write(bytes);
                dest.flush();
                continue;
            }

            dest.write(buffer, 0, bytes);
            dest.flush();
        }
    }
}
