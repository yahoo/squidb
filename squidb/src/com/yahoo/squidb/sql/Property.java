/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.sql;

/**
 * Property represents a typed column in a database.
 * <p>
 * Note the column affinities in SQLite do not align exactly to Java types and that SQLite can store any value in any
 * column despite the column's affinity. When a Property is used to read the value of a model (by
 * {@link com.yahoo.squidb.data.AbstractModel#get(Property) AbstractModel.get(Property)} or a cursor row
 * (by {@link com.yahoo.squidb.data.SquidCursor#get(Property) SquidCursor.get(Property)}), the following
 * possibilities can occur:
 * <ol>
 * <li>The column value is null, in which case {@code null} is returned</li>
 * <li>The column value is the correct type and will be returned as such</li>
 * <li>The column value is not the correct type. If it can be coerced to the correct type, it will be coerced and
 * returned. If it cannot be coerced, an exception is thrown.</li>
 * </ol>
 *
 * @param <TYPE> a database supported type, such as String or Integer
 */
public abstract class Property<TYPE> extends Field<TYPE> implements Cloneable {

    // --- implementation

    /** The database table this property represents a column of */
    public final SqlTable<?> table;

    /** Extras for column definition (e.g. "COLLATE NOCASE") */
    public final String columnDefinition;

    private Function<?> function = null;

    /**
     * Create a property by table and column name
     */
    protected Property(SqlTable<?> table, String columnName) {
        this(table, columnName, null, null);
    }

    /**
     * Create a property by table and column name. The additional column definition information will be used when
     * creating the table.
     */
    protected Property(SqlTable<?> table, String columnName, String columnDef) {
        this(table, columnName, null, columnDef);
    }

    /**
     * Create a property by table and column name and with the given alias. The additional column definition
     * information will be used when creating the table.
     */
    protected Property(SqlTable<?> table, String columnName, String alias, String columnDefinition) {
        super(columnName, table == null ? null : table.getName());
        this.table = table;
        this.alias = alias;
        this.columnDefinition = columnDefinition;
    }

    protected Property(Function<?> function, String alias) {
        this(null, null, alias, null);
        this.function = function;
    }

    /**
     * @return the additional column definition information
     */
    public String getColumnDefinition() {
        return columnDefinition;
    }

    @Override
    protected void appendQualifiedExpression(SqlBuilder builder, boolean forSqlValidation) {
        if (function != null) {
            function.appendToSqlBuilder(builder, forSqlValidation);
        } else {
            super.appendQualifiedExpression(builder, forSqlValidation);
        }
    }

    @Override
    public String getExpression() {
        if (function != null) {
            throw new UnsupportedOperationException("Can't call getExpression() on a Property that wraps a Function");
        }
        return super.getExpression();
    }

    @Override
    protected String expressionForComparison() {
        if (function != null) {
            return function.expressionForComparison();
        }
        return super.expressionForComparison();
    }

    /**
     * Accept a {@link PropertyVisitor}
     */
    public abstract <RETURN, PARAMETER> RETURN accept(PropertyVisitor<RETURN, PARAMETER> visitor, PARAMETER data);

    /**
     * Accept a {@link PropertyWritingVisitor}
     */
    public abstract <RETURN, DST, PARAMETER> RETURN accept(PropertyWritingVisitor<RETURN, DST, PARAMETER> visitor,
            DST dst, PARAMETER data);

    /**
     * Return a clone of this property
     */
    @SuppressWarnings("unchecked")
    @Override
    public Property<TYPE> clone() {
        try {
            return (Property<TYPE>) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Clone this property with the given alias. For example, when called on a property represented by
     * "table.property as property", returns a property represented by "table.property as newAlias".
     */
    @Override
    public Property<TYPE> as(String newAlias) {
        return (Property<TYPE>) super.as(newAlias);
    }

    /**
     * Clone this property with the given alias and qualified with the given table name. For example, when called on a
     * property represented by "table.property as property", returns a property represented by
     * "tableAlias.property as columnAlias".
     *
     * @param tableAlias the table name to use as the qualifier
     * @param columnAlias the new alias for the column
     * @return a clone of this property
     */
    public Property<TYPE> as(String tableAlias, String columnAlias) {
        if (SqlUtils.isEmpty(tableAlias) || function != null) {
            return as(columnAlias);
        }
        SqlTable<?> aliasedTable = table == null ? null : table.as(tableAlias);
        return as(aliasedTable, columnAlias);
    }

    /**
     * Clone this property with the given alias and qualified with the given table name. The original property
     * expression remains the same. For example, when called on a property represented by "table.property as property",
     * returns a property represented by "newTable.property as newAlias".
     *
     * @param newTable the {@link SqlTable table} to use as the qualifier. If this is a function property, this
     * argument is ignored.
     * @param newAlias the new alias for the column
     * @return a clone of this property
     */
    public Property<TYPE> as(SqlTable<?> newTable, String newAlias) {
        if (function != null) {
            return as(newAlias);
        }
        return cloneWithExpressionAndAlias(newTable, getExpression(), newAlias);
    }

    /**
     * Clone this property with the given alias and qualified with the given table name. The property expression is the
     * old alias if one exists, or the old expression otherwise. Examples:
     * <p>
     * When called on a property represented by "table.property", returns a property represented by
     * "newTable.property as newAlias".
     * <p>
     * When called on a property represented by "table.property as myProperty", returns a property represented by
     * "newTable.myProperty as newAlias".
     *
     * @param newTable the {@link SqlTable table} to use as the qualifier
     * @param newAlias the new alias for the column
     * @return a clone of this property
     */
    public Property<TYPE> asSelectionFromTable(SqlTable<?> newTable, String newAlias) {
        return cloneWithExpressionAndAlias(newTable, getName(), newAlias);
    }

    @SuppressWarnings("unchecked")
    private Property<TYPE> cloneWithExpressionAndAlias(SqlTable<?> table, String expression, String alias) {
        try {
            return getClass().getConstructor(SqlTable.class, String.class, String.class,
                    String.class).newInstance(table, expression, alias, null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // --- helper classes and interfaces

    /**
     * Visitor interface for property classes
     */
    public interface PropertyVisitor<RETURN, PARAMETER> {

        RETURN visitInteger(Property<Integer> property, PARAMETER data);

        RETURN visitLong(Property<Long> property, PARAMETER data);

        RETURN visitDouble(Property<Double> property, PARAMETER data);

        RETURN visitString(Property<String> property, PARAMETER data);

        RETURN visitBoolean(Property<Boolean> property, PARAMETER data);

        RETURN visitBlob(Property<byte[]> property, PARAMETER data);
    }

    /**
     * Visitor interface for writing some value into a destination object based on the property class
     */
    public interface PropertyWritingVisitor<RETURN, DST, PARAMETER> {

        RETURN visitInteger(Property<Integer> property, DST dst, PARAMETER data);

        RETURN visitLong(Property<Long> property, DST dst, PARAMETER data);

        RETURN visitDouble(Property<Double> property, DST dst, PARAMETER data);

        RETURN visitString(Property<String> property, DST dst, PARAMETER data);

        RETURN visitBoolean(Property<Boolean> property, DST dst, PARAMETER data);

        RETURN visitBlob(Property<byte[]> property, DST dst, PARAMETER data);
    }

    // --- children

    /**
     * Integer property type. See {@link Property}
     */
    public static class IntegerProperty extends Property<Integer> {

        public IntegerProperty(SqlTable<?> table, String name) {
            super(table, name);
        }

        public IntegerProperty(SqlTable<?> table, String name, String columnDefinition) {
            super(table, name, columnDefinition);
        }

        public IntegerProperty(SqlTable<?> table, String name, String alias, String columnDefinition) {
            super(table, name, alias, columnDefinition);
        }

        public IntegerProperty(Function<Integer> function, String alias) {
            super(function, alias);
        }

        /**
         * Construct an IntegerProperty from a {@link Function} and with the given alias, e.g. "LENGTH(column) AS len"
         *
         * @param function the function
         * @param selectAs the alias to use. May be null.
         */
        public static IntegerProperty fromFunction(Function<Integer> function, String selectAs) {
            return new IntegerProperty(function, selectAs);
        }

        /**
         * Construct an IntegerProperty from a literal value and with the given alias, e.g. "10 AS ten"
         *
         * @param literal the literal value
         * @param selectAs the alias to use. May be null.
         */
        public static IntegerProperty literal(int literal, String selectAs) {
            return new IntegerProperty(null, String.valueOf(literal), selectAs, null);
        }

        /**
         * Construct an IntegerProperty represented by the expression "COUNT(1)"
         */
        public static IntegerProperty countProperty() {
            return fromFunction(Function.count(), "count");
        }

        /**
         * Construct an IntegerProperty represented by the expression "COUNT(field)" or "COUNT(DISTINCT field)"
         *
         * @param field the field to count
         * @param distinct whether to count distinct values only or not
         */
        public static IntegerProperty countProperty(Field<?> field, boolean distinct) {
            Function<Integer> function = distinct ? Function.countDistinct(field) : Function.count(field);
            return fromFunction(function, "count");
        }

        @Override
        public <RETURN, PARAMETER> RETURN accept(PropertyVisitor<RETURN, PARAMETER> visitor, PARAMETER data) {
            return visitor.visitInteger(this, data);
        }

        @Override
        public <RETURN, DST, PARAMETER> RETURN accept(PropertyWritingVisitor<RETURN, DST, PARAMETER> visitor,
                DST dst, PARAMETER data) {
            return visitor.visitInteger(this, dst, data);
        }

        @Override
        public IntegerProperty as(String newAlias) {
            return (IntegerProperty) super.as(newAlias);
        }

        @Override
        public IntegerProperty as(String tableAlias, String columnAlias) {
            return (IntegerProperty) super.as(tableAlias, columnAlias);
        }

        @Override
        public IntegerProperty as(SqlTable<?> newTable, String columnAlias) {
            return (IntegerProperty) super.as(newTable, columnAlias);
        }

        @Override
        public IntegerProperty asSelectionFromTable(SqlTable<?> newTable, String columnAlias) {
            return (IntegerProperty) super.asSelectionFromTable(newTable, columnAlias);
        }
    }

    /**
     * String property type. See {@link Property}
     */
    public static class StringProperty extends Property<String> {

        public StringProperty(SqlTable<?> table, String name) {
            super(table, name);
        }

        public StringProperty(SqlTable<?> table, String name, String columnDefinition) {
            super(table, name, columnDefinition);
        }

        public StringProperty(SqlTable<?> table, String name, String alias, String columnDefinition) {
            super(table, name, alias, columnDefinition);
        }

        public StringProperty(Function<String> function, String alias) {
            super(function, alias);
        }

        /**
         * Construct a StringProperty from a {@link Function} and with the given alias, e.g.
         * "UPPER(column) AS uppercase"
         *
         * @param function the function
         * @param selectAs the alias to use. May be null.
         */
        public static StringProperty fromFunction(Function<String> function, String selectAs) {
            return new StringProperty(function, selectAs);
        }

        /**
         * Construct a StringProperty from a literal value and with the given alias, e.g. "'hello' AS greeting"
         *
         * @param literal the literal value
         * @param selectAs the alias to use. May be null.
         */
        public static StringProperty literal(String literal, String selectAs) {
            return new StringProperty(null, SqlUtils.sanitizeStringAsLiteral(literal), selectAs, null);
        }

        @Override
        public <RETURN, PARAMETER> RETURN accept(PropertyVisitor<RETURN, PARAMETER> visitor, PARAMETER data) {
            return visitor.visitString(this, data);
        }

        @Override
        public <RETURN, DST, PARAMETER> RETURN accept(PropertyWritingVisitor<RETURN, DST, PARAMETER> visitor,
                DST dst, PARAMETER data) {
            return visitor.visitString(this, dst, data);
        }

        @Override
        public StringProperty as(String newAlias) {
            return (StringProperty) super.as(newAlias);
        }

        @Override
        public StringProperty as(String tableAlias, String columnAlias) {
            return (StringProperty) super.as(tableAlias, columnAlias);
        }

        @Override
        public StringProperty as(SqlTable<?> newTable, String columnAlias) {
            return (StringProperty) super.as(newTable, columnAlias);
        }

        @Override
        public StringProperty asSelectionFromTable(SqlTable<?> newTable, String columnAlias) {
            return (StringProperty) super.asSelectionFromTable(newTable, columnAlias);
        }

        /**
         * @return a {@link Criterion} that the property is null or the empty string ''
         */
        public Criterion isEmpty() {
            return isNull().or(eq(""));
        }

        /**
         * @return a {@link Criterion} that the property is not null and not the empty string ''
         */
        public Criterion isNotEmpty() {
            return isNotNull().and(neq(""));
        }

        /**
         * @return a {@link Criterion} that the property matches the full-text query expression. This is only useful for
         * queries on virtual tables using fts3 or fts4.
         * @see <a href="http://www.sqlite.org/fts3.html#section_3">http://www.sqlite.org/fts3.html#section_3</a>
         */
        public Criterion match(String value) {
            return new BinaryCriterion(this, Operator.match, value);
        }
    }

    /**
     * Double property type. See {@link Property}
     */
    public static class DoubleProperty extends Property<Double> {

        public DoubleProperty(SqlTable<?> table, String name) {
            super(table, name);
        }

        public DoubleProperty(SqlTable<?> table, String name, String columnDefinition) {
            super(table, name, columnDefinition);
        }

        public DoubleProperty(SqlTable<?> table, String name, String alias, String columnDefinition) {
            super(table, name, alias, columnDefinition);
        }

        public DoubleProperty(Function<Double> function, String selectAs) {
            super(function, selectAs);
        }

        /**
         * Construct a DoubleProperty from a {@link Function} and with the given alias, e.g. "AVG(column) AS average"
         *
         * @param function the function
         * @param selectAs the alias to use. May be null.
         */
        public static DoubleProperty fromFunction(Function<Double> function, String selectAs) {
            return new DoubleProperty(function, selectAs);
        }

        /**
         * Construct a StringProperty from a literal value and with the given alias, e.g. "3.14 AS pi"
         *
         * @param literal the literal value
         * @param selectAs the alias to use. May be null.
         */
        public static DoubleProperty literal(double literal, String selectAs) {
            return new DoubleProperty(null, String.valueOf(literal), selectAs, null);
        }

        @Override
        public <RETURN, PARAMETER> RETURN accept(PropertyVisitor<RETURN, PARAMETER> visitor, PARAMETER data) {
            return visitor.visitDouble(this, data);
        }

        @Override
        public <RETURN, DST, PARAMETER> RETURN accept(PropertyWritingVisitor<RETURN, DST, PARAMETER> visitor,
                DST dst, PARAMETER data) {
            return visitor.visitDouble(this, dst, data);
        }

        @Override
        public DoubleProperty as(String columnAlias) {
            return (DoubleProperty) super.as(columnAlias);
        }

        @Override
        public DoubleProperty as(String tableAlias, String columnAlias) {
            return (DoubleProperty) super.as(tableAlias, columnAlias);
        }

        @Override
        public DoubleProperty as(SqlTable<?> newTable, String columnAlias) {
            return (DoubleProperty) super.as(newTable, columnAlias);
        }

        @Override
        public DoubleProperty asSelectionFromTable(SqlTable<?> newTable, String columnAlias) {
            return (DoubleProperty) super.asSelectionFromTable(newTable, columnAlias);
        }
    }

    /**
     * Long property type. See {@link Property}
     */
    public static class LongProperty extends Property<Long> {

        public LongProperty(SqlTable<?> table, String name) {
            super(table, name);
        }

        public LongProperty(SqlTable<?> table, String name, String columnDefinition) {
            super(table, name, columnDefinition);
        }

        public LongProperty(SqlTable<?> table, String name, String alias, String columnDefinition) {
            super(table, name, alias, columnDefinition);
        }

        public LongProperty(Function<Long> function, String selectAs) {
            super(function, selectAs);
        }

        /**
         * Construct a LongProperty from a {@link Function} and with the given alias, e.g. "SUM(column) AS sum"
         *
         * @param function the function
         * @param selectAs the alias to use. May be null.
         */
        public static LongProperty fromFunction(Function<Long> function, String selectAs) {
            return new LongProperty(function, selectAs);
        }

        /**
         * Construct a LongProperty from a literal value and with the given alias, e.g. "1000000000000 AS trillion"
         *
         * @param literal the literal value
         * @param selectAs the alias to use. May be null.
         */
        public static LongProperty literal(long literal, String selectAs) {
            return new LongProperty(null, String.valueOf(literal), selectAs, null);
        }

        @Override
        public <RETURN, PARAMETER> RETURN accept(PropertyVisitor<RETURN, PARAMETER> visitor, PARAMETER data) {
            return visitor.visitLong(this, data);
        }

        @Override
        public <RETURN, DST, PARAMETER> RETURN accept(PropertyWritingVisitor<RETURN, DST, PARAMETER> visitor,
                DST dst, PARAMETER data) {
            return visitor.visitLong(this, dst, data);
        }

        @Override
        public LongProperty as(String newAlias) {
            return (LongProperty) super.as(newAlias);
        }

        @Override
        public LongProperty as(String tableAlias, String columnAlias) {
            return (LongProperty) super.as(tableAlias, columnAlias);
        }

        @Override
        public LongProperty as(SqlTable<?> newTable, String columnAlias) {
            return (LongProperty) super.as(newTable, columnAlias);
        }

        @Override
        public LongProperty asSelectionFromTable(SqlTable<?> newTable, String columnAlias) {
            return (LongProperty) super.asSelectionFromTable(newTable, columnAlias);
        }
    }

    /**
     * Boolean property type. See {@link Property}
     */
    public static class BooleanProperty extends Property<Boolean> {

        public BooleanProperty(SqlTable<?> table, String name) {
            super(table, name);
        }

        public BooleanProperty(SqlTable<?> table, String name, String columnDefinition) {
            super(table, name, columnDefinition);
        }

        public BooleanProperty(SqlTable<?> table, String name, String alias, String columnDefinition) {
            super(table, name, alias, columnDefinition);
        }

        public BooleanProperty(Function<Integer> function, String selectAs) {
            super(function, selectAs);
        }

        /**
         * Construct a BooleanProperty from a {@link Function} and with the given alias,
         * e.g. "CASE WHEN column &gt; 10 THEN 1 ELSE 0 END AS moreThanTen"
         *
         * @param function the function
         * @param selectAs the alias to use. May be null.
         */
        public static BooleanProperty fromFunction(Function<Integer> function, String selectAs) {
            return new BooleanProperty(function, selectAs);
        }

        /**
         * Construct a BooleanProperty from a literal value and with the given alias, e.g. "1 AS true"
         *
         * @param literal the literal value
         * @param selectAs the alias to use. May be null.
         */
        public static BooleanProperty literal(boolean literal, String selectAs) {
            return new BooleanProperty(null, String.valueOf(literal ? 1 : 0), selectAs, null);
        }

        @Override
        public <RETURN, PARAMETER> RETURN accept(PropertyVisitor<RETURN, PARAMETER> visitor, PARAMETER data) {
            return visitor.visitBoolean(this, data);
        }

        @Override
        public <RETURN, DST, PARAMETER> RETURN accept(PropertyWritingVisitor<RETURN, DST, PARAMETER> visitor,
                DST dst, PARAMETER data) {
            return visitor.visitBoolean(this, dst, data);
        }

        @Override
        public BooleanProperty as(String newAlias) {
            return (BooleanProperty) super.as(newAlias);
        }

        @Override
        public BooleanProperty as(String tableAlias, String columnAlias) {
            return (BooleanProperty) super.as(tableAlias, columnAlias);
        }

        @Override
        public BooleanProperty as(SqlTable<?> newTable, String columnAlias) {
            return (BooleanProperty) super.as(newTable, columnAlias);
        }

        @Override
        public BooleanProperty asSelectionFromTable(SqlTable<?> newTable, String columnAlias) {
            return (BooleanProperty) super.asSelectionFromTable(newTable, columnAlias);
        }

        public Criterion isTrue() {
            return neq(Function.FALSE);
        }

        public Criterion isFalse() {
            return eq(Function.FALSE);
        }

        public Criterion eq(boolean b) {
            return b ? isTrue() : isFalse();
        }

        public Criterion neq(boolean b) {
            return b ? isFalse() : isTrue();
        }
    }

    /**
     * Binary blob property type. See {@link Property}
     */
    public static class BlobProperty extends Property<byte[]> {

        public BlobProperty(SqlTable<?> table, String name) {
            super(table, name);
        }

        public BlobProperty(SqlTable<?> table, String name, String columnDefinition) {
            super(table, name, columnDefinition);
        }

        public BlobProperty(SqlTable<?> table, String name, String alias, String columnDefinition) {
            super(table, name, alias, columnDefinition);
        }

        @Override
        public <RETURN, PARAMETER> RETURN accept(PropertyVisitor<RETURN, PARAMETER> visitor, PARAMETER data) {
            return visitor.visitBlob(this, data);
        }

        @Override
        public <RETURN, DST, PARAMETER> RETURN accept(PropertyWritingVisitor<RETURN, DST, PARAMETER> visitor,
                DST dst, PARAMETER data) {
            return visitor.visitBlob(this, dst, data);
        }

        @Override
        public BlobProperty as(String newAlias) {
            return (BlobProperty) super.as(newAlias);
        }

        @Override
        public BlobProperty as(String tableAlias, String columnAlias) {
            return (BlobProperty) super.as(tableAlias, columnAlias);
        }

        @Override
        public BlobProperty as(SqlTable<?> newTable, String columnAlias) {
            return (BlobProperty) super.as(newTable, columnAlias);
        }

        @Override
        public BlobProperty asSelectionFromTable(SqlTable<?> newTable, String columnAlias) {
            return (BlobProperty) super.asSelectionFromTable(newTable, columnAlias);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(super.toString());
        String tableName = table == null ? "null" : table.getExpression();
        sb.append(" Table=").append(tableName).append(" ColumnDefinition=")
                .append(columnDefinition);
        return sb.toString();
    }
}
