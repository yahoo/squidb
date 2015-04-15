/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.sql;

import android.text.TextUtils;

import java.util.List;

/**
 * A SQLite database object
 */
abstract class DBObject<T extends DBObject<?>> extends CompilableWithArguments implements Cloneable {

    protected String alias;
    protected final String expression;
    protected final String qualifier;

    /**
     * @param expression the string-literal representation of this object
     */
    protected DBObject(String expression) {
        this(expression, null);
    }

    /**
     * @param expression the string-literal representation of this object
     * @param qualifier the string-literal representation of a qualifying object, e.g. a table name if this object
     * represents a column
     */
    protected DBObject(String expression, String qualifier) {
        this.expression = expression;
        this.qualifier = qualifier;
    }

    /**
     * Clones this object with an alias
     */
    @SuppressWarnings("unchecked")
    public T as(String newAlias) {
        try {
            T clone = (T) clone();
            clone.alias = newAlias;
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @return true if this object has an alias
     */
    public boolean hasAlias() {
        return !TextUtils.isEmpty(alias);
    }

    /**
     * @return true if this object has a qualifier
     */
    public boolean hasQualifier() {
        return !TextUtils.isEmpty(qualifier);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DBObject<?> dbObject = (DBObject<?>) o;

        if (alias != null ? !alias.equals(dbObject.alias) : dbObject.alias != null) {
            return false;
        }
        String myExpression = getExpression();
        String otherExpression = dbObject.getExpression();
        if (myExpression != null ? !myExpression.equals(otherExpression) : otherExpression != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = alias != null ? alias.hashCode() : 0;
        String expression = getExpression();
        result = 31 * result + (expression != null ? expression.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Expression=").append(getExpression());
        if (hasQualifier()) {
            sb.append(" Qualifier=").append(qualifier);
        }
        if (hasAlias()) {
            sb.append(" Alias=").append(alias);
        }
        return sb.toString();
    }

    /**
     * @return the string-literal representation of this object, or its alias (if it has one)
     */
    public final String getName() {
        if (hasAlias()) {
            return alias;
        }
        return getExpression();
    }

    /**
     * @return the string-literal representation of this object
     */
    public String getExpression() {
        return expression;
    }

    /**
     * @return the string-literal representation of this object, prepended with its qualifier (if it has one)
     */
    public final String getQualifiedExpression() {
        if (hasQualifier()) {
            return qualifier + '.' + getExpression();
        }
        return getExpression();
    }

    /**
     * Appends an expression used in a SELECT statement to represent this object
     *
     * @param sql StringBuilder to append to
     * @param selectionArgsBuilder list to contain values that bind to the replaceable character '?'
     */
    @Override
    void appendCompiledStringWithArguments(StringBuilder sql, List<Object> selectionArgsBuilder) {
        appendQualifiedExpression(sql, selectionArgsBuilder);
        if (hasAlias()) {
            sql.append(" AS ").append(alias);
        } else if (hasQualifier()) {
            sql.append(" AS ").append(expression);
        }
    }

    protected void appendQualifiedExpression(StringBuilder sql, List<Object> selectionArgsBuilder) {
        sql.append(getQualifiedExpression());
    }
}
