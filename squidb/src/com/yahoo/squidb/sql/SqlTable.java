/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.sql;

import com.yahoo.squidb.data.AbstractModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * A database object from which a select operation can be performed, such as a {@link Table} or {@link View}
 */
public abstract class SqlTable<T extends AbstractModel> extends DBObject<SqlTable<T>> {

    protected final Class<? extends T> modelClass;
    protected final List<Property<?>> properties;

    /**
     * @param expression the string-literal representation of this SqlTable
     */
    protected SqlTable(Class<? extends T> modelClass, List<Property<?>> properties, String expression) {
        super(expression);
        this.modelClass = modelClass;
        this.properties = properties;
    }

    /**
     * @param expression the string-literal representation of this SqlTable
     * @param qualifier the string-literal representation of a qualifying object, e.g. a database name
     */
    protected SqlTable(Class<? extends T> modelClass, List<Property<?>> properties, String expression, String qualifier) {
        super(expression, qualifier);
        this.modelClass = modelClass;
        this.properties = properties;
    }

    /**
     * @return the model class represented by this table
     */
    public Class<? extends T> getModelClass() {
        return modelClass;
    }

    /**
     * @return the properties array corresponding to this table
     */
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
    public List<Field<?>> qualifyFields(Field<?>... fields) {
        if (fields == null) {
            return null;
        }

        return qualifyFields(Arrays.asList(fields));
    }

    /**
     * Clone the given {@link Field fields} with this object's name as their qualifier. This is useful for selecting
     * from views, subqueries, or aliased tables.
     *
     * @param fields the fields to clone
     * @return the given fields cloned and with this object as their qualifier
     */
    public List<Field<?>> qualifyFields(List<? extends Field<?>> fields) {
        if (fields == null) {
            return null;
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
    public <P extends Property<?>> List<P> qualifyProperties(P... properties) {
        if (properties == null) {
            return null;
        }
        return qualifyProperties(Arrays.asList(properties));
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
    public <P extends Property<?>> List<P> qualifyProperties(List<P> properties) {
        if (properties == null) {
            return null;
        }
        List<P> result = new ArrayList<>(properties.size());
        for (P property : properties) {
            result.add(qualifyField(property));
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
    public Field<?> qualifyField(Field<?> field) {
        if (field instanceof Property<?>) {
            return qualifyField((Property<?>) field);
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
    public <P extends Property<?>> P qualifyField(P property) {
        return (P) property.asSelectionFromTable(this, null);
    }

    /**
     * @return the fields associated to this data source
     */
    protected List<? extends Field<?>> allFields() {
        if (properties == null) {
            return Collections.emptyList();
        }
        return properties;
    }

    @Override
    public SqlTable<T> as(String newAlias) {
        List<Property<?>> newProperties = properties == null ? null : new ArrayList<Property<?>>(properties.size());
        if (newProperties == null) {
            return asNewAliasWithProperties(newAlias, null);
        }
        SqlTable<T> result = asNewAliasWithProperties(newAlias, Collections.unmodifiableList(newProperties));
        for (Property<?> p : properties) {
            newProperties.add(result.qualifyField(p));
        }
        return result;
    }

    protected abstract SqlTable<T> asNewAliasWithProperties(String newAlias, List<Property<?>> newProperties);

}
