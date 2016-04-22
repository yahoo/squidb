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
    /** NOT EQUALS ('&gt;&lt;') */
    neq("<>"),
    /** IS */
    is(" IS "),
    /** IS NOT */
    isNot(" IS NOT "),
    /** GREATER THAN ('&gt;') */
    gt(">"),
    /** LESS THAN ('&lt;') */
    lt("<"),
    /** GREATER THAN OR EQUAL ('&gt;=') */
    gte(">="),
    /** LESS THAN OR EQUAL ('&lt;=') */
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
    /** NOT LIKE */
    notLike(" NOT LIKE "),
    /** IN */
    in(" IN "),
    /** NOT IN */
    notIn(" NOT IN "),
    /** BETWEEN */
    between(" BETWEEN "),
    /** NOT BETWEEN */
    notBetween(" NOT BETWEEN "),
    /** GLOB */
    glob(" GLOB "),
    /** NOT GLOB */
    notGlob(" NOT GLOB "),
    /** MATCH */
    match(" MATCH ");

    private static final Map<Operator, Operator> contraryRegistry = new HashMap<>();

    static {
        contraryRegistry.put(eq, neq);
        contraryRegistry.put(neq, eq);
        contraryRegistry.put(is, isNot);
        contraryRegistry.put(isNot, is);
        contraryRegistry.put(gt, lte);
        contraryRegistry.put(lte, gt);
        contraryRegistry.put(lt, gte);
        contraryRegistry.put(gte, lt);
        contraryRegistry.put(like, notLike);
        contraryRegistry.put(notLike, like);
        contraryRegistry.put(in, notIn);
        contraryRegistry.put(notIn, in);
        contraryRegistry.put(between, notBetween);
        contraryRegistry.put(notBetween, between);
        contraryRegistry.put(glob, notGlob);
        contraryRegistry.put(notGlob, glob);
    }

    private final String operator;

    Operator(String operator) {
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
