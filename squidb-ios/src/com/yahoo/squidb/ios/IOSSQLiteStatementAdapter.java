/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.ios;

import com.yahoo.android.sqlite.SQLiteStatement;
import com.yahoo.squidb.data.ISQLitePreparedStatement;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Wrapper for the iOS port of SQLiteStatement that implements the common {@link ISQLitePreparedStatement} interface.
 */
public class IOSSQLiteStatementAdapter implements ISQLitePreparedStatement {

    private final SQLiteStatement statement;

    IOSSQLiteStatementAdapter(@Nonnull SQLiteStatement statement) {
        this.statement = statement;
    }

    @Override
    public void close() {
        statement.close();
    }

    @Override
    public void bindNull(int index) {
        statement.bindNull(index);
    }

    @Override
    public void bindLong(int index, long value) {
        statement.bindLong(index, value);
    }

    @Override
    public void bindDouble(int index, double value) {
        statement.bindDouble(index, value);
    }

    @Override
    public void bindString(int index, @Nonnull String value) {
        statement.bindString(index, value);
    }

    @Override
    public void bindBlob(int index, @Nonnull byte[] value) {
        statement.bindBlob(index, value);
    }

    @Override
    public void clearBindings() {
        statement.clearBindings();
    }

    @Override
    public void execute() {
        statement.execute();
    }

    @Override
    public int executeUpdateDelete() {
        return statement.executeUpdateDelete();
    }

    @Override
    public long executeInsert() {
        return statement.executeInsert();
    }

    @Override
    public long simpleQueryForLong() {
        return statement.simpleQueryForLong();
    }

    @Override
    @Nullable
    public String simpleQueryForString() {
        return statement.simpleQueryForString();
    }
}
