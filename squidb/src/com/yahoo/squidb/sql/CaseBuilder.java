/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.sql;

import java.util.ArrayList;
import java.util.List;

/**
 * Builder class for creating CASE statements. CASE statements may optionally have a base expression (CASE
 * expression WHEN ... THEN ...), which is compared to each WHEN expression during evaluation. They may also
 * optionally have an ELSE branch if no WHEN branch evaluates to true (otherwise the result is NULL).
 *
 * @see <a href="https://www.sqlite.org/lang_expr.html#case">https://www.sqlite.org/lang_expr.html#case</a>
 */
public class CaseBuilder {

    private final Object baseExpression;
    private ArrayList<Object> whens;
    private ArrayList<Object> thens;
    private Object elseValue;

    private boolean ended;

    CaseBuilder(Object baseExpression) {
        this.baseExpression = baseExpression;
    }

    /**
     * Add a WHEN branch to this statement
     *
     * @param when the expression to evaluate, or to compare against the base expression
     * @param then the result if evaluation passes
     * @return this builder object, to allow chaining method calls
     */
    public CaseBuilder when(Object when, Object then) {
        if (ended) {
            throw new IllegalStateException("Can't call when() after calling end()");
        }
        if (whens == null) {
            // always intialized together
            whens = new ArrayList<Object>();
            thens = new ArrayList<Object>();
        }
        whens.add(when);
        thens.add(then);
        return this;
    }

    /**
     * Set the value of the ELSE branch for this statement
     *
     * @param value the expression to evaluate when no WHEN branch evaluation passes
     * @return this builder object, to allow chaining method calls
     */
    public CaseBuilder elseExpr(Object value) {
        if (ended) {
            throw new IllegalStateException("Can't call elseExpr() after calling end()");
        }
        this.elseValue = value;
        return this;
    }

    /**
     * Completes this CASE statement and creates a Function representing it
     */
    public <T> Function<T> end() {
        if (whens == null || whens.size() == 0) {
            throw new IllegalStateException("CASE statement must have at least one WHEN branch");
        }
        if (ended) {
            throw new IllegalStateException("end() already called on this CASE statement");
        }
        ended = true;

        return new Function<T>() {
            @Override
            protected void appendFunctionExpression(StringBuilder sql, List<Object> selectionArgsBuilder) {
                sql.append("(CASE");
                if (baseExpression != null) {
                    sql.append(' ');
                    SqlUtils.addToSqlString(sql, selectionArgsBuilder, baseExpression);
                }
                int size = whens.size();
                for (int i = 0; i < size; i++) {
                    sql.append(" WHEN ");
                    SqlUtils.addToSqlString(sql, selectionArgsBuilder, whens.get(i));
                    sql.append(" THEN ");
                    SqlUtils.addToSqlString(sql, selectionArgsBuilder, thens.get(i));
                }
                if (elseValue != null) {
                    sql.append(" ELSE ");
                    SqlUtils.addToSqlString(sql, selectionArgsBuilder, elseValue);
                }
                sql.append(" END)");
            }
        };
    }
}
