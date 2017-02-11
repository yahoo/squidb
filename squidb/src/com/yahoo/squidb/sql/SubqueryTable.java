/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.sql;

import com.yahoo.squidb.data.ViewModel;

import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A table represented by a subquery
 */
public class SubqueryTable extends QueryTable {

    private SubqueryTable(@Nullable Class<? extends ViewModel> modelClass, @Nonnull List<Property<?>> properties,
            @Nonnull String name, @Nonnull Query query) {
        super(modelClass, properties, name, null, query);
    }

    /**
     * Construct a new SubqueryTable backed by the given Query and aliased to the given name
     *
     * @param query the backing Query
     * @param name the name for the table
     * @return a new SubqueryTable
     */
    @Nonnull
    public static SubqueryTable fromQuery(@Nonnull Query query, @Nonnull String name) {
        return new SubqueryTable(null, Collections.<Property<?>>emptyList(), name, query);
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
    @Nonnull
    public static SubqueryTable fromQuery(@Nonnull Query query, @Nonnull String name,
            @Nonnull Class<? extends ViewModel> modelClass, @Nonnull List<Property<?>> properties) {
        return new SubqueryTable(modelClass, properties, name, query);
    }

    @Override
    @Nonnull
    public SubqueryTable as(@Nonnull String newAlias) {
        return (SubqueryTable) super.as(newAlias);
    }

    @Override
    @Nonnull
    protected SubqueryTable asNewAliasWithProperties(@Nonnull String newAlias, @Nonnull List<Property<?>> newProperties) {
        return new SubqueryTable(modelClass, newProperties, newAlias, query);
    }

    @Override
    void appendToSqlBuilder(@Nonnull SqlBuilder builder, boolean forSqlValidation) {
        builder.sql.append("(");
        query.appendToSqlBuilder(builder, forSqlValidation);
        builder.sql.append(") AS ").append(getName());
    }
}
