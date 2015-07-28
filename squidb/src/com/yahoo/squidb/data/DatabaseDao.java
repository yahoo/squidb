/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.data;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteTransactionListener;
import android.net.Uri;

import com.yahoo.squidb.data.UriNotifier.DBOperation;
import com.yahoo.squidb.sql.CompiledStatement;
import com.yahoo.squidb.sql.Criterion;
import com.yahoo.squidb.sql.Delete;
import com.yahoo.squidb.sql.Insert;
import com.yahoo.squidb.sql.Property;
import com.yahoo.squidb.sql.Property.IntegerProperty;
import com.yahoo.squidb.sql.Query;
import com.yahoo.squidb.sql.SqlTable;
import com.yahoo.squidb.sql.Table;
import com.yahoo.squidb.sql.TableStatement.ConflictAlgorithm;
import com.yahoo.squidb.sql.Update;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Class for reading data from an instance of {@link AbstractDatabase}.

 */
public class DatabaseDao {

    private final AbstractDatabase database;

    public DatabaseDao(AbstractDatabase database) {
        this.database = database;
    }

    // --- dao methods

    protected String getDatabaseName() {
        return database.getName();
    }


}
