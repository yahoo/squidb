/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.sql;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A {@link Field} defined as a SQLite function.
 * <p>
 * When used with queries and models, it is typical to wrap the function with a {@link Property}:
 *
 * <pre>
 * Function&lt;Long&gt; maxFunc = Function.max(Model.TIMESTAMP);
 * LongProperty maxTimestamp = LongProperty.fromFunction(maxFunc, "maxTimestamp");
 * Query query = Query.select(Model.TYPE, maxTimestamp).groupBy(Model.TYPE);
 * SquidCursor&lt;Model&gt; cursor = db.query(Model.class, query);
 * </pre>
 *
 * This allows the value to be read directly from the cursor using
 * {@link com.yahoo.squidb.data.SquidCursor#get(Property) get(Property)}, or to be read from a model instance that was
 * created by reading from the cursor:
 *
 * <pre>
 * cursor.moveToPosition(position);
 * long maxTimestamp = cursor.get(maxTimestamp);
 * // or
 * Model model = new Model(cursor);
 * long maxTimestamp = model.get(maxTimestamp);
 * </pre>
 */
public abstract class Function<TYPE> extends Field<TYPE> {

    /** constant value 1, suitable for use with a boolean function */
    public static final Function<Integer> TRUE = rawFunction("1");

    /** constant value 0, suitable for use with a boolean function */
    public static final Function<Integer> FALSE = rawFunction("0");

    public Function() {
        this("");
    }

    protected Function(@Nonnull String expression) {
        super(expression);
    }

    @Override
    protected void appendQualifiedExpression(@Nonnull SqlBuilder builder, boolean forSqlValidation) {
        appendFunctionExpression(builder, forSqlValidation);
    }

    protected abstract void appendFunctionExpression(@Nonnull SqlBuilder builder, boolean forSqlValidation);

    @Override
    @Nonnull
    public String getExpression() {
        throw new UnsupportedOperationException("Function expressions cannot be converted to a String without a "
                + "VersionCode for context. Instead use getExpression(VersionCode)");
    }

    /**
     * @return the expression for the function as it would be compiled with the given CompileContext
     */
    @Nonnull
    public String getExpression(@Nonnull CompileContext forCompileContext) {
        SqlBuilder builder = new SqlBuilder(forCompileContext, false);
        appendQualifiedExpression(builder, false);
        return builder.getSqlString();
    }

    @Override
    @Nonnull
    protected String expressionForComparison() {
        return getExpression(CompileContext.defaultContextForVersionCode(VERSION_FOR_TO_STRING));
    }

    /**
     * Create a Function call with the given name and list of arguments. Returns a function equivalent to
     * "functionName(arg1, arg2, ...)"
     */
    @Nonnull
    public static <T> Function<T> functionWithArguments(@Nonnull String functionName, @Nonnull Object... arguments) {
        return new ArgumentFunction<>(functionName, arguments);
    }

    /**
     * Create a Function with the given expression
     */
    @Nonnull
    public static <T> Function<T> rawFunction(@Nonnull String expression) {
        return new RawFunction<>(expression);
    }

    /**
     * Create a Function representing the result of a subquery. Note: the query must have exactly one column in its
     * result set (i.e. one field in the SELECT clause) for this to be valid SQL.
     */
    @Nonnull
    public static <T> Function<T> fromQuery(@Nonnull Query query) {
        return new QueryFunction<>(query);
    }

    /**
     * Create a Function that transforms all ASCII characters of an input string to uppercase
     */
    @Nonnull
    public static Function<String> upper(@Nonnull Field<String> field) {
        return new ArgumentFunction<>("UPPER", field);
    }

    /**
     * Create a Function that transforms all ASCII characters of an input string to lowercase
     */
    @Nonnull
    public static Function<String> lower(@Nonnull Field<String> field) {
        return new ArgumentFunction<>("LOWER", field);
    }

    /**
     * Create a Function that computes a substring starting at {@code start}. Note that substring in SQLite is
     * 1-indexed, i.e. the leftmost character is index 1.
     */
    @Nonnull
    public static Function<String> substr(@Nonnull Field<String> field, @Nonnull Object start) {
        return new ArgumentFunction<>("SUBSTR", field, start);
    }

    /**
     * Create a Function that computes a substring starting at {@code start} and with {@code length} number of
     * characters. Note that substring in SQLite is 1-indexed, i.e. the leftmost character is index 1.
     */
    @Nonnull
    public static Function<String> substr(@Nonnull Field<String> field, @Nonnull Object start, @Nonnull Object length) {
        return new ArgumentFunction<>("SUBSTR", field, start, length);
    }

    /**
     * Create a Function that concatenates the operands as a string
     */
    @Nonnull
    public static Function<String> strConcat(@Nonnull Object... objects) {
        if (objects.length == 0) {
            throw new IllegalArgumentException("Can't concatenate an empty list of objects");
        }
        return new ArgumentFunction<String>("", objects) {
            @Override
            protected String separator() {
                return " || ";
            }
        };
    }

    /**
     * Create a Function that counts all rows (i.e. count(*))
     */
    @Nonnull
    public static Function<Integer> count() {
        return new RawFunction<>("COUNT(*)");
    }

    /**
     * Create a Function that counts the number of values of the specified field
     */
    @Nonnull
    public static Function<Integer> count(@Nonnull Field<?> field) {
        return new ArgumentFunction<>("COUNT", field);
    }

    /**
     * @return a Function that counts the number of distinct values of the specified field
     */
    @Nonnull
    public static Function<Integer> countDistinct(@Nonnull Field<?> field) {
        return new DistinctArgumentFunction<>("COUNT", field);
    }

    /**
     * Create a Function that returns the length of the specified input. For string input, returns the number of
     * characters (not bytes) in the string prior to the first NUL character. For blob input, returns the number of
     * bytes in the blob. For numeric input, returns the length of a string representation of the number.
     */
    @Nonnull
    public static Function<Integer> length(@Nonnull Field<?> field) {
        return new ArgumentFunction<>("LENGTH", field);
    }

    /**
     * Create a function that returns the maximum value of all values in a group
     */
    @Nonnull
    public static <T> Function<T> max(@Nonnull Field<T> field) {
        return new ArgumentFunction<>("MAX", field);
    }

    /**
     * Create a function that returns the minimum value of all values in a group
     */
    @Nonnull
    public static <T> Function<T> min(@Nonnull Field<T> field) {
        return new ArgumentFunction<>("MIN", field);
    }

    /**
     * Create a function that returns the sum of values in a group
     */
    @Nonnull
    public static <T extends Number> Function<T> sum(@Nonnull Field<T> field) {
        return new ArgumentFunction<>("SUM", field);
    }

    /**
     * Create a function that returns the sum of distinct values in a group
     */
    @Nonnull
    public static <T extends Number> Function<T> sumDistinct(@Nonnull Field<T> field) {
        return new DistinctArgumentFunction<>("SUM", field);
    }

    /**
     * Create a function that returns the average value in a group
     */
    @Nonnull
    public static <T extends Number> Function<Double> avg(@Nonnull Field<T> field) {
        return new ArgumentFunction<>("AVG", field);
    }

    /**
     * Create a function that returns the average value of the distinct values in a group
     */
    @Nonnull
    public static <T extends Number> Function<Double> avgDistinct(@Nonnull Field<T> field) {
        return new DistinctArgumentFunction<>("AVG", field);
    }

    /**
     * Create a function that returns the absolute value of a numeric input
     */
    @Nonnull
    public static <T extends Number> Function<T> abs(@Nonnull Field<T> field) {
        return new ArgumentFunction<>("ABS", field);
    }

    /**
     * Create a Function that concatenates non-null values in a group, separated by commas
     */
    @Nonnull
    public static Function<String> groupConcat(@Nonnull Field<?> field) {
        return new ArgumentFunction<>("GROUP_CONCAT", field);
    }

    /**
     * Create a Function that concatenates non-null distinct values in a group, separated by commas
     */
    @Nonnull
    public static Function<String> groupConcatDistinct(@Nonnull Field<?> field) {
        return new DistinctArgumentFunction<>("GROUP_CONCAT", field);
    }

    /**
     * Create a Function that concatenates non-null values in a group, separated by the specified separator
     */
    @Nonnull
    public static Function<String> groupConcat(@Nonnull Field<?> field, @Nullable String separator) {
        return new ArgumentFunction<>("GROUP_CONCAT", field, separator);
    }

    /**
     * Create a Function that returns 1 (true) if the criterion passes or 0 (false) otherwise
     */
    @Nonnull
    public static Function<Integer> caseWhen(@Nonnull Criterion when) {
        return caseWhen(when, TRUE, FALSE);
    }

    /**
     * Create a Function that returns the given values based on whether the specified criterion passes
     */
    @Nonnull
    public static <T> Function<T> caseWhen(@Nonnull final Criterion when, @Nullable final Object ifTrue,
            @Nullable final Object ifFalse) {
        return caseWhen(when, ifTrue).elseExpr(ifFalse).end();
    }

    /**
     * Begins a CASE statement, populating it with the first WHEN ... THEN branch
     */
    @Nonnull
    public static CaseBuilder caseWhen(@Nonnull Criterion when, @Nullable Object then) {
        if (when == null) {
            throw new IllegalArgumentException("Can't construct a CASE WHEN statement with a null criterion");
        }
        return new CaseBuilder(null).when(when, then);
    }

    /**
     * Begins a CASE statement with a base expression. Note: to use SQLite NULL literal as the base expression,
     * use {@link Field#NULL}.
     */
    @Nonnull
    public static CaseBuilder caseExpr(@Nonnull Object baseExpression) {
        return new CaseBuilder(baseExpression);
    }

    /**
     * Create a Function that casts the input to the specified type. Note this is a SQLite cast.
     */
    @Nonnull
    public static <T, R> Function<R> cast(@Nonnull final Field<T> field, @Nonnull final String newType) {
        return new ArgumentFunction<R>("CAST") {
            @Override
            protected void appendArgumentList(@Nonnull SqlBuilder builder, @Nonnull Object[] arguments,
                    boolean forSqlValidation) {
                builder.addValueToSql(field, forSqlValidation);
                builder.sql.append(" AS ").append(newType);
            }
        };
    }

    /**
     * Create a Function that returns the first non-null value from the list of input values. If all input values are
     * null, this function returns null.
     */
    @Nonnull
    public static <T> Function<T> coalesce(@Nonnull Object... values) {
        return new ArgumentFunction<>("COALESCE", values);
    }

    /**
     * Create a Function that evaluates an arithmetic addition expression
     */
    @Nonnull
    public static <T extends Number> Function<T> add(@Nonnull Object... args) {
        return new MathFunction<>(MathOperator.PLUS, args);
    }

    /**
     * Create a Function that evaluates an arithmetic subtraction expression
     */
    @Nonnull
    public static <T extends Number> Function<T> subtract(@Nonnull Object... args) {
        return new MathFunction<>(MathOperator.MINUS, args);
    }

    /**
     * Create a Function that evaluates an arithmetic multiplication expression
     */
    @Nonnull
    public static <T extends Number> Function<T> multiply(@Nonnull Object... args) {
        return new MathFunction<>(MathOperator.MULT, args);
    }

    /**
     * Create a Function that evaluates an arithmetic division expression
     */
    @Nonnull
    public static <T extends Number> Function<T> divide(@Nonnull Object... args) {
        return new MathFunction<>(MathOperator.DIVIDE, args);
    }

    /**
     * Create a Function that evaluates an arithmetic modulo expression
     */
    @Nonnull
    public static <T extends Number> Function<T> modulo(@Nonnull Object lhs, @Nonnull Object rhs) {
        return new MathFunction<>(MathOperator.MODULO, lhs, rhs);
    }

    /**
     * Create a Function that evaluates a binary AND expression
     */
    @Nonnull
    public static <T extends Number> Function<T> bitwiseAnd(@Nonnull Object... args) {
        return new MathFunction<>(MathOperator.BITWISE_AND, args);
    }

    /**
     * Create a Function that evaluates a binary OR expression
     */
    @Nonnull
    public static <T extends Number> Function<T> bitwiseOr(@Nonnull Object... args) {
        return new MathFunction<>(MathOperator.BITWISE_OR, args);
    }
}
