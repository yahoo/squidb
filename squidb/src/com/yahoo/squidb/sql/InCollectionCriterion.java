/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.sql;

import java.util.Collection;

class InCollectionCriterion extends BinaryCriterion {

    private final Collection<?> collection;

    InCollectionCriterion(Field<?> expression, Operator operator, Collection<?> value) {
        super(expression, operator, value);
        this.collection = value;
    }

    @Override
    protected void afterPopulateOperator(SqlBuilder builder, boolean forSqlValidation) {
        builder.sql.append("(");
        builder.addCollectionArg(collection);
        builder.sql.append(")");
    }

    @Override
    protected BinaryCriterion constructNegatedCriterion(Operator negatedOperator) {
        return new InCollectionCriterion(field, negatedOperator, collection);
    }
}
