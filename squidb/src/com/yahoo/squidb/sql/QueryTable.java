/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.sql;

import com.yahoo.squidb.data.ViewModel;

/**
 * Table backed by a query. Examples include views or subqueries (e.g. SELECT FROM (SELECT...)).
 */
abstract class QueryTable extends SqlTable<ViewModel> {

    final Query query;

    protected QueryTable(Class<? extends ViewModel> modelClass, Property<?>[] properties, String name,
            String databaseName, Query query) {
        super(modelClass, properties, name, databaseName);
        this.query = query;
    }

    /**
     * @return the selected {@link Field}s of the underlying query, qualified by this table's name
     */
    public Field<?>[] qualifiedFields() {
        return qualifyFields(query.getFields());
    }

    @Override
    protected Field<?>[] allFields() {
        if (properties != null) {
            return properties;
        } else {
            return qualifiedFields();
        }
    }
}
