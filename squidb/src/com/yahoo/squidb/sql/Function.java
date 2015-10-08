/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.sql;

import com.yahoo.squidb.utility.VersionCode;

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
        this(null);
    }

    protected Function(String expression) {
        super(expression);
    }

    @Override
    protected void appendQualifiedExpression(SqlBuilder builder, boolean forSqlValidation) {
        appendFunctionExpression(builder, forSqlValidation);
    }

    protected abstract void appendFunctionExpression(SqlBuilder builder, boolean forSqlValidation);

    @Override
    public String getExpression() {
        throw new UnsupportedOperationException("Function expressions cannot be converted to a String without a "
                + "VersionCode for context. Instead use getExpression(VersionCode)");
    }

    /**
     * Return the expression for the function as it would be compiled for the given SQLite version
     */
    public String getExpression(VersionCode forSqliteVersion) {
        SqlBuilder builder = new SqlBuilder(forSqliteVersion, false);
        appendQualifiedExpression(builder, false);
        return builder.getSqlString();
    }

    @Override
    protected String expressionForComparison() {
        return getExpression(VersionCode.LATEST);
    }

    /**
     * Create a Function call with the given name and list of arguments. Returns
     * a function equivalent to "functionName(arg1, arg2, ...)"
     */
    public static <T> Function<T> functionWithArguments(String functionName, Object... arguments) {
        return new ArgumentFunction<T>(functionName, arguments);
    }

    /**
     * Create a Function with the given expression
     */
    public static <T> Function<T> rawFunction(String expression) {
        return new RawFunction<T>(expression);
    }

    /**
     * Create a Function representing the result of a subquery. Note: the query must have exactly one column in its
     * result set (i.e. one field in the SELECT clause) for this to be valid SQL.
     */
    public static <T> Function<T> fromQuery(Query query) {
        return new QueryFunction<T>(query);
    }

    /**
     * Create a Function that transforms all ASCII characters of an input string to uppercase
     */
    public static Function<String> upper(Field<String> field) {
        return new ArgumentFunction<String>("UPPER", field);
    }

    /**
     * Create a Function that transforms all ASCII characters of an input string to lowercase
     */
    public static Function<String> lower(Field<String> field) {
        return new ArgumentFunction<String>("LOWER", field);
    }

    /**
     * Create a Function that computes a substring starting at {@code start}. Note that substring in SQLite is
     * 1-indexed, i.e. the leftmost character is index 1.
     */
    public static Function<String> substr(Field<String> field, Object start) {
        return new ArgumentFunction<String>("SUBSTR", field, start);
    }

    /**
     * Create a Function that computes a substring starting at {@code start} and with {@code length} number of
     * characters. Note that substring in SQLite is 1-indexed, i.e. the leftmost character is index 1.
     */
    public static Function<String> substr(Field<String> field, Object start, Object length) {
        return new ArgumentFunction<String>("SUBSTR", field, start, length);
    }

    /**
     * Create a Function that concatenates the operands as a string
     */
    public static Function<String> strConcat(Object... objects) {
        if (objects == null || objects.length == 0) {
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
    public static Function<Integer> count() {
        return new RawFunction<Integer>("COUNT(*)");
    }

    /**
     * Create a Function that counts the number of values of the specified field
     */
    public static Function<Integer> count(Field<?> field) {
        return new ArgumentFunction<Integer>("COUNT", field);
    }

    /**
     * @return a Function that counts the number of distinct values of the specified field
     */
    public static Function<Integer> countDistinct(Field<?> field) {
        return new DistinctArgumentFunction<Integer>("COUNT", field);
    }

    /**
     * Create a Function that returns the length of the specified input. For string input, returns the number of
     * characters (not bytes) in the string prior to the first NUL character. For blob input, returns the number of
     * bytes in the blob. For numeric input, returns the length of a string representation of the number.
     */
    public static Function<Integer> length(Field<?> field) {
        return new ArgumentFunction<Integer>("LENGTH", field);
    }

    /**
     * Create a function that returns the maximum value of all values in a group
     */
    public static <T> Function<T> max(Field<T> field) {
        return new ArgumentFunction<T>("MAX", field);
    }

    /**
     * Create a function that returns the minimum value of all values in a group
     */
    public static <T> Function<T> min(Field<T> field) {
        return new ArgumentFunction<T>("MIN", field);
    }

    /**
     * Create a function that returns the sum of values in a group
     */
    public static <T extends Number> Function<T> sum(Field<T> field) {
        return new ArgumentFunction<T>("SUM", field);
    }

    /**
     * Create a function that returns the sum of distinct values in a group
     */
    public static <T extends Number> Function<T> sumDistinct(Field<T> field) {
        return new DistinctArgumentFunction<T>("SUM", field);
    }

    /**
     * Create a function that returns the average value in a group
     */
    public static <T extends Number> Function<Double> avg(Field<T> field) {
        return new ArgumentFunction<Double>("AVG", field);
    }

    /**
     * Create a function that returns the average value of the distinct values in a group
     */
    public static <T extends Number> Function<Double> avgDistinct(Field<T> field) {
        return new DistinctArgumentFunction<Double>("AVG", field);
    }

    /**
     * Create a function that returns the absolute value of a numeric input
     */
    public static <T extends Number> Function<T> abs(Field<T> field) {
        return new ArgumentFunction<T>("ABS", field);
    }

    /**
     * Create a Function that concatenates non-null values in a group, separated by commas
     */
    public static Function<String> groupConcat(Field<?> field) {
        return new ArgumentFunction<String>("GROUP_CONCAT", field);
    }

    /**
     * Create a Function that concatenates non-null distinct values in a group, separated by commas
     */
    public static Function<String> groupConcatDistinct(Field<?> field) {
        return new DistinctArgumentFunction<String>("GROUP_CONCAT", field);
    }

    /**
     * Create a Function that concatenates non-null values in a group, separated by the specified separator
     */
    public static Function<String> groupConcat(Field<?> field, String separator) {
        return new ArgumentFunction<String>("GROUP_CONCAT", field, separator);
    }

    /**
     * Create a Function that returns 1 (true) if the criterion passes or 0 (false) otherwise
     */
    public static Function<Integer> caseWhen(Criterion when) {
        return caseWhen(when, TRUE, FALSE);
    }

    /**
     * Create a Function that returns the given values based on whether the specified criterion passes
     */
    public static <T> Function<T> caseWhen(final Criterion when, final Object ifTrue, final Object ifFalse) {
        return caseWhen(when, ifTrue).elseExpr(ifFalse).end();
    }

    /**
     * Begins a CASE statement, populating it with the first WHEN ... THEN branch
     */
    public static CaseBuilder caseWhen(Criterion when, Object then) {
        if (when == null) {
            throw new IllegalArgumentException("Can't construct a CASE WHEN statement with a null criterion");
        }
        return new CaseBuilder(null).when(when, then);
    }

    /**
     * Begins a CASE statement with a base expression. Note: to use SQLite NULL literal as the base expression,
     * use {@link Field#NULL}.
     */
    public static CaseBuilder caseExpr(Object baseExpression) {
        return new CaseBuilder(baseExpression);
    }

    /**
     * Create a Function that casts the input to the specified type. Note this is a SQLite cast.
     */
    public static <T, R> Function<R> cast(final Field<T> field, final String newType) {
        return new ArgumentFunction<R>("CAST") {
            @Override
            protected void appendArgumentList(SqlBuilder builder, Object[] arguments, boolean forSqlValidation) {
                builder.addValueToSql(field, forSqlValidation);
                builder.sql.append(" AS ").append(newType);
            }
        };
    }

    /**
     * Create a Function that returns the first non-null value from the list of input values. If all input values are
     * null, this function returns null.
     */
    public static <T> Function<T> coalesce(Object... values) {
        return new ArgumentFunction<T>("COALESCE", values);
    }

    /**
     * Create a Function that evaluates an arithmetic addition expression
     */
    public static <T extends Number> Function<T> add(Object... args) {
        return new MathFunction<T>(MathOperator.PLUS, args);
    }

    /**
     * Create a Function that evaluates an arithmetic subtraction expression
     */
    public static <T extends Number> Function<T> subtract(Object... args) {
        return new MathFunction<T>(MathOperator.MINUS, args);
    }

    /**
     * Create a Function that evaluates an arithmetic multiplication expression
     */
    public static <T extends Number> Function<T> multiply(Object... args) {
        return new MathFunction<T>(MathOperator.MULT, args);
    }

    /**
     * Create a Function that evaluates an arithmetic division expression
     */
    public static <T extends Number> Function<T> divide(Object... args) {
        return new MathFunction<T>(MathOperator.DIVIDE, args);
    }

    /**
     * Create a Function that evaluates an arithmetic modulo expression
     */
    public static <T extends Number> Function<T> modulo(Object lhs, Object rhs) {
        return new MathFunction<T>(MathOperator.MODULO, lhs, rhs);
    }

    /**
     * Create a Function that evaluates a binary AND expression
     */
    public static <T extends Number> Function<T> bitwiseAnd(Object... args) {
        return new MathFunction<T>(MathOperator.BITWISE_AND, args);
    }

    /**
     * Create a Function that evaluates a binary OR expression
     */
    public static <T extends Number> Function<T> bitwiseOr(Object... args) {
        return new MathFunction<T>(MathOperator.BITWISE_OR, args);
    }
}
