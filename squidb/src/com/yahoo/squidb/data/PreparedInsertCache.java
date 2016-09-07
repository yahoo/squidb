/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.data;

import com.yahoo.squidb.sql.CompiledStatement;
import com.yahoo.squidb.sql.Insert;
import com.yahoo.squidb.sql.Table;
import com.yahoo.squidb.sql.TableStatement;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

// TODO: This relies on GC to close the prepared statements when the cache is destroyed. Can we close them more safely?
// This class is not threadsafe. We currently keep a threadlocal instance of it in SquidDatabase that is invalidated
// when the DB is closed.
class PreparedInsertCache {

    private final Map<Class<? extends TableModel>, ISQLitePreparedStatement[]>
            preparedStatementCache = new HashMap<>();

    ISQLitePreparedStatement getPreparedInsert(SquidDatabase db, Table table,
            TableStatement.ConflictAlgorithm conflictAlgorithm) {

        Class<? extends TableModel> modelClass = table.getModelClass();
        ISQLitePreparedStatement[] preparedStatements = preparedStatementCache.get(modelClass);

        if (preparedStatements == null) {
            preparedStatements = new ISQLitePreparedStatement[TableStatement.ConflictAlgorithm.values().length];
            preparedStatementCache.put(modelClass, preparedStatements);
        }

        if (conflictAlgorithm == null) {
            conflictAlgorithm = TableStatement.ConflictAlgorithm.NONE;
        }

        ISQLitePreparedStatement toReturn = preparedStatements[conflictAlgorithm.ordinal()];
        if (toReturn == null) {
            toReturn = prepareInsert(db, table, conflictAlgorithm);
            preparedStatements[conflictAlgorithm.ordinal()] = toReturn;
        }
        return toReturn;
    }

    private ISQLitePreparedStatement prepareInsert(SquidDatabase db, Table table,
            TableStatement.ConflictAlgorithm conflictAlgorithm) {
        Object[] placeholders = new Object[table.getProperties().length];
        Arrays.fill(placeholders, new Object());

        Insert insert = Insert.into(table).columns(table.getProperties())
                .values(placeholders).onConflict(conflictAlgorithm);
        CompiledStatement compiled = insert.compile(db.getSqliteVersion());

        return db.prepareStatement(compiled.sql);
    }
}
