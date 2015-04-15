/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.sql;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Fields represent a selectable attribute, e.g. a column, function, or literal value. Most clients will not use this
 * class directly.
 *
 * @param <TYPE> the type of this Field
 */
public class Field<TYPE> extends DBObject<Field<TYPE>> {

    /** SQLite NULL literal value */
    public static final Field<Void> NULL = new Field<Void>("NULL");

    /**
     * @param expression the string-literal representation of this field
     */
    protected Field(String expression) {
        super(expression);
    }

    /**
     * @param expression the string-literal representation of this field
     * @param qualifier the string-literal representation of a qualifying object, e.g. a table name
     */
    protected Field(String expression, String qualifier) {
        super(expression, qualifier);
    }

    /**
     * Construct a Field with the given expression
     *
     * @param expression the string-literal representation of the returned field
     */
    public static <T> Field<T> field(String expression) {
        return new Field<T>(expression);
    }

    /**
     * Construct a Field with the given expression and qualifier
     *
     * @param expression the string-literal representation of this field
     * @param qualifier the string-literal representation of a qualifying object, e.g. a table name
     */
    public static <T> Field<T> field(String expression, String qualifier) {
        return new Field<T>(expression, qualifier);
    }

    /**
     * @return a {@link Criterion} that the field must be equal to the given value
     */
    public Criterion eq(Object value) {
        if (value == null) {
            return isNull();
        }
        return new BinaryCriterion(this, Operator.eq, value);
    }

    /**
     * @return a {@link Criterion} that the field must be equal to the given string, ignoring case. Due to a bug in
     * sqlite, this will only work for ASCII characters.
     */
    public Criterion eqCaseInsensitive(String value) {
        if (value == null) {
            return isNull();
        }
        return new BinaryCriterion(this, Operator.eq, value) {
            @Override
            protected void afterPopulateOperator(StringBuilder sql, List<Object> selectionArgsBuilder) {
                super.afterPopulateOperator(sql, selectionArgsBuilder);
                sql.append(" COLLATE NOCASE ");
            }
        };
    }

    /**
     * @return a {@link Criterion} comparing the field to the given value using the IS operator.
     */
    public Criterion is(Object value) {
        return new BinaryCriterion(this, Operator.is, value);
    }

    /**
     * @return a {@link Criterion} comparing the field to the given value using the IS NOT operator.
     */
    public Criterion isNot(Object value) {
        return new BinaryCriterion(this, Operator.isNot, value);
    }

    /**
     * @return a {@link Criterion} that the field must not be equal to the given value
     */
    public Criterion neq(Object value) {
        if (value == null) {
            return isNotNull();
        }
        return new BinaryCriterion(this, Operator.neq, value);
    }

    /**
     * @return a {@link Criterion} that the field must be greater than the given value
     */
    public Criterion gt(Object value) {
        return new BinaryCriterion(this, Operator.gt, value);
    }

    /**
     * @return a {@link Criterion} that the field must be greater than or equal to the given value
     */
    public Criterion gte(Object value) {
        return new BinaryCriterion(this, Operator.gte, value);
    }

    /**
     * @return a {@link Criterion} that the field must be less than the given value
     */
    public Criterion lt(Object value) {
        return new BinaryCriterion(this, Operator.lt, value);
    }

    /**
     * @return a {@link Criterion} that the field must be less than or equal to the given value
     */
    public Criterion lte(Object value) {
        return new BinaryCriterion(this, Operator.lte, value);
    }

    /**
     * @return a {@link Criterion} that the field must be null
     */
    public Criterion isNull() {
        return is(null);
    }

    /**
     * @return a {@link Criterion} that the field must not be null
     */
    public Criterion isNotNull() {
        return isNot(null);
    }

    /**
     * @return a {@link Criterion} that the field must be between the specified lower and upper bounds
     */
    public Criterion between(final Object lower, final Object upper) {
        return new BinaryCriterion(this, Operator.between, null) {
            @Override
            protected void afterPopulateOperator(StringBuilder sql, List<Object> selectionArgsBuilder) {
                SqlUtils.addToSqlString(sql, selectionArgsBuilder, lower);
                sql.append(" AND ");
                SqlUtils.addToSqlString(sql, selectionArgsBuilder, upper);
            }
        };
    }

    /**
     * @param value the pattern to compare against
     * @return a {@link Criterion} that the field is LIKE the given pattern
     */
    public Criterion like(Object value) {
        return new BinaryCriterion(this, Operator.like, value);
    }

    /**
     * Returns a {@link Criterion} that the field is LIKE the given pattern, using the specified escape character to
     * escape the '%' and '_' meta-characters and itself. If your pattern is a string,
     * note that this method will not alter the input string for you; use {@link SqlUtils#escapeLikePattern(String,
     * char) SqlUtils.escapeLikePattern()} to add escapes where necessary.
     *
     * @param pattern the pattern to compare against
     * @param escape a character in the like pattern that escapes the '%' and '_' meta-characters and itself
     * @return a {@link Criterion} that the field is LIKE the given pattern
     */
    public Criterion like(Object pattern, final char escape) {
        return new BinaryCriterion(this, Operator.like, pattern) {
            @Override
            protected void afterPopulateOperator(StringBuilder sql, List<Object> selectionArgsBuilder) {
                super.afterPopulateOperator(sql, selectionArgsBuilder);
                sql.append(" ESCAPE ").append(SqlUtils.toSanitizedString(Character.toString(escape)));
            }
        };
    }

    /**
     * @param value the pattern to compare against
     * @return a {@link Criterion} that the field matches the given pattern
     */
    public Criterion glob(Object value) {
        return new BinaryCriterion(this, Operator.glob, value);
    }

    /**
     * @return a {@link Criterion} that the field's value is in the list of values specified
     */
    public Criterion in(Object... values) {
        return in(Arrays.asList(values));
    }

    /**
     * @return a {@link Criterion} that the field's value is in the collection of values
     */
    public Criterion in(final Collection<?> values) {
        return new BinaryCriterion(this, Operator.in, values) {
            @Override
            protected void afterPopulateOperator(StringBuilder sql, List<Object> selectionArgsBuilder) {
                sql.append("(");
                SqlUtils.addCollectionArgToSqlString(sql, selectionArgsBuilder, values);
                sql.append(")");
            }
        };
    }

    /**
     * @return a {@link Criterion} that the field's value is in the result of the {@link Query}
     */
    public Criterion in(Query query) {
        if (query == null) {
            return in(Collections.EMPTY_SET);
        }
        return new BinaryCriterion(this, Operator.in, query);
    }

    /**
     * @return a {@link Criterion} using this field as a literal value
     */
    public Criterion asCriterion() {
        return Criterion.literal(this);
    }

    /**
     * @return an {@link Order} that sorts ascending by this field's value
     */
    public Order asc() {
        return Order.asc(this);
    }

    /**
     * @return an {@link Order} that sorts descending by this field's value
     */
    public Order desc() {
        return Order.desc(this);
    }

    /**
     * @return an {@link Order} that sorts by this field's value in the order specified by the array of values
     */
    public Order byArray(TYPE[] order) {
        return Order.byArray(this, order);
    }
}
