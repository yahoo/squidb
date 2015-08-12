/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.sql;

import com.yahoo.squidb.data.ViewModel;
import com.yahoo.squidb.utility.VersionCode;

/**
 * A SQLite View. Views are read-only tables composed from a pre-defined SELECT statement.
 */
public class View extends QueryTable {

    private boolean temporary;

    private View(Class<? extends ViewModel> modelClass, Property<?>[] properties, String expression, Query query,
            boolean temporary) {
        super(modelClass, properties, expression, query);
        this.temporary = temporary;
    }

    /**
     * Construct a View from the given query and with the given name
     *
     * @param query the {@link Query} that composes this View
     * @param name the name for this View
     * @return a new View instance
     */
    public static View fromQuery(Query query, String name) {
        return fromQuery(query, name, null, null);
    }

    /**
     * Construct a View from the given query with the given name and represented by the given model class
     *
     * @param query the {@link Query} that composes this View
     * @param name the name for this View
     * @param modelClass the model class representing this View
     * @return a new View instance
     */
    public static View fromQuery(Query query, String name, Class<? extends ViewModel> modelClass,
            Property<?>[] properties) {
        return new View(modelClass, properties, name, query, false);
    }

    /**
     * Construct a temporary View from the given query and with the given name
     *
     * @param query the {@link Query} that composes this View
     * @param name the name for this View
     * @return a new View instance
     */
    public static View temporaryFromQuery(Query query, String name) {
        return temporaryFromQuery(query, name, null, null);
    }

    /**
     * Construct a temporary View from the given query with the given name and represented by the given model class
     *
     * @param query the {@link Query} that composes this View
     * @param name the name for this View
     * @param modelClass the model class representing this View
     * @return a new View instance
     */
    public static View temporaryFromQuery(Query query, String name, Class<? extends ViewModel> modelClass,
            Property<?>[] properties) {
        return new View(modelClass, properties, name, query, true);
    }

    /**
     * Append the SQL statement that creates this View to the given {@link StringBuilder}
     */
    public void createViewSql(VersionCode sqliteVersion, StringBuilder sql) {
        sql.append("CREATE ");
        if (temporary) {
            sql.append("TEMPORARY ");
        }
        sql.append("VIEW IF NOT EXISTS ")
                .append(getExpression()).append(" AS ")
                .append(query.toRawSql(sqliteVersion));
    }
}
