/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.sql;

import com.yahoo.squidb.data.AbstractModel;
import com.yahoo.squidb.utility.SquidUtilities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A database object from which a select operation can be performed, such as a {@link Table} or {@link View}
 */
public abstract class SqlTable<T extends AbstractModel> extends DBObject<SqlTable<T>> {

    protected final Class<? extends T> modelClass;

    @Nonnull
    protected final List<Property<?>> properties;

    /**
     * @param expression the string-literal representation of this SqlTable
     */
    protected SqlTable(@Nullable Class<? extends T> modelClass, @Nonnull List<Property<?>> properties,
            @Nonnull String expression) {
        super(expression);
        this.modelClass = modelClass;
        this.properties = properties;
    }

    /**
     * @param expression the string-literal representation of this SqlTable
     * @param qualifier the string-literal representation of a qualifying object, e.g. a database name
     */
    protected SqlTable(@Nullable Class<? extends T> modelClass, @Nonnull List<Property<?>> properties,
            @Nonnull String expression, @Nullable String qualifier) {
        super(expression, qualifier);
        this.modelClass = modelClass;
        this.properties = properties;
    }

    /**
     * @return the model class represented by this table
     */
    @Nullable
    public Class<? extends T> getModelClass() {
        return modelClass;
    }

    /**
     * @return the properties array corresponding to this data source/model. May return an empty list if this table is
     * not associated with a particular model class, e.g. if it is a {@link SubqueryTable} not backed by a model.
     * To get a list of fields from this data source in the case when it is not associated with a model class, use
     * {@link #allFields()}
     */
    @Nonnull
    public List<Property<?>> getProperties() {
        return properties;
    }

    /**
     * Clone the given {@link Field fields} with this object's name as their qualifier. This is useful for selecting
     * from views, subqueries, or aliased tables.
     *
     * @param fields the fields to clone
     * @return the given fields cloned and with this object as their qualifier
     */
    @Nonnull
    public List<Field<?>> qualifyFields(@Nonnull Field<?>... fields) {
        return qualifyFields(SquidUtilities.asList(fields));
    }

    /**
     * Clone the given {@link Field fields} with this object's name as their qualifier. This is useful for selecting
     * from views, subqueries, or aliased tables.
     *
     * @param fields the fields to clone
     * @return the given fields cloned and with this object as their qualifier
     */
    @Nonnull
    public List<Field<?>> qualifyFields(@Nonnull List<? extends Field<?>> fields) {
        if (fields.isEmpty()) {
            return Collections.emptyList();
        }
        List<Field<?>> result = new ArrayList<>(fields.size());
        for (Field<?> field : fields) {
            result.add(qualifyField(field));
        }

        return result;
    }

    /**
     * Clone the given {@link Property properties} with this object's name as their qualifier. This is useful for
     * selecting from views, subqueries, or aliased tables. This method is distinct from the more general
     * {@link #qualifyFields(Field[])} because it is guaranteed to return a list of the same property type that it is
     * called with.
     *
     * @param properties the properties to clone
     * @return the given properties cloned and with this object as their qualifier
     */
    @Nonnull
    public <P extends Property<?>> List<P> qualifyProperties(@Nonnull P... properties) {
        return qualifyProperties(SquidUtilities.asList(properties));
    }

    /**
     * Clone the given {@link Property properties} with this object's name as their qualifier. This is useful for
     * selecting from views, subqueries, or aliased tables. This method is distinct from the more general
     * {@link #qualifyFields(List)} because it is guaranteed to return a list of the same property type that it is
     * called with.
     *
     * @param properties the properties to clone
     * @return the given properties cloned and with this object as their qualifier
     */
    @Nonnull
    public <P extends Property<?>> List<P> qualifyProperties(@Nonnull List<P> properties) {
        if (properties.isEmpty()) {
            return Collections.emptyList();
        }
        List<P> result = new ArrayList<>(properties.size());
        for (P property : properties) {
            result.add(qualifyProperty(property));
        }

        return result;
    }

    /**
     * Clone the given {@link Field} with this object's name as its qualifier. This is useful for selecting
     * from views, subqueries, or aliased tables.
     *
     * @param field the field to clone
     * @return a clone of the given field with this object as its qualifier
     */
    @Nonnull
    public Field<?> qualifyField(@Nonnull Field<?> field) {
        if (field instanceof Property<?>) {
            return qualifyProperty((Property<?>) field);
        } else {
            return Field.field(field.getName(), getName());
        }
    }

    /**
     * Clone the given {@link Property} with this object's name as its qualifier. This is useful for selecting
     * from views, subqueries, or aliased tables. This method is distinct from the more general
     * {@link #qualifyField(Field)} because it is guaranteed to return the same property type that it is called with.
     *
     * @param property the property to clone
     * @return a clone of the given property with this object as its qualifier
     */
    @SuppressWarnings("unchecked")
    @Nonnull
    public <P extends Property<?>> P qualifyProperty(@Nonnull P property) {
        return (P) property.asSelectionFromTable(this, null);
    }

    /**
     * @return the fields associated to this data source
     */
    @Nonnull
    protected List<? extends Field<?>> allFields() {
        return properties;
    }

    @Override
    @Nonnull
    public SqlTable<T> as(@Nonnull String newAlias) {
        List<Property<?>> newProperties = new ArrayList<>(properties.size());
        SqlTable<T> result = asNewAliasWithProperties(newAlias, Collections.unmodifiableList(newProperties));
        for (Property<?> p : properties) {
            newProperties.add(result.qualifyProperty(p));
        }
        return result;
    }

    protected abstract SqlTable<T> asNewAliasWithProperties(@Nonnull String newAlias,
            @Nonnull List<Property<?>> newProperties);

}
