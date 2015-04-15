/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.sql;

import java.util.List;

/**
 * Ordering term for a SELECT statement. In addition to the {@link #asc(Object) asc} and {@link #desc(Object) desc}
 * static methods, you can create an ordering term from a Field using {@link Field#asc()} and {@link Field#desc()}.
 */
public class Order extends CompilableWithArguments {

    private static enum OrderType {
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

        StringBuilder orderCase = new StringBuilder();
        orderCase.append("(CASE ").append(field.getName()).append(" ");
        for (int i = 0; i < order.length; i++) {
            orderCase.append("WHEN ");
            SqlUtils.addToSqlString(orderCase, order[i]);
            orderCase.append(" THEN ").append(Integer.toString(i)).append(" ");
        }
        orderCase.append(" ELSE ").append(Integer.toString(order.length)).append(" END)");
        return fromExpression(orderCase.toString());
    }

    public static Order fromExpression(String expression) {
        return new Order(expression, OrderType.RAW);
    }

    @Override
    void appendCompiledStringWithArguments(StringBuilder sql, List<Object> selectionArgsBuilder) {
        if (orderType == OrderType.RAW) {
            sql.append(expression);
        } else {
            SqlUtils.addToSqlString(sql, selectionArgsBuilder, expression);
            sql.append(" ").append(orderType.toString());
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
