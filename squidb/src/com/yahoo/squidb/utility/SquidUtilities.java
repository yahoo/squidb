/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.utility;

import com.yahoo.squidb.data.ICursor;
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

    /**
     * Dump the contents of the cursor to the provided builder, formatted in a readable way
     *
     * @param cursor the cursor to print
     */
    public static void dumpCursor(ICursor cursor) {
        dumpCursor(cursor, 20);
    }

    /**
     * Dump the contents of the cursor to the system log, formatted in a readable way
     *
     * @param cursor the cursor to dump
     * @param maxColumnWidth maximum width for each column
     */
    public static void dumpCursor(ICursor cursor, int maxColumnWidth) {
        StringBuilder builder = new StringBuilder("\n");
        dumpCursor(cursor, maxColumnWidth, builder);
        Logger.d(Logger.LOG_TAG, builder.toString());
    }

    /**
     * Dump the contents of the cursor to the provided builder, formatted in a readable way
     *
     * @param cursor the cursor to dump
     * @param builder the builder to append to
     */
    public static void dumpCursor(ICursor cursor, StringBuilder builder) {
        dumpCursor(cursor, 20, builder);
    }

    /**
     * Dump the contents of the cursor to the provided builder, formatted in a readable way
     *
     * @param cursor the cursor to dump
     * @param maxColumnWidth maximum width for each column
     * @param builder the builder to append to
     */
    public static void dumpCursor(ICursor cursor, int maxColumnWidth, StringBuilder builder) {
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
    public static void dumpCurrentRow(ICursor cursor) {
        dumpCurrentRow(cursor, 20);
    }

    /**
     * Dump the contents of the current row to the system log
     *
     * @param cursor the cursor, with its position already moved to the desired row
     * @param maxColumnWidth maximum width for each column
     */
    public static void dumpCurrentRow(ICursor cursor, int maxColumnWidth) {
        StringBuilder builder = new StringBuilder("\n");
        dumpCurrentRow(cursor, maxColumnWidth, builder);
        Logger.d(Logger.LOG_TAG, builder.toString());
    }

    /**
     * Dump the contents of the current row to the provided builder
     *
     * @param cursor the cursor, with its position already moved to the desired row
     * @param builder the builder to append to
     */
    public static void dumpCurrentRow(ICursor cursor, StringBuilder builder) {
        dumpCurrentRow(cursor, 20, builder);
    }

    /**
     * Dump the contents of the current row to the provided builder
     *
     * @param cursor the cursor, with its position already moved to the desired row
     * @param maxColumnWidth maximum width for each column
     * @param builder the builder to append to
     */
    public static void dumpCurrentRow(ICursor cursor, int maxColumnWidth, StringBuilder builder) {
        for (int i = 0, count = cursor.getColumnCount(); i < count; i++) {
            addColumnToRowBuilder(builder, cursor.getString(i), maxColumnWidth);
        }
    }

    private static void addColumnToRowBuilder(StringBuilder builder, String value, int maxColumnWidth) {
        if (value == null) {
            value = "null";
        }
        if (maxColumnWidth <= 0) { // This won't be as well formatted, but it's good for things like EXPLAIN QUERY PLAN
            builder.append(value);
        } else if (value.length() > maxColumnWidth) {
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
     * A wrapper around {@link Collections#addAll(Collection, Object[])} that performs a null check on the objects
     * array before attempting the add
     */
    public static <T> void addAll(Collection<T> collection, T... objects) {
        if (objects != null) {
            Collections.addAll(collection, objects);
        }
    }

    // --- serialization

    /**
     * Copy database files to the given folder. Useful for debugging.
     *
     * @param database the SquidDatabase to copy
     * @param toFolder the directory to copy files into
     */
    public static void copyDatabase(SquidDatabase database, String toFolder) {
        File folderFile = new File(toFolder);
        if (!(folderFile.mkdirs() || folderFile.isDirectory())) {
            Logger.e(Logger.LOG_TAG, "Error creating directories for database copy");
            return;
        }
        File dbFile = new File(database.getDatabasePath());
        try {
            copyFile(dbFile, new File(folderFile.getAbsolutePath() + File.separator + database.getName()));
        } catch (Exception e) {
            Logger.e(Logger.LOG_TAG, "Error copying database " + database.getName(), e);
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
