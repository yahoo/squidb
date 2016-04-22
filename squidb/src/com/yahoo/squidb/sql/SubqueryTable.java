/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.sql;

import com.yahoo.squidb.data.ViewModel;

/**
 * A table represented by a subquery
 */
public class SubqueryTable extends QueryTable {

    private SubqueryTable(Class<? extends ViewModel> modelClass, Property<?>[] properties, String name, Query query) {
        super(modelClass, properties, name, null, query);
    }

    /**
     * Construct a new SubqueryTable backed by the given Query and aliased to the given name
     *
     * @param query the backing Query
     * @param name the name for the table
     * @return a new SubqueryTable
     */
    public static SubqueryTable fromQuery(Query query, String name) {
        return new SubqueryTable(null, null, name, query);
    }

    /**
     * Construct a new SubqueryTable backed by the given Query and aliased to the given name and associate it with the
     * given ViewModel
     *
     * @param query the backing Query
     * @param name the name for the table
     * @param modelClass the ViewModel to associate
     * @return a new SubqueryTable
     */
    public static SubqueryTable fromQuery(Query query, String name, Class<? extends ViewModel> modelClass,
            Property<?>[] properties) {
        return new SubqueryTable(modelClass, properties, name, query);
    }

    @Override
    public SubqueryTable as(String newAlias) {
        return (SubqueryTable) super.as(newAlias);
    }

    @Override
    protected SubqueryTable asNewAliasWithPropertiesArray(String newAlias, Property<?>[] newProperties) {
        return new SubqueryTable(modelClass, newProperties, newAlias, query);
    }

    @Override
    void appendToSqlBuilder(SqlBuilder builder, boolean forSqlValidation) {
        builder.sql.append("(");
        query.appendToSqlBuilder(builder, forSqlValidation);
        builder.sql.append(") AS ").append(getName());
    }
}
