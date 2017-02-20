/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.sql;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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

    private static final TableModelName FUNCTION_TABLE_MODEL_NAME = new TableModelName(null, "");

    /** The database table this property represents a column of */
    @Nonnull
    public final TableModelName tableModelName;

    /** Extras for column definition (e.g. "COLLATE NOCASE") */
    @Nullable
    public final String columnDefinition;

    private Function<?> function = null;

    /**
     * Create a property by table and column name
     */
    protected Property(@Nonnull TableModelName tableModelName, @Nonnull String columnName) {
        this(tableModelName, columnName, null, null);
    }

    /**
     * Create a property by table and column name. The additional column definition information will be used when
     * creating the table.
     */
    protected Property(@Nonnull TableModelName tableModelName, @Nonnull String columnName,
            @Nullable String columnDef) {
        this(tableModelName, columnName, null, columnDef);
    }

    /**
     * Create a property by table and column name and with the given alias. The additional column definition
     * information will be used when creating the table.
     */
    protected Property(@Nonnull TableModelName tableModelName, @Nonnull String columnName,
            @Nullable String alias, @Nullable String columnDefinition) {
        super(columnName, tableModelName.tableName);
        this.tableModelName = tableModelName;
        this.alias = alias;
        this.columnDefinition = columnDefinition;
    }

    protected Property(@Nonnull Function<?> function, @Nonnull String alias) {
        this(FUNCTION_TABLE_MODEL_NAME, "", alias, null);
        this.function = function;
    }

    /**
     * @return the additional column definition information
     */
    @Nullable
    public String getColumnDefinition() {
        return columnDefinition;
    }

    public boolean isPrimaryKey() {
        return columnDefinition != null && columnDefinition.contains("PRIMARY KEY");
    }

    @Override
    protected void appendQualifiedExpression(@Nonnull SqlBuilder builder, boolean forSqlValidation) {
        if (function != null) {
            function.appendToSqlBuilder(builder, forSqlValidation);
        } else {
            super.appendQualifiedExpression(builder, forSqlValidation);
        }
    }

    @Override
    @Nonnull
    public String getExpression() {
        if (function != null) {
            throw new UnsupportedOperationException("Can't call getExpression() on a Property that wraps a Function");
        }
        return super.getExpression();
    }

    @Override
    @Nonnull
    protected String expressionForComparison() {
        if (function != null) {
            return function.expressionForComparison();
        }
        return super.expressionForComparison();
    }

    /**
     * Accept a {@link PropertyVisitor}
     */
    public abstract <RETURN, PARAMETER> RETURN accept(@Nonnull PropertyVisitor<RETURN, PARAMETER> visitor, PARAMETER data);

    /**
     * Accept a {@link PropertyWritingVisitor}
     */
    public abstract <RETURN, DST, PARAMETER> RETURN accept(@Nonnull PropertyWritingVisitor<RETURN, DST, PARAMETER> visitor,
            DST dst, PARAMETER data);

    /**
     * Return a clone of this property
     */
    @SuppressWarnings("unchecked")
    @Override
    @Nonnull
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
    @Nonnull
    public Property<TYPE> as(@Nonnull String newAlias) {
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
    @Nonnull
    public Property<TYPE> as(@Nonnull String tableAlias, @Nonnull String columnAlias) {
        if (SqlUtils.isEmpty(tableAlias) || function != null) {
            return as(columnAlias);
        }
        return cloneWithExpressionAndAlias(new TableModelName(tableModelName.modelClass, tableAlias),
                getExpression(), columnAlias);
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
    @Nonnull
    public Property<TYPE> as(@Nonnull SqlTable<?> newTable, @Nonnull String newAlias) {
        if (newTable == null || SqlUtils.isEmpty(newTable.getName()) || function != null) {
            return as(newAlias);
        }
        return cloneWithExpressionAndAlias(new TableModelName(newTable.getModelClass(), newTable.getName()),
                getExpression(), newAlias);
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
    @Nonnull
    public Property<TYPE> asSelectionFromTable(@Nonnull SqlTable<?> newTable, @Nullable String newAlias) {
        TableModelName newTableModelName = new TableModelName(newTable.getModelClass(), newTable.getName());
        return cloneWithExpressionAndAlias(newTableModelName, getName(), newAlias);
    }

    @SuppressWarnings("unchecked")
    @Nonnull
    private Property<TYPE> cloneWithExpressionAndAlias(@Nonnull TableModelName tableModelName,
            @Nonnull String expression, @Nullable String alias) {
        try {
            return getClass().getConstructor(TableModelName.class, String.class, String.class,
                    String.class).newInstance(tableModelName, expression, alias, null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // --- helper classes and interfaces

    /**
     * Visitor interface for property classes
     */
    public interface PropertyVisitor<RETURN, PARAMETER> {

        RETURN visitInteger(@Nonnull Property<Integer> property, PARAMETER data);

        RETURN visitLong(@Nonnull Property<Long> property, PARAMETER data);

        RETURN visitDouble(@Nonnull Property<Double> property, PARAMETER data);

        RETURN visitString(@Nonnull Property<String> property, PARAMETER data);

        RETURN visitBoolean(@Nonnull Property<Boolean> property, PARAMETER data);

        RETURN visitBlob(@Nonnull Property<byte[]> property, PARAMETER data);
    }

    /**
     * Visitor interface for writing some value into a destination object based on the property class
     */
    public interface PropertyWritingVisitor<RETURN, DST, PARAMETER> {

        RETURN visitInteger(@Nonnull Property<Integer> property, DST dst, PARAMETER data);

        RETURN visitLong(@Nonnull Property<Long> property, DST dst, PARAMETER data);

        RETURN visitDouble(@Nonnull Property<Double> property, DST dst, PARAMETER data);

        RETURN visitString(@Nonnull Property<String> property, DST dst, PARAMETER data);

        RETURN visitBoolean(@Nonnull Property<Boolean> property, DST dst, PARAMETER data);

        RETURN visitBlob(@Nonnull Property<byte[]> property, DST dst, PARAMETER data);
    }

    // --- children

    /**
     * Integer property type. See {@link Property}
     */
    public static class IntegerProperty extends Property<Integer> {

        public IntegerProperty(@Nonnull TableModelName tableModelName, @Nonnull String name) {
            super(tableModelName, name);
        }

        public IntegerProperty(@Nonnull TableModelName tableModelName, @Nonnull String name,
                @Nullable String columnDefinition) {
            super(tableModelName, name, columnDefinition);
        }

        public IntegerProperty(@Nonnull TableModelName tableModelName, @Nonnull String name,
                @Nullable String alias, @Nullable String columnDefinition) {
            super(tableModelName, name, alias, columnDefinition);
        }

        public IntegerProperty(@Nonnull Function<Integer> function, @Nonnull String alias) {
            super(function, alias);
        }

        /**
         * Construct an IntegerProperty from a {@link Function} and with the given alias, e.g. "LENGTH(column) AS len"
         *
         * @param function the function
         * @param selectAs the alias to use. May be null.
         */
        @Nonnull
        public static IntegerProperty fromFunction(@Nonnull Function<Integer> function, @Nonnull String selectAs) {
            return new IntegerProperty(function, selectAs);
        }

        /**
         * Construct an IntegerProperty from a literal value and with the given alias, e.g. "10 AS ten"
         *
         * @param literal the literal value
         * @param selectAs the alias to use. May be null.
         */
        @Nonnull
        public static IntegerProperty literal(int literal, @Nonnull String selectAs) {
            return fromFunction(Function.<Integer>rawFunction(String.valueOf(literal)), selectAs);
        }

        /**
         * Construct an IntegerProperty represented by the expression "COUNT(1)"
         */
        @Nonnull
        public static IntegerProperty countProperty() {
            return fromFunction(Function.count(), "count");
        }

        /**
         * Construct an IntegerProperty represented by the expression "COUNT(field)" or "COUNT(DISTINCT field)"
         *
         * @param field the field to count
         * @param distinct whether to count distinct values only or not
         */
        @Nonnull
        public static IntegerProperty countProperty(@Nonnull Field<?> field, boolean distinct) {
            Function<Integer> function = distinct ? Function.countDistinct(field) : Function.count(field);
            return fromFunction(function, "count");
        }

        @Override
        public <RETURN, PARAMETER> RETURN accept(@Nonnull PropertyVisitor<RETURN, PARAMETER> visitor, PARAMETER data) {
            return visitor.visitInteger(this, data);
        }

        @Override
        public <RETURN, DST, PARAMETER> RETURN accept(@Nonnull PropertyWritingVisitor<RETURN, DST, PARAMETER> visitor,
                DST dst, PARAMETER data) {
            return visitor.visitInteger(this, dst, data);
        }

        @Override
        @Nonnull
        public IntegerProperty as(@Nonnull String newAlias) {
            return (IntegerProperty) super.as(newAlias);
        }

        @Override
        @Nonnull
        public IntegerProperty as(@Nonnull String tableAlias, @Nonnull String columnAlias) {
            return (IntegerProperty) super.as(tableAlias, columnAlias);
        }

        @Override
        @Nonnull
        public IntegerProperty as(@Nonnull SqlTable<?> newTable, @Nonnull String columnAlias) {
            return (IntegerProperty) super.as(newTable, columnAlias);
        }

        @Override
        @Nonnull
        public IntegerProperty asSelectionFromTable(@Nonnull SqlTable<?> newTable, @Nullable String columnAlias) {
            return (IntegerProperty) super.asSelectionFromTable(newTable, columnAlias);
        }
    }

    /**
     * String property type. See {@link Property}
     */
    public static class StringProperty extends Property<String> {

        public StringProperty(@Nonnull TableModelName tableModelName, @Nonnull String name) {
            super(tableModelName, name);
        }

        public StringProperty(@Nonnull TableModelName tableModelName, @Nonnull String name,
                @Nullable String columnDefinition) {
            super(tableModelName, name, columnDefinition);
        }

        public StringProperty(@Nonnull TableModelName tableModelName, @Nonnull String name, @Nullable String alias,
                @Nullable String columnDefinition) {
            super(tableModelName, name, alias, columnDefinition);
        }

        public StringProperty(@Nonnull Function<String> function, @Nonnull String alias) {
            super(function, alias);
        }

        /**
         * Construct a StringProperty from a {@link Function} and with the given alias, e.g.
         * "UPPER(column) AS uppercase"
         *
         * @param function the function
         * @param selectAs the alias to use. May be null.
         */
        @Nonnull
        public static StringProperty fromFunction(@Nonnull Function<String> function, @Nonnull String selectAs) {
            return new StringProperty(function, selectAs);
        }

        /**
         * Construct a StringProperty from a literal value and with the given alias, e.g. "'hello' AS greeting"
         *
         * @param literal the literal value
         * @param selectAs the alias to use. May be null.
         */
        @Nonnull
        public static StringProperty literal(@Nullable String literal, @Nonnull String selectAs) {
            return fromFunction(Function.<String>rawFunction(SqlUtils.sanitizeStringAsLiteral(literal)), selectAs);
        }

        @Override
        public <RETURN, PARAMETER> RETURN accept(@Nonnull PropertyVisitor<RETURN, PARAMETER> visitor, PARAMETER data) {
            return visitor.visitString(this, data);
        }

        @Override
        public <RETURN, DST, PARAMETER> RETURN accept(@Nonnull PropertyWritingVisitor<RETURN, DST, PARAMETER> visitor,
                DST dst, PARAMETER data) {
            return visitor.visitString(this, dst, data);
        }

        @Override
        @Nonnull
        public StringProperty as(@Nonnull String newAlias) {
            return (StringProperty) super.as(newAlias);
        }

        @Override
        @Nonnull
        public StringProperty as(@Nonnull String tableAlias, @Nonnull String columnAlias) {
            return (StringProperty) super.as(tableAlias, columnAlias);
        }

        @Override
        @Nonnull
        public StringProperty as(@Nonnull SqlTable<?> newTable, @Nonnull String columnAlias) {
            return (StringProperty) super.as(newTable, columnAlias);
        }

        @Override
        @Nonnull
        public StringProperty asSelectionFromTable(@Nonnull SqlTable<?> newTable, @Nullable String columnAlias) {
            return (StringProperty) super.asSelectionFromTable(newTable, columnAlias);
        }

        /**
         * @return a {@link Criterion} that the property is null or the empty string ''
         */
        @Nonnull
        public Criterion isEmpty() {
            return isNull().or(eq(""));
        }

        /**
         * @return a {@link Criterion} that the property is not null and not the empty string ''
         */
        @Nonnull
        public Criterion isNotEmpty() {
            return isNotNull().and(neq(""));
        }

        /**
         * @return a {@link Criterion} that the property matches the full-text query expression. This is only useful for
         * queries on virtual tables using fts3 or fts4.
         * @see <a href="http://www.sqlite.org/fts3.html#section_3">http://www.sqlite.org/fts3.html#section_3</a>
         */
        @Nonnull
        public Criterion match(@Nullable String value) {
            return new BinaryCriterion(this, Operator.match, value);
        }
    }

    /**
     * Double property type. See {@link Property}
     */
    public static class DoubleProperty extends Property<Double> {

        public DoubleProperty(@Nonnull TableModelName tableModelName, @Nonnull String name) {
            super(tableModelName, name);
        }

        public DoubleProperty(@Nonnull TableModelName tableModelName, @Nonnull String name,
                @Nullable String columnDefinition) {
            super(tableModelName, name, columnDefinition);
        }

        public DoubleProperty(@Nonnull TableModelName tableModelName, @Nonnull String name, @Nullable String alias,
                @Nullable String columnDefinition) {
            super(tableModelName, name, alias, columnDefinition);
        }

        public DoubleProperty(@Nonnull Function<Double> function, @Nonnull String selectAs) {
            super(function, selectAs);
        }

        /**
         * Construct a DoubleProperty from a {@link Function} and with the given alias, e.g. "AVG(column) AS average"
         *
         * @param function the function
         * @param selectAs the alias to use. May be null.
         */
        @Nonnull
        public static DoubleProperty fromFunction(@Nonnull Function<Double> function, @Nonnull String selectAs) {
            return new DoubleProperty(function, selectAs);
        }

        /**
         * Construct a StringProperty from a literal value and with the given alias, e.g. "3.14 AS pi"
         *
         * @param literal the literal value
         * @param selectAs the alias to use. May be null.
         */
        @Nonnull
        public static DoubleProperty literal(double literal, @Nonnull String selectAs) {
            return fromFunction(Function.<Double>rawFunction(String.valueOf(literal)), selectAs);
        }

        @Override
        public <RETURN, PARAMETER> RETURN accept(@Nonnull PropertyVisitor<RETURN, PARAMETER> visitor, PARAMETER data) {
            return visitor.visitDouble(this, data);
        }

        @Override
        public <RETURN, DST, PARAMETER> RETURN accept(@Nonnull PropertyWritingVisitor<RETURN, DST, PARAMETER> visitor,
                DST dst, PARAMETER data) {
            return visitor.visitDouble(this, dst, data);
        }

        @Override
        @Nonnull
        public DoubleProperty as(@Nonnull String columnAlias) {
            return (DoubleProperty) super.as(columnAlias);
        }

        @Override
        @Nonnull
        public DoubleProperty as(@Nonnull String tableAlias, @Nonnull String columnAlias) {
            return (DoubleProperty) super.as(tableAlias, columnAlias);
        }

        @Override
        @Nonnull
        public DoubleProperty as(@Nonnull SqlTable<?> newTable, @Nonnull String columnAlias) {
            return (DoubleProperty) super.as(newTable, columnAlias);
        }

        @Override
        @Nonnull
        public DoubleProperty asSelectionFromTable(@Nonnull SqlTable<?> newTable, @Nullable String columnAlias) {
            return (DoubleProperty) super.asSelectionFromTable(newTable, columnAlias);
        }
    }

    /**
     * Long property type. See {@link Property}
     */
    public static class LongProperty extends Property<Long> {

        public LongProperty(@Nonnull TableModelName tableModelName, @Nonnull String name) {
            super(tableModelName, name);
        }

        public LongProperty(@Nonnull TableModelName tableModelName, @Nonnull String name,
                @Nullable String columnDefinition) {
            super(tableModelName, name, columnDefinition);
        }

        public LongProperty(@Nonnull TableModelName tableModelName, @Nonnull String name, @Nullable String alias,
                @Nullable String columnDefinition) {
            super(tableModelName, name, alias, columnDefinition);
        }

        public LongProperty(@Nonnull Function<Long> function, @Nonnull String selectAs) {
            super(function, selectAs);
        }

        /**
         * Construct a LongProperty from a {@link Function} and with the given alias, e.g. "SUM(column) AS sum"
         *
         * @param function the function
         * @param selectAs the alias to use. May be null.
         */
        @Nonnull
        public static LongProperty fromFunction(@Nonnull Function<Long> function, @Nonnull String selectAs) {
            return new LongProperty(function, selectAs);
        }

        /**
         * Construct a LongProperty from a literal value and with the given alias, e.g. "1000000000000 AS trillion"
         *
         * @param literal the literal value
         * @param selectAs the alias to use. May be null.
         */
        @Nonnull
        public static LongProperty literal(long literal, @Nonnull String selectAs) {
            return fromFunction(Function.<Long>rawFunction(String.valueOf(literal)), selectAs);
        }

        @Override
        public <RETURN, PARAMETER> RETURN accept(@Nonnull PropertyVisitor<RETURN, PARAMETER> visitor, PARAMETER data) {
            return visitor.visitLong(this, data);
        }

        @Override
        public <RETURN, DST, PARAMETER> RETURN accept(@Nonnull PropertyWritingVisitor<RETURN, DST, PARAMETER> visitor,
                DST dst, PARAMETER data) {
            return visitor.visitLong(this, dst, data);
        }

        @Override
        @Nonnull
        public LongProperty as(@Nonnull String newAlias) {
            return (LongProperty) super.as(newAlias);
        }

        @Override
        @Nonnull
        public LongProperty as(@Nonnull String tableAlias, @Nonnull String columnAlias) {
            return (LongProperty) super.as(tableAlias, columnAlias);
        }

        @Override
        @Nonnull
        public LongProperty as(@Nonnull SqlTable<?> newTable, @Nonnull String columnAlias) {
            return (LongProperty) super.as(newTable, columnAlias);
        }

        @Override
        @Nonnull
        public LongProperty asSelectionFromTable(@Nonnull SqlTable<?> newTable, @Nullable String columnAlias) {
            return (LongProperty) super.asSelectionFromTable(newTable, columnAlias);
        }
    }

    /**
     * Boolean property type. See {@link Property}
     */
    public static class BooleanProperty extends Property<Boolean> {

        public BooleanProperty(@Nonnull TableModelName tableModelName, @Nonnull String name) {
            super(tableModelName, name);
        }

        public BooleanProperty(@Nonnull TableModelName tableModelName, @Nonnull String name,
                @Nullable String columnDefinition) {
            super(tableModelName, name, columnDefinition);
        }

        public BooleanProperty(@Nonnull TableModelName tableModelName, @Nonnull String name, @Nullable String alias,
                @Nullable String columnDefinition) {
            super(tableModelName, name, alias, columnDefinition);
        }

        public BooleanProperty(@Nonnull Function<Integer> function, @Nonnull String selectAs) {
            super(function, selectAs);
        }

        /**
         * Construct a BooleanProperty from a {@link Function} and with the given alias,
         * e.g. "CASE WHEN column &gt; 10 THEN 1 ELSE 0 END AS moreThanTen"
         *
         * @param function the function
         * @param selectAs the alias to use. May be null.
         */
        @Nonnull
        public static BooleanProperty fromFunction(@Nonnull Function<Integer> function, @Nonnull String selectAs) {
            return new BooleanProperty(function, selectAs);
        }

        /**
         * Construct a BooleanProperty from a literal value and with the given alias, e.g. "1 AS true"
         *
         * @param literal the literal value
         * @param selectAs the alias to use. May be null.
         */
        @Nonnull
        public static BooleanProperty literal(boolean literal, @Nonnull String selectAs) {
            return fromFunction(Function.<Integer>rawFunction(String.valueOf(literal ? 1 : 0)), selectAs);
        }

        @Override
        public <RETURN, PARAMETER> RETURN accept(@Nonnull PropertyVisitor<RETURN, PARAMETER> visitor, PARAMETER data) {
            return visitor.visitBoolean(this, data);
        }

        @Override
        public <RETURN, DST, PARAMETER> RETURN accept(@Nonnull PropertyWritingVisitor<RETURN, DST, PARAMETER> visitor,
                DST dst, PARAMETER data) {
            return visitor.visitBoolean(this, dst, data);
        }

        @Override
        @Nonnull
        public BooleanProperty as(@Nonnull String newAlias) {
            return (BooleanProperty) super.as(newAlias);
        }

        @Override
        @Nonnull
        public BooleanProperty as(@Nonnull String tableAlias, @Nonnull String columnAlias) {
            return (BooleanProperty) super.as(tableAlias, columnAlias);
        }

        @Override
        @Nonnull
        public BooleanProperty as(@Nonnull SqlTable<?> newTable, @Nonnull String columnAlias) {
            return (BooleanProperty) super.as(newTable, columnAlias);
        }

        @Override
        @Nonnull
        public BooleanProperty asSelectionFromTable(@Nonnull SqlTable<?> newTable, @Nullable String columnAlias) {
            return (BooleanProperty) super.asSelectionFromTable(newTable, columnAlias);
        }

        @Nonnull
        public Criterion isTrue() {
            return neq(Function.FALSE);
        }

        @Nonnull
        public Criterion isFalse() {
            return eq(Function.FALSE);
        }

        @Nonnull
        public Criterion eq(boolean b) {
            return b ? isTrue() : isFalse();
        }

        @Nonnull
        public Criterion neq(boolean b) {
            return b ? isFalse() : isTrue();
        }
    }

    /**
     * Binary blob property type. See {@link Property}
     */
    public static class BlobProperty extends Property<byte[]> {

        public BlobProperty(@Nonnull TableModelName tableModelName, @Nonnull String name) {
            super(tableModelName, name);
        }

        public BlobProperty(@Nonnull TableModelName tableModelName, @Nonnull String name,
                @Nullable String columnDefinition) {
            super(tableModelName, name, columnDefinition);
        }

        public BlobProperty(@Nonnull TableModelName tableModelName, @Nonnull String name, @Nullable String alias,
                @Nullable String columnDefinition) {
            super(tableModelName, name, alias, columnDefinition);
        }

        @Override
        public <RETURN, PARAMETER> RETURN accept(@Nonnull PropertyVisitor<RETURN, PARAMETER> visitor, PARAMETER data) {
            return visitor.visitBlob(this, data);
        }

        @Override
        public <RETURN, DST, PARAMETER> RETURN accept(@Nonnull PropertyWritingVisitor<RETURN, DST, PARAMETER> visitor,
                DST dst, PARAMETER data) {
            return visitor.visitBlob(this, dst, data);
        }

        @Override
        @Nonnull
        public BlobProperty as(@Nonnull String newAlias) {
            return (BlobProperty) super.as(newAlias);
        }

        @Override
        @Nonnull
        public BlobProperty as(@Nonnull String tableAlias, @Nonnull String columnAlias) {
            return (BlobProperty) super.as(tableAlias, columnAlias);
        }

        @Override
        @Nonnull
        public BlobProperty as(@Nonnull SqlTable<?> newTable, @Nonnull String columnAlias) {
            return (BlobProperty) super.as(newTable, columnAlias);
        }

        @Override
        @Nonnull
        public BlobProperty asSelectionFromTable(@Nonnull SqlTable<?> newTable, @Nullable String columnAlias) {
            return (BlobProperty) super.asSelectionFromTable(newTable, columnAlias);
        }
    }

    /**
     * Extension of {@link StringProperty} meant for storing enum values. Uses a type parameter so that the enum type
     * being serialized is known at compile time.
     *
     * @param <T> an enum type for this property to hold
     */
    public static class EnumProperty<T extends Enum<T>> extends StringProperty {

        public EnumProperty(@Nonnull TableModelName tableModelName, @Nonnull String name) {
            super(tableModelName, name);
        }

        public EnumProperty(@Nonnull TableModelName tableModelName, @Nonnull String name,
                @Nullable String columnDefinition) {
            super(tableModelName, name, columnDefinition);
        }

        public EnumProperty(@Nonnull TableModelName tableModelName, @Nonnull String name, @Nullable String alias,
                @Nullable String columnDefinition) {
            super(tableModelName, name, alias, columnDefinition);
        }

        public EnumProperty(@Nonnull Function<String> function, @Nonnull String alias) {
            super(function, alias);
        }

        /**
         * Construct an EnumProperty from a literal enum value and with the given alias, e.g. "'ENUM_VAL_1' AS
         * greeting"
         *
         * @param literal the literal value
         * @param selectAs the alias to use. May be null.
         */
        @Nonnull
        public static <T extends Enum<T>> EnumProperty<T> literal(@Nullable T literal, @Nonnull String selectAs) {
            String enumString = literal == null ? "null" : SqlUtils.sanitizeStringAsLiteral(literal.name());
            return new EnumProperty<>(Function.<String>rawFunction(enumString), selectAs);
        }

        @Override
        @SuppressWarnings("unchecked")
        @Nonnull
        public EnumProperty<T> as(@Nonnull String newAlias) {
            return (EnumProperty<T>) super.as(newAlias);
        }

        @Override
        @SuppressWarnings("unchecked")
        @Nonnull
        public EnumProperty<T> as(@Nonnull String tableAlias, @Nonnull String columnAlias) {
            return (EnumProperty<T>) super.as(tableAlias, columnAlias);
        }

        @Override
        @SuppressWarnings("unchecked")
        @Nonnull
        public EnumProperty<T> as(@Nonnull SqlTable<?> newTable, @Nonnull String columnAlias) {
            return (EnumProperty<T>) super.as(newTable, columnAlias);
        }

        @Override
        @SuppressWarnings("unchecked")
        @Nonnull
        public EnumProperty<T> asSelectionFromTable(@Nonnull SqlTable<?> newTable, @Nullable String columnAlias) {
            return (EnumProperty<T>) super.asSelectionFromTable(newTable, columnAlias);
        }
    }

    @Override
    @Nonnull
    public String toString() {
        return super.toString() + " Table=" + tableModelName.tableName + " ColumnDefinition=" + columnDefinition;
    }
}
