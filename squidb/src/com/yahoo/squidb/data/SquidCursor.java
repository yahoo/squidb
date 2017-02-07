/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.data;

import com.yahoo.squidb.sql.Field;
import com.yahoo.squidb.sql.Property;
import com.yahoo.squidb.sql.Property.PropertyVisitor;
import com.yahoo.squidb.sql.Query;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A wrapper around a {@link ICursor} that allows clients to extract individual {@link Property properties} or read an
 * entire {@link AbstractModel model} from a row in the cursor. After obtaining a cursor (such as from
 * {@link SquidDatabase#query(Class, com.yahoo.squidb.sql.Query) SquidDatabase.query}), as long as it is at a valid
 * position, you can read properties any of the following ways:
 *
 * <pre>
 * // read a single property
 * String name = cursor.get(Model.NAME);
 * // create a new Model instance with properties from the current row
 * Model model = new Model(cursor);
 * // read values from the current row into an existing model instance
 * model.readPropertiesFromCursor(cursor);
 * </pre>
 *
 * @param <TYPE> the model type that can be read or constructed from this cursor
 */
public class SquidCursor<TYPE extends AbstractModel> implements ICursor {

    /** Model class that is suggested for reading from this cursor */
    private final Class<TYPE> modelHint;

    /** Properties read by this cursor */
    private final List<? extends Field<?>> fields;

    /** Property reading visitor */
    private static final CursorReadingVisitor reader = new CursorReadingVisitor();

    /** Wrapped cursor */
    private final ICursor cursor;

    /**
     * Create a SquidCursor from the supplied {@link ICursor}
     *
     * @param cursor the backing cursor
     * @param fields properties read from this cursor
     */
    public SquidCursor(@Nonnull ICursor cursor, @Nullable Class<TYPE> modelHint, @Nonnull List<? extends Field<?>> fields) {
        this.cursor = cursor;
        this.modelHint = modelHint;
        this.fields = fields;
    }

    /**
     * Get the value for column corresponding to the given {@link Property}
     *
     * @param property the property corresponding to the desired column
     * @return the value of the property
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public <PROPERTY_TYPE> PROPERTY_TYPE get(@Nonnull Property<PROPERTY_TYPE> property) {
        return (PROPERTY_TYPE) property.accept(reader, this);
    }

    /**
     * @return the {@link ICursor} backing this SquidCursor. If you are on Android and you need to pass this object
     * across process boundaries, and if this SquidCursor was obtained from a SquidDatabase, you can safely cast
     * the object returned by this method to an Android cursor
     */
    @Nonnull
    public ICursor getCursor() {
        return cursor;
    }

    /**
     * @return the class object that represents a "hint" about which class should be used to read from this cursor.
     * This class is only a suggestion, and may be null if the cursor was not constructed with a model hint (this
     * may be the case if a null class was passed to {@link SquidDatabase#query(Class, Query)})
     */
    @Nullable
    public Class<TYPE> getModelHintClass() {
        return modelHint;
    }

    /**
     * @return the list of {@link Field fields} in this cursor
     */
    @Nonnull
    public List<? extends Field<?>> getFields() {
        return fields;
    }

    @Override
    public int getCount() {
        return cursor.getCount();
    }

    @Override
    public int getPosition() {
        return cursor.getPosition();
    }

    @Override
    public boolean move(int offset) {
        return cursor.move(offset);
    }

    @Override
    public boolean moveToPosition(int position) {
        return cursor.moveToPosition(position);
    }

    @Override
    public boolean moveToFirst() {
        return cursor.moveToFirst();
    }

    @Override
    public boolean moveToLast() {
        return cursor.moveToLast();
    }

    @Override
    public boolean moveToNext() {
        return cursor.moveToNext();
    }

    @Override
    public boolean moveToPrevious() {
        return cursor.moveToPrevious();
    }

    @Override
    public boolean isFirst() {
        return cursor.isFirst();
    }

    @Override
    public boolean isLast() {
        return cursor.isLast();
    }

    @Override
    public boolean isBeforeFirst() {
        return cursor.isBeforeFirst();
    }

    @Override
    public boolean isAfterLast() {
        return cursor.isAfterLast();
    }

    @Override
    public int getColumnIndex(@Nonnull String columnName) {
        return cursor.getColumnIndex(columnName);
    }

    @Override
    public int getColumnIndexOrThrow(@Nonnull String columnName) throws IllegalArgumentException {
        return cursor.getColumnIndexOrThrow(columnName);
    }

    @Override
    @Nonnull
    public String getColumnName(int columnIndex) {
        return cursor.getColumnName(columnIndex);
    }

    @Override
    @Nonnull
    public String[] getColumnNames() {
        return cursor.getColumnNames();
    }

    @Override
    public int getColumnCount() {
        return cursor.getColumnCount();
    }

    @Override
    @Nullable
    public byte[] getBlob(int columnIndex) {
        return cursor.getBlob(columnIndex);
    }

    @Override
    @Nullable
    public String getString(int columnIndex) {
        return cursor.getString(columnIndex);
    }

    @Override
    public short getShort(int columnIndex) {
        return cursor.getShort(columnIndex);
    }

    @Override
    public int getInt(int columnIndex) {
        return cursor.getInt(columnIndex);
    }

    @Override
    public long getLong(int columnIndex) {
        return cursor.getLong(columnIndex);
    }

    @Override
    public float getFloat(int columnIndex) {
        return cursor.getFloat(columnIndex);
    }

    @Override
    public double getDouble(int columnIndex) {
        return cursor.getDouble(columnIndex);
    }

    @Override
    public int getType(int columnIndex) {
        return cursor.getType(columnIndex);
    }

    @Override
    public boolean isNull(int columnIndex) {
        return cursor.isNull(columnIndex);
    }

    @Override
    public void close() {
        cursor.close();
    }

    @Override
    public boolean isClosed() {
        return cursor.isClosed();
    }

    /**
     * Visitor that reads properties from a cursor
     */
    private static class CursorReadingVisitor implements PropertyVisitor<Object, SquidCursor<?>> {

        @Override
        @Nullable
        public Object visitDouble(@Nonnull Property<Double> property, @Nonnull SquidCursor<?> cursor) {
            int column = columnIndex(property, cursor);
            if (cursor.isNull(column)) {
                return null;
            }
            return cursor.getDouble(column);
        }

        @Override
        @Nullable
        public Object visitInteger(@Nonnull Property<Integer> property, @Nonnull SquidCursor<?> cursor) {
            int column = columnIndex(property, cursor);
            if (cursor.isNull(column)) {
                return null;
            }
            return cursor.getInt(column);
        }

        @Override
        @Nullable
        public Object visitLong(@Nonnull Property<Long> property, @Nonnull SquidCursor<?> cursor) {
            int column = columnIndex(property, cursor);
            if (cursor.isNull(column)) {
                return null;
            }
            return cursor.getLong(column);
        }

        @Override
        @Nullable
        public Object visitString(@Nonnull Property<String> property, @Nonnull SquidCursor<?> cursor) {
            int column = columnIndex(property, cursor);
            if (cursor.isNull(column)) {
                return null;
            }
            return cursor.getString(column);
        }

        @Override
        @Nullable
        public Object visitBoolean(@Nonnull Property<Boolean> property, @Nonnull SquidCursor<?> cursor) {
            int column = columnIndex(property, cursor);
            if (cursor.isNull(column)) {
                return null;
            }
            int value = cursor.getInt(column);
            return value != 0;
        }

        @Override
        @Nullable
        public Object visitBlob(@Nonnull Property<byte[]> property, @Nonnull SquidCursor<?> cursor) {
            int column = columnIndex(property, cursor);
            if (cursor.isNull(column)) {
                return null;
            }
            return cursor.getBlob(column);
        }

        private int columnIndex(@Nonnull Property<?> property, @Nonnull SquidCursor<?> cursor) {
            return cursor.getColumnIndexOrThrow(property.getName());
        }

    }

}
