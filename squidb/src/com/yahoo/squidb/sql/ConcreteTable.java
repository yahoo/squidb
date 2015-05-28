/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.sql;

import com.yahoo.squidb.data.TableModel;

public abstract class ConcreteTable extends SqlTable<TableModel> {

    protected ConcreteTable(Class<? extends TableModel> modelClass, Property<?>[] properties, String expression) {
        super(modelClass, properties, expression);
    }

    protected ConcreteTable(Class<? extends TableModel> modelClass, Property<?>[] properties, String expression,
            String qualifier) {
        super(modelClass, properties, expression, qualifier);
    }

    public abstract void appendCreateTableSql(StringBuilder sql,
            Property.PropertyVisitor<Void, StringBuilder> propertyVisitor);

    public abstract ConcreteTable qualifiedFromDatabase(String databaseName);
}
