package com.yahoo.squidb.data;

import com.yahoo.squidb.sql.CompiledStatement;
import com.yahoo.squidb.sql.Insert;
import com.yahoo.squidb.sql.Table;
import com.yahoo.squidb.sql.TableStatement;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

class PreparedInsertCache {

    private final Map<Class<? extends TableModel>, ThreadLocal<ISQLitePreparedStatement>[]>
            preparedStatementCache = new HashMap<>();

    @SuppressWarnings("unchecked")
    synchronized ISQLitePreparedStatement getPreparedInsert(SquidDatabase db, Table table,
            TableStatement.ConflictAlgorithm conflictAlgorithm) {

        Class<? extends TableModel> modelClass = table.getModelClass();
        ThreadLocal<ISQLitePreparedStatement>[] preparedStatements = preparedStatementCache.get(modelClass);

        if (preparedStatements == null) {
            preparedStatements = (ThreadLocal<ISQLitePreparedStatement>[])
                    new ThreadLocal[TableStatement.ConflictAlgorithm.values().length];
            preparedStatementCache.put(modelClass, preparedStatements);
        }

        if (conflictAlgorithm == null) {
            conflictAlgorithm = TableStatement.ConflictAlgorithm.NONE;
        }

        ThreadLocal<ISQLitePreparedStatement> threadLocalStatement = preparedStatements[conflictAlgorithm.ordinal()];
        if (threadLocalStatement == null) {
            threadLocalStatement = new ThreadLocal<>();
            preparedStatements[conflictAlgorithm.ordinal()] = threadLocalStatement;
        }

        ISQLitePreparedStatement toReturn = threadLocalStatement.get();
        if (toReturn == null) {
            toReturn = prepareInsert(db, table, conflictAlgorithm);
            threadLocalStatement.set(toReturn);
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

    synchronized void invalidateAll() {
        // TODO: This relies on GC to close the prepared statements. Can we close them more safely?
        preparedStatementCache.clear();
    }

}
