/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.utility;

import com.yahoo.squidb.sql.Field;
import com.yahoo.squidb.sql.SqlUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A mapping from column names to selectable {@link Field Fields}. This is useful for renaming columns passed as
 * projection arguments by a caller, as well as disambiguating column names when doing joins.
 */
public class ProjectionMap {

    private Map<String, Field<?>> map;

    /**
     * Construct an empty ProjectionMap
     */
    public ProjectionMap() {
        map = new LinkedHashMap<String, Field<?>>();
    }

    /**
     * Construct a ProjectionMap that is a copy of another.
     *
     * @param other the other ProjectionMap to copy
     */
    public ProjectionMap(ProjectionMap other) {
        map = new LinkedHashMap<String, Field<?>>(other.map);
    }

    /**
     * Add a {@link Field} to the map. If the name of the Field being added does not match the name, it will be aliased
     * to that name before being added to the map.
     *
     * @param name the key to use
     * @param column the Field to add
     * @return the value of any previous mapping with the specified key, or null if there was no mapping
     */
    public Field<?> put(String name, Field<?> column) {
        if (column == null) {
            throw new IllegalArgumentException("Cannot use null column in ProjectionMap");
        }
        if (SqlUtils.isEmpty(name)) {
            throw new IllegalArgumentException("Cannot use empty string as a key");
        }
        if (!SqlUtils.equals(name, column.getName())) {
            column = column.as(name);
        }
        return map.put(name, column);
    }

    /**
     * Add a {@link Field} to the map using its current name as the key
     *
     * @param column the Field to add
     * @return the value of any previous mapping with the specified key, or null if there was no mapping
     */
    public Field<?> put(Field<?> column) {
        if (column == null) {
            throw new IllegalArgumentException("Cannot use null column in ProjectionMap");
        }
        return map.put(column.getName(), column);
    }

    /**
     * Add multiple {@link Field Fields} to the map using their current names as keys
     *
     * @param columns the Fields to add
     */
    public void putAll(Field<?>... columns) {
        if (columns != null) {
            for (Field<?> field : columns) {
                put(field);
            }
        }
    }

    /**
     * Add a {@link Field} represented by the given expression to the map
     *
     * @param expression the expression to add
     * @return the value of any previous mapping with the specified key, or null if there was no mapping
     */
    public Field<?> put(String expression) {
        if (SqlUtils.isEmpty(expression)) {
            throw new IllegalArgumentException("Expression cannot be empty");
        }
        return map.put(expression, Field.field(expression));
    }

    /**
     * Get the {@link Field} mapped to the specified key
     *
     * @param key the key
     * @return the {@link Field} mapped to the given key, or null if no mapping exists for that key
     */
    public Field<?> get(String key) {
        return map.get(key);
    }

    /**
     * @return a list of {@link Field Fields} in the map
     */
    public List<Field<?>> getDefaultProjection() {
        return new ArrayList<Field<?>>(map.values());
    }

    /**
     * @return an array of keys (column names) in the map
     */
    public String[] getDefaultProjectionNames() {
        return map.keySet().toArray(new String[map.size()]);
    }
}
