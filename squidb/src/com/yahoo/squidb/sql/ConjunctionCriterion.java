/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.sql;

import java.util.List;

class ConjunctionCriterion extends Criterion {

    private final Criterion baseCriterion;
    private final Criterion[] additionalCriterions;

    ConjunctionCriterion(Operator operator, Criterion criterion, Criterion... additionalCriterions) {
        super(operator);
        this.baseCriterion = criterion;
        this.additionalCriterions = additionalCriterions;
    }

    // For internal cloning
    private ConjunctionCriterion(Criterion baseCriterion, Criterion[] additionalCriterions, Criterion appendToEnd,
            Operator operator) {
        super(operator);
        this.baseCriterion = baseCriterion;
        int newLength = additionalCriterions.length + 1;
        this.additionalCriterions = new Criterion[newLength];
        System.arraycopy(additionalCriterions, 0, this.additionalCriterions, 0, additionalCriterions.length);
        this.additionalCriterions[newLength - 1] = appendToEnd;
    }

    @Override
    protected void populate(StringBuilder sql, List<Object> selectionArgsBuilder) {
        baseCriterion.appendCompiledStringWithArguments(sql, selectionArgsBuilder);
        for (Criterion c : additionalCriterions) {
            sql.append(operator);
            c.appendCompiledStringWithArguments(sql, selectionArgsBuilder);
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
            return new ConjunctionCriterion(baseCriterion, additionalCriterions, criterion, operator);
        }
        return null;
    }
}
