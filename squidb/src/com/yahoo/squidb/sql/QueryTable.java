/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.sql;

import com.yahoo.squidb.data.ViewModel;
import com.yahoo.squidb.utility.SquidUtilities;

/**
 * Table backed by a query. Examples include views or subqueries (e.g. SELECT FROM (SELECT...)).
 */
abstract class QueryTable extends SqlTable<ViewModel> {

    final Query query;

    protected QueryTable(Class<? extends ViewModel> modelClass, String name, Query query) {
        super(modelClass, name);
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
        if (modelClass != null) {
            return SquidUtilities.getProperties(modelClass);
        } else {
            return qualifiedFields();
        }
    }
}
