/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.sql;

import com.yahoo.squidb.utility.Logger;
import com.yahoo.squidb.utility.VersionCode;

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
 * Person.AGE.gte(18).and(Person.IS_EMPLOYED.isTrue()) // person.age &gt;= 18 AND person.is_employed &lt;&gt; 0
 * Person.NUM_SIBLINGS.neq(0).or(Person.NUM_PETS.neq(0)) // person.num_siblings &lt;&gt; 0 OR person.num_pets &lt;&gt; 0
 *
 * Person.AGE.gte(18).negate() // person.age &lt; 18
 * Person.ID.in(1,2,3).negate() // NOT person._id IN (1,2,3)
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
     * @return a {@link Criterion} that combines the given criterions with AND
     */
    public static Criterion and(Criterion criterion, Criterion... criterions) {
        return new ConjunctionCriterion(Operator.and, criterion, criterions);
    }

    /**
     * @return a {@link Criterion} that combines the given criterions with AND
     */
    public static Criterion and(List<Criterion> criterions) {
        return new ConjunctionCriterion(Operator.and, criterions);
    }

    /**
     * @return a {@link Criterion} that combines the given criterions with OR
     */
    public static Criterion or(Criterion criterion, Criterion... criterions) {
        return new ConjunctionCriterion(Operator.or, criterion, criterions);
    }

    /**
     * @return a {@link Criterion} that combines the given criterions with OR
     */
    public static Criterion or(List<Criterion> criterions) {
        return new ConjunctionCriterion(Operator.or, criterions);
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
            protected void populate(SqlBuilder builder, boolean forSqlValidation) {
                builder.sql.append(operator).append("(");
                query.appendToSqlBuilder(builder, forSqlValidation);
                builder.sql.append(")");
            }
        };
    }

    /**
     * @return a {@link Criterion} that evaluates the raw selection and selection args. If the selection string is
     * empty, this will return null.
     *
     * Note: if you use Criterion.fromRawSelection in a SQL statement, you should not call
     * {@link SqlStatement#toRawSql(VersionCode) toRawSql} on that statement and then execute the resulting SQL, as it
     * may contain unbound arguments. Instead, execute the statement using e.g.
     * {@link com.yahoo.squidb.data.SquidDatabase#query(Class, Query) SquidDatabase.query},
     * {@link com.yahoo.squidb.data.SquidDatabase#update(Update) SquidDatabase.update}, or
     * {@link com.yahoo.squidb.data.SquidDatabase#delete(Delete) SquidDatabase.delete}
     */
    public static Criterion fromRawSelection(final String selection, final String[] selectionArgs) {
        if (SqlUtils.isEmpty(selection)) {
            return null;
        }
        return new Criterion(null) {
            @Override
            protected void populate(SqlBuilder builder, boolean forSqlValidation) {
                if (forSqlValidation) {
                    builder.sql.append("(");
                }
                builder.sql.append(selection);
                if (selectionArgs != null && selectionArgs.length > 0) {
                    if (builder.args == null) {
                        Logger.w(Logger.LOG_TAG, "Raw selection criterion converted to raw SQL with unbound arguments");
                    } else {
                        Collections.addAll(builder.args, selectionArgs);
                    }
                }
                if (forSqlValidation) {
                    builder.sql.append(")");
                }
            }
        };
    }

    /**
     * Construct a {@link Criterion} using the given literal value, e.g. <code>"WHERE 3"</code> or
     * <code>"WHERE 'happy'"</code>. SQLite evaluates the where clause as a boolean expression by casting the result to
     * a numeric value and comparing to zero. For example, the values NULL, 0.0, 0, 'sqlite' and '0sqlite' are all
     * considered to be false. Values 1, 1.0, 0.1, -0.1 and '1sqlite' are considered to be true.
     */
    public static Criterion literal(final Object value) {
        return new Criterion(null) {
            @Override
            protected void populate(SqlBuilder builder, boolean forSqlValidation) {
                builder.addValueToSql(value, forSqlValidation);
            }
        };
    }

    /**
     * Convert this object's contents to valid SQL and append it to the SQL string being built. If
     * <code>selectionArgsBuilder</code> is provided, then use the '?' character in place of literal values and add
     * those values to <code>selectionArgsBuilder</code> instead. In most cases, use
     * {@link SqlBuilder#addValueToSql(Object, boolean)} to handle this properly.
     *
     * @param builder The {@link SqlBuilder} for building the SQL statement
     * @param forSqlValidation forSqlValidation true if this statement is being compiled to validate against malicious
     * SQL
     */
    protected abstract void populate(SqlBuilder builder, boolean forSqlValidation);

    /**
     * Append a string representation of this Criterion
     *
     * @param builder The {@link SqlBuilder} for building the SQL statement
     * @param forSqlValidation forSqlValidation true if this statement is being compiled to validate against malicious
     * SQL
     */
    @Override
    void appendToSqlBuilder(SqlBuilder builder, boolean forSqlValidation) {
        builder.sql.append("(");
        populate(builder, forSqlValidation);
        builder.sql.append(")");
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
     * @param criterion another criterion to be appended with AND. If null, this Criterion will be returned unmodified.
     * @return a criterion equivalent to (this AND criterion)
     */
    public Criterion and(Criterion criterion) {
        if (criterion == null) {
            return this;
        }
        return and(this, criterion);
    }

    /**
     * @param criterion another criterion to be appended with OR. If null, this Criterion will be returned unmodified.
     * @return a criterion equivalent to (this OR criterion)
     */
    public Criterion or(Criterion criterion) {
        if (criterion == null) {
            return this;
        }
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
