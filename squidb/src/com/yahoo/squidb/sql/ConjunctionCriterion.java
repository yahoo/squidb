/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.sql;

import com.yahoo.squidb.utility.SquidUtilities;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

class ConjunctionCriterion extends Criterion {

    private final List<Criterion> criterions = new ArrayList<>();

    ConjunctionCriterion(@Nonnull Operator operator, @Nonnull Criterion baseCriterion,
            @Nonnull Criterion... additionalCriterions) {
        super(operator);
        if (baseCriterion == null) {
            throw new IllegalArgumentException("First Criterion of a ConjunctionCriterion must not be null");
        }
        this.criterions.add(baseCriterion);
        SquidUtilities.addAll(this.criterions, additionalCriterions);
    }

    ConjunctionCriterion(@Nonnull Operator operator, @Nonnull List<Criterion> criterions) {
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
    protected void populate(@Nonnull SqlBuilder builder, boolean forSqlValidation) {
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
    @Nonnull
    public Criterion and(@Nullable Criterion criterion) {
        Criterion toReturn = checkOperatorAndAppendCriterions(Operator.and, criterion);
        if (toReturn == null) {
            return super.and(criterion);
        }
        return toReturn;
    }

    @Override
    @Nonnull
    public Criterion or(@Nullable Criterion criterion) {
        Criterion toReturn = checkOperatorAndAppendCriterions(Operator.or, criterion);
        if (toReturn == null) {
            return super.or(criterion);
        }
        return toReturn;
    }

    @Nullable
    private Criterion checkOperatorAndAppendCriterions(@Nonnull Operator toCheck, @Nullable Criterion criterion) {
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
