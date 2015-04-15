/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.sql;

import java.util.HashMap;
import java.util.Map;

/**
 * SQLite operators that can be used in WHERE clauses
 */
public enum Operator {
    /** EQUALS ('=') */
    eq("="),
    /** NOT EQUALS ('<>') */
    neq("<>"),
    /** IS */
    is(" IS "),
    /** IS NOT */
    isNot(" IS NOT "),
    /** GREATER THAN ('>') */
    gt(">"),
    /** LESS THAN ('<') */
    lt("<"),
    /** GREATER THAN OR EQUAL ('>=') */
    gte(">="),
    /** LESS THAN OR EQUAL ('<=') */
    lte("<="),
    /** AND */
    and(" AND "),
    /** OR */
    or(" OR "),
    /** NOT */
    not(" NOT "),
    /** EXISTS */
    exists(" EXISTS "),
    /** LIKE */
    like(" LIKE "),
    /** IN */
    in(" IN "),
    /** BETWEEN */
    between(" BETWEEN "),
    /** GLOB */
    glob(" GLOB "),
    /** MATCH */
    match(" MATCH ");

    private static final Map<Operator, Operator> contraryRegistry = new HashMap<Operator, Operator>();

    static {
        contraryRegistry.put(eq, neq);
        contraryRegistry.put(neq, eq);
        contraryRegistry.put(is, isNot);
        contraryRegistry.put(isNot, is);
        contraryRegistry.put(gt, lte);
        contraryRegistry.put(lte, gt);
        contraryRegistry.put(lt, gte);
        contraryRegistry.put(gte, lt);
    }

    private final String operator;

    private Operator(String operator) {
        this.operator = operator;
    }

    /**
     * @return the {@link Operator} representing the contrary to this operator, if one exists
     */
    public Operator getContrary() {
        return contraryRegistry.get(this);
    }

    @Override
    public String toString() {
        return this.operator;
    }
}
