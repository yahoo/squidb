/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.sql;

import com.yahoo.squidb.data.ViewModel;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Table backed by a query. Examples include views or subqueries (e.g. SELECT FROM (SELECT...)).
 */
abstract class QueryTable extends SqlTable<ViewModel> {

    final Query query;

    protected QueryTable(@Nullable Class<? extends ViewModel> modelClass, @Nonnull List<Property<?>> properties,
            @Nonnull String name, @Nullable String databaseName, @Nonnull Query query) {
        super(modelClass, properties, name, databaseName);
        this.query = query;
    }

    /**
     * @return the selected {@link Field}s of the underlying query, qualified by this table's name
     */
    @Nonnull
    public List<? extends Field<?>> qualifiedFields() {
        return qualifyFields(query.getFields());
    }

    @Override
    @Nonnull
    protected List<? extends Field<?>> allFields() {
        if (!properties.isEmpty()) {
            return properties;
        } else {
            return qualifiedFields();
        }
    }
}
