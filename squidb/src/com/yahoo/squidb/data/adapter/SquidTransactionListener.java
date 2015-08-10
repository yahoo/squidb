/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.data.adapter;

public interface SquidTransactionListener {

    void onBegin();

    void onCommit();

    void onRollback();

}
