/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.sql;

import android.text.TextUtils;

import java.util.Collections;
import java.util.List;

/**
 * Criterions are primarily used to construct the WHERE clause of a SQL statement. Most criterion objects can be
 * created directly from a {@link Field} (or {@link Property}, which is a Field), e.g.
 *
 * <pre>
 * Person.ID.eq(101) // "person._id = 101"
 * Person.NAME.isNotNull() // "person.name IS NOT NULL"
 * </pre>
 *
 * Criterions can be easily combined (with AND and OR) and negated (with NOT or with an appropriate contrary operator):
 *
 * <pre>
 * Person.AGE.gte(18).and(Person.IS_EMPLOYED.isTrue()) // "person.age {@code>=} 18 AND person.is_employed {@code<>} 0"
 * Person.NUM_SIBLINGS.neq(0).or(Person.NUM_PETS.neq(0)) // "person.num_siblings {@code } 0 OR person.num_pets {@code<>} 0"
 *
 * Person.AGE.gte(18).negate() // "person.age {@code<} 18"
 * Person.ID.in(1,2,3).negate() // "NOT person._id IN (1,2,3)"
 * </pre>
 *
 * Additionally, this class provides static method equivalents to the above.
 */
public abstract class Criterion extends CompilableWithArguments {

    protected final Operator operator;

    public Criterion(Operator operator) {
        this.operator = operator;
    }

    /**
     * All rows match this criterion
     */
    public static final Criterion all = new Criterion(null) {
        @Override
        protected void populate(StringBuilder sql, List<Object> selectionArgsBuilder) {
            sql.append(1);
        }

        @Override
        public Criterion negate() {
            return none;
        }
    };

    /**
     * No rows match this criterion
     */
    public static final Criterion none = new Criterion(null) {
        @Override
        protected void populate(StringBuilder sql, List<Object> selectionArgsBuilder) {
            sql.append(0);
        }

        @Override
        public Criterion negate() {
            return all;
        }
    };

    /**
     * @return a {@link Criterion} that combines the given criterions with AND
     */
    public static Criterion and(final Criterion criterion, final Criterion... criterions) {
        return new ConjunctionCriterion(Operator.and, criterion, criterions);
    }

    /**
     * @return a {@link Criterion} that combines the given criterions with OR
     */
    public static Criterion or(final Criterion criterion, final Criterion... criterions) {
        return new ConjunctionCriterion(Operator.or, criterion, criterions);
    }

    /**
     * @return a {@link Criterion} that negates the given criterion
     */
    public static Criterion not(Criterion criterion) {
        return new NegationCriterion(criterion);
    }

    /**
     * @return a {@link Criterion} that evaluates whether the specified {@link Query} returns any rows
     */
    public static Criterion exists(final Query query) {
        return new Criterion(Operator.exists) {
            @Override
            protected void populate(StringBuilder sql, List<Object> selectionArgsBuilder) {
                sql.append(operator).append("(");
                query.appendCompiledStringWithArguments(sql, selectionArgsBuilder);
                sql.append(")");
            }
        };
    }

    /**
     * @return a {@link Criterion} that evaluates the raw selection and selection args
     */
    public static Criterion fromRawSelection(final String selection, final String[] selectionArgs) {
        if (TextUtils.isEmpty(selection)) {
            return Criterion.all;
        }
        return new Criterion(null) {
            @Override
            protected void populate(StringBuilder sql, List<Object> selectionArgsBuilder) {
                sql.append(selection);
                if (selectionArgs != null && selectionArgsBuilder != null) {
                    Collections.addAll(selectionArgsBuilder, selectionArgs);
                }
            }
        };
    }

    /**
     * Construct a {@link Criterion} using the given literal value, e.g. {@code "WHERE 3"} or {@code "WHERE 'happy'"}.
     * SQLite evaluates the where clause as a boolean expression by casting the result to a numeric value and comparing
     * to zero. For example, the values NULL, 0.0, 0, 'sqlite' and '0sqlite' are all considered to be false. Values 1,
     * 1.0, 0.1, -0.1 and '1sqlite' are considered to be true.
     */
    public static Criterion literal(final Object value) {
        return new Criterion(null) {
            @Override
            protected void populate(StringBuilder sql, List<Object> selectionArgsBuilder) {
                SqlUtils.addToSqlString(sql, selectionArgsBuilder, value);
            }
        };
    }

    /**
     * Convert this object's contents to valid SQL and append it to the SQL string being built. If
     * {@code selectionArgsBuilder} is provided, then use the '?' character in place of literal values and add those
     * values to {@code selectionArgsBuilder} instead. In most cases, use
     * {@link SqlUtils#addToSqlString(StringBuilder, List, Object)} to handle this properly.
     *
     * @param sql the SQL string being built
     * @param selectionArgsBuilder holds arguments that should bind to '?' characters appended to sql
     */
    protected abstract void populate(StringBuilder sql, List<Object> selectionArgsBuilder);

    /**
     * Append a string representation of this Criterion
     *
     * @param sql StringBuilder to append to
     * @param selectionArgsBuilder list to contain values that bind to the replaceable character '?'
     */
    @Override
    void appendCompiledStringWithArguments(StringBuilder sql, List<Object> selectionArgsBuilder) {
        sql.append("(");
        populate(sql, selectionArgsBuilder);
        sql.append(")");
    }

    /**
     * @return a logically negated version of this criterion. If it is a case of simple operator negation,
     * only the operator will be negated (e.g. {@code "myField = 3"} would become {@code "myField <> 3"}). If the
     * negation cannot be performed in the operator, the criterion will be wrapped in a "not" statement (e.g. {@code
     * "not (myField like 'Sam')"}).
     */
    public Criterion negate() {
        return not(this);
    }

    /**
     * @param criterion another criterion to be appended with AND
     * @return a criterion equivalent to (this AND criterion)
     */
    public Criterion and(Criterion criterion) {
        return and(this, criterion);
    }

    /**
     * @param criterion another criterion to be appended with OR
     * @return a criterion equivalent to (this OR criterion)
     */
    public Criterion or(Criterion criterion) {
        return or(this, criterion);
    }

    @Override
    public boolean equals(Object o) {
        return this == o || !(o == null || getClass() != o.getClass()) && this.toString().equals(o.toString());
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }
}
