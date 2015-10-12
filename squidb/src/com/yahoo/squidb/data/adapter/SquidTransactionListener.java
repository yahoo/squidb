/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.data.adapter;

/**
 * A re-declaration of {@link android.database.sqlite.SQLiteTransactionListener} so that all wrapper/adapter classes
 * for different versions of {@link android.database.sqlite.SQLiteDatabase SQLiteDatabase} can provide a unified
 * interface for transaction methods.
 */
public interface SquidTransactionListener {

    /**
     * @see android.database.sqlite.SQLiteTransactionListener#onBegin()
     */
    void onBegin();

    /**
     * @see android.database.sqlite.SQLiteTransactionListener#onCommit()
     */
    void onCommit();

    /**
     * @see android.database.sqlite.SQLiteTransactionListener#onRollback()
     */
    void onRollback();

}
