/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.sql;

/**
 * Ordering term for a SELECT statement. In addition to the {@link #asc(Object) asc} and {@link #desc(Object) desc}
 * static methods, you can create an ordering term from a Field using {@link Field#asc()} and {@link Field#desc()}.
 */
public class Order extends CompilableWithArguments {

    private enum OrderType {
        DESC, ASC, RAW
    }

    private final Object expression;
    private final OrderType orderType;

    private Order(Object expression) {
        this(expression, OrderType.ASC);
    }

    private Order(Object expression, OrderType orderType) {
        this.expression = expression;
        this.orderType = orderType;
    }

    /**
     * Sort the results in ascending ('ASC') order by the given expression
     */
    public static Order asc(Object expression) {
        return new Order(expression);
    }

    /**
     * Sort the results in descending ('DESC') order by the given expression
     */
    public static Order desc(Object expression) {
        return new Order(expression, OrderType.DESC);
    }

    /**
     * Sort the results by the value of the {@link Field}, ordered by values in the array. This is a bit of a hack.
     *
     * @param field the Field to order by
     * @param order values for the specified field, in the order they should appear in the result set
     */
    public static <T> Order byArray(Field<T> field, T[] order) {
        if (order == null || order.length == 0) {
            return Order.asc("0");
        }

        CaseBuilder caseBuilder = Function.caseExpr(field);
        for (int i = 0; i < order.length; i++) {
            caseBuilder.when(order[i], i);
        }
        caseBuilder.elseExpr(order.length);
        return Order.asc(caseBuilder.end());
    }

    public static Order fromExpression(String expression) {
        return new Order(expression, OrderType.RAW);
    }

    @Override
    void appendToSqlBuilder(SqlBuilder builder, boolean forSqlValidation) {
        if (orderType == OrderType.RAW) {
            builder.sql.append(expression);
        } else {
            builder.addValueToSql(expression, forSqlValidation);
            builder.sql.append(" ").append(orderType.toString());
        }
    }

    /**
     * @return an Order term that is the reverse of this one
     */
    public Order reverse() {
        if (orderType == OrderType.ASC) {
            return new Order(expression, OrderType.DESC);
        } else if (orderType == OrderType.DESC) {
            return new Order(expression, OrderType.ASC);
        } else {
            throw new UnsupportedOperationException("reverse() is not currently supported for order type "
                    + orderType.toString());
        }
    }
}
