/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.sql;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class ConjunctionCriterion extends Criterion {

    private final List<Criterion> criterions = new ArrayList<>();

    ConjunctionCriterion(Operator operator, Criterion baseCriterion, Criterion... additionalCriterions) {
        super(operator);
        if (baseCriterion == null) {
            throw new IllegalArgumentException("First Criterion of a ConjunctionCriterion must not be null");
        }
        this.criterions.add(baseCriterion);
        if (additionalCriterions != null) {
            Collections.addAll(this.criterions, additionalCriterions);
        }
    }

    ConjunctionCriterion(Operator operator, List<Criterion> criterions) {
        super(operator);
        if (criterions == null || criterions.isEmpty()) {
            throw new IllegalArgumentException("Must specify at least one criterion for a ConjunctionCriterion");
        }
        if (criterions.get(0) == null) {
            throw new NullPointerException("First Criterion of ConjunctionCriterion List must not be null");
        }
        this.criterions.addAll(criterions);
    }

    @Override
    protected void populate(SqlBuilder builder, boolean forSqlValidation) {
        criterions.get(0).appendToSqlBuilder(builder, forSqlValidation);
        for (int i = 1; i < criterions.size(); i++) {
            Criterion c = criterions.get(i);
            if (c != null) {
                builder.sql.append(operator);
                c.appendToSqlBuilder(builder, forSqlValidation);
            }
        }
    }

    @Override
    public Criterion and(Criterion criterion) {
        Criterion toReturn = checkOperatorAndAppendCriterions(Operator.and, criterion);
        if (toReturn == null) {
            return super.and(criterion);
        }
        return toReturn;
    }

    @Override
    public Criterion or(Criterion criterion) {
        Criterion toReturn = checkOperatorAndAppendCriterions(Operator.or, criterion);
        if (toReturn == null) {
            return super.or(criterion);
        }
        return toReturn;
    }

    private Criterion checkOperatorAndAppendCriterions(Operator toCheck, Criterion criterion) {
        if (criterion == null) {
            return this;
        }
        if (operator.equals(toCheck)) {
            ConjunctionCriterion newCriterion = new ConjunctionCriterion(operator, this.criterions);
            newCriterion.criterions.add(criterion);
            return newCriterion;
        }
        return null;
    }
}
