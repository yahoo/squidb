/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.json;

import com.yahoo.squidb.sql.Field;
import com.yahoo.squidb.sql.Function;
import com.yahoo.squidb.sql.Property.StringProperty;
import com.yahoo.squidb.sql.SqlTable;
import com.yahoo.squidb.sql.TableModelName;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class JSONProperty<T> extends StringProperty {

    public JSONProperty(@Nonnull TableModelName tableModelName, @Nonnull String name) {
        super(tableModelName, name);
    }

    public JSONProperty(@Nonnull TableModelName tableModelName, @Nonnull String name,
            @Nullable String columnDefinition) {
        super(tableModelName, name, columnDefinition);
    }

    public JSONProperty(@Nonnull TableModelName tableModelName, @Nonnull String name, @Nullable String alias,
            @Nullable String columnDefinition) {
        super(tableModelName, name, alias, columnDefinition);
    }

    public JSONProperty(@Nonnull Function<String> function, @Nonnull String alias) {
        super(function, alias);
    }

    /**
     * Construct a JSONProperty from a {@link Function} and with the given alias, e.g.
     * "UPPER(column) AS uppercase"
     *
     * @param function the function
     * @param selectAs the alias to use. May be null.
     */
    @Nonnull
    public static <T> JSONProperty<T> fromJSONFunction(@Nonnull Function<String> function, @Nonnull String selectAs) {
        return new JSONProperty<>(function, selectAs);
    }

    /**
     * Construct a JSONProperty from a JSON string and with the given alias, e.g. "'hello' AS greeting". This is a
     * convenience method equivalent to <code>fromJSONFunction(JSONFunctions.json(jsonString), selectAs)</code>
     *
     * @param jsonString the JSON string to use
     * @param selectAs the alias to use. May be null.
     */
    @Nonnull
    public static <T> JSONProperty<T> fromJSONString(@Nullable String jsonString, @Nonnull String selectAs) {
        return fromJSONFunction(JSONFunctions.json(jsonString == null ? Field.NULL : jsonString), selectAs);
    }

    @Override
    @SuppressWarnings("unchecked")
    @Nonnull
    public JSONProperty<T> as(@Nonnull String newAlias) {
        return (JSONProperty<T>) super.as(newAlias);
    }

    @Override
    @SuppressWarnings("unchecked")
    @Nonnull
    public JSONProperty<T> as(@Nonnull String tableAlias, @Nonnull String columnAlias) {
        return (JSONProperty<T>) super.as(tableAlias, columnAlias);
    }

    @Override
    @SuppressWarnings("unchecked")
    @Nonnull
    public JSONProperty<T> as(@Nonnull SqlTable<?> newTable, @Nonnull String columnAlias) {
        return (JSONProperty<T>) super.as(newTable, columnAlias);
    }

    @Override
    @SuppressWarnings("unchecked")
    @Nonnull
    public JSONProperty<T> asSelectionFromTable(@Nonnull SqlTable<?> newTable, @Nullable String columnAlias) {
        return (JSONProperty<T>) super.asSelectionFromTable(newTable, columnAlias);
    }
}
