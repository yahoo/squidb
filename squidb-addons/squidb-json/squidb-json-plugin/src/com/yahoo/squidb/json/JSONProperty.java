/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.json;

import com.yahoo.squidb.sql.Function;
import com.yahoo.squidb.sql.Property.StringProperty;
import com.yahoo.squidb.sql.SqlTable;
import com.yahoo.squidb.sql.TableModelName;

public class JSONProperty<T> extends StringProperty {

    public JSONProperty(TableModelName tableModelName, String name) {
        super(tableModelName, name);
    }

    public JSONProperty(TableModelName tableModelName, String name, String columnDefinition) {
        super(tableModelName, name, columnDefinition);
    }

    public JSONProperty(TableModelName tableModelName, String name, String alias, String columnDefinition) {
        super(tableModelName, name, alias, columnDefinition);
    }

    public JSONProperty(Function<String> function, String alias) {
        super(function, alias);
    }

    /**
     * Construct a JSONProperty from a {@link Function} and with the given alias, e.g.
     * "UPPER(column) AS uppercase"
     *
     * @param function the function
     * @param selectAs the alias to use. May be null.
     */
    public static <T> JSONProperty<T> fromJSONFunction(Function<String> function, String selectAs) {
        return new JSONProperty<>(function, selectAs);
    }

    /**
     * Construct a JSONProperty from a JSON string and with the given alias, e.g. "'hello' AS greeting". This is a
     * convenience method equivalent to <code>fromJSONFunction(JSONFunctions.json(jsonString), selectAs)</code>
     *
     * @param jsonString the JSON string to use
     * @param selectAs the alias to use. May be null.
     */
    public static <T> JSONProperty<T> fromJSONString(String jsonString, String selectAs) {
        return fromJSONFunction(JSONFunctions.json(jsonString), selectAs);
    }

    @Override
    @SuppressWarnings("unchecked")
    public JSONProperty<T> as(String newAlias) {
        return (JSONProperty<T>) super.as(newAlias);
    }

    @Override
    @SuppressWarnings("unchecked")
    public JSONProperty<T> as(String tableAlias, String columnAlias) {
        return (JSONProperty<T>) super.as(tableAlias, columnAlias);
    }

    @Override
    @SuppressWarnings("unchecked")
    public JSONProperty<T> as(SqlTable<?> newTable, String columnAlias) {
        return (JSONProperty<T>) super.as(newTable, columnAlias);
    }

    @Override
    @SuppressWarnings("unchecked")
    public JSONProperty<T> asSelectionFromTable(SqlTable<?> newTable, String columnAlias) {
        return (JSONProperty<T>) super.asSelectionFromTable(newTable, columnAlias);
    }
}
