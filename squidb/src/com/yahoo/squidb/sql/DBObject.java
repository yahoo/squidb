/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.sql;

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
        return !SqlUtils.isEmpty(alias);
    }

    /**
     * @return true if this object has a qualifier
     */
    public boolean hasQualifier() {
        return !SqlUtils.isEmpty(qualifier);
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
        String myExpression = expressionForComparison();
        String otherExpression = dbObject.expressionForComparison();

        if (myExpression != null ? !myExpression.equals(otherExpression) : otherExpression != null) {
            return false;
        }
        return !(qualifier != null ? !qualifier.equals(dbObject.qualifier) : dbObject.qualifier != null);

    }

    @Override
    public int hashCode() {
        int result = alias != null ? alias.hashCode() : 0;
        String expression = expressionForComparison();
        result = 31 * result + (expression != null ? expression.hashCode() : 0);
        result = 31 * result + (qualifier != null ? qualifier.hashCode() : 0);
        return result;
    }

    /**
     * @return a string-literal representation of this object suitable for implementing equals() and hashCode(). Most
     * subclasses will not need to override this; only classes like {@link Function} or {@link Property} where
     * {@link #getExpression()} is implemented differently need to care about this.
     */
    protected String expressionForComparison() {
        return getExpression();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Expression=").append(expressionForComparison());
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
        StringBuilder builder = new StringBuilder();
        appendQualifiedExpressionToStringBuilder(builder);
        return builder.toString();
    }

    /**
     * Appends an expression used in a SELECT statement to represent this object
     *
     * @param builder the {@link SqlBuilder} to append to
     * @param forSqlValidation true if this statement is being compiled to validate against malicious SQL
     */
    @Override
    void appendToSqlBuilder(SqlBuilder builder, boolean forSqlValidation) {
        appendQualifiedExpression(builder, forSqlValidation);
        if (hasAlias()) {
            builder.sql.append(" AS ").append(alias);
        } else if (hasQualifier()) {
            builder.sql.append(" AS ").append(expression);
        }
    }

    protected void appendQualifiedExpression(SqlBuilder builder, boolean forSqlValidation) {
        appendQualifiedExpressionToStringBuilder(builder.sql);
    }

    private void appendQualifiedExpressionToStringBuilder(StringBuilder builder) {
        if (hasQualifier()) {
            builder.append(qualifier).append('.');
        }
        builder.append(getExpression());
    }
}
