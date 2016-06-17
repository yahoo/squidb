/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.data;

import com.yahoo.squidb.sql.Field;
import com.yahoo.squidb.sql.Property;
import com.yahoo.squidb.sql.Property.PropertyVisitor;

import java.util.List;

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
    public SquidCursor(ICursor cursor, List<? extends Field<?>> fields) {
        this.cursor = cursor;
        this.fields = fields;
    }

    /**
     * Get the value for column corresponding to the given {@link Property}
     *
     * @param property the property corresponding to the desired column
     * @return the value of the property
     */
    @SuppressWarnings("unchecked")
    public <PROPERTY_TYPE> PROPERTY_TYPE get(Property<PROPERTY_TYPE> property) {
        return (PROPERTY_TYPE) property.accept(reader, this);
    }

    /**
     * @return the {@link ICursor} backing this SquidCursor. If you are on Android and you need to pass this object
     * across process boundaries, and if this SquidCursor was obtained from a SquidDatabase, you can safely cast
     * the object returned by this method to an Android cursor
     */
    public ICursor getCursor() {
        return cursor;
    }

    /**
     * @return the list of {@link Field fields} in this cursor
     */
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
    public int getColumnIndex(String columnName) {
        return cursor.getColumnIndex(columnName);
    }

    @Override
    public int getColumnIndexOrThrow(String columnName) throws IllegalArgumentException {
        return cursor.getColumnIndexOrThrow(columnName);
    }

    @Override
    public String getColumnName(int columnIndex) {
        return cursor.getColumnName(columnIndex);
    }

    @Override
    public String[] getColumnNames() {
        return cursor.getColumnNames();
    }

    @Override
    public int getColumnCount() {
        return cursor.getColumnCount();
    }

    @Override
    public byte[] getBlob(int columnIndex) {
        return cursor.getBlob(columnIndex);
    }

    @Override
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
        public Object visitDouble(Property<Double> property, SquidCursor<?> cursor) {
            int column = columnIndex(property, cursor);
            if (cursor.isNull(column)) {
                return null;
            }
            return cursor.getDouble(column);
        }

        @Override
        public Object visitInteger(Property<Integer> property, SquidCursor<?> cursor) {
            int column = columnIndex(property, cursor);
            if (cursor.isNull(column)) {
                return null;
            }
            return cursor.getInt(column);
        }

        @Override
        public Object visitLong(Property<Long> property, SquidCursor<?> cursor) {
            int column = columnIndex(property, cursor);
            if (cursor.isNull(column)) {
                return null;
            }
            return cursor.getLong(column);
        }

        @Override
        public Object visitString(Property<String> property, SquidCursor<?> cursor) {
            int column = columnIndex(property, cursor);
            if (cursor.isNull(column)) {
                return null;
            }
            return cursor.getString(column);
        }

        @Override
        public Object visitBoolean(Property<Boolean> property, SquidCursor<?> cursor) {
            int column = columnIndex(property, cursor);
            if (cursor.isNull(column)) {
                return null;
            }
            int value = cursor.getInt(column);
            return value != 0;
        }

        @Override
        public Object visitBlob(Property<byte[]> property, SquidCursor<?> cursor) {
            int column = columnIndex(property, cursor);
            if (cursor.isNull(column)) {
                return null;
            }
            return cursor.getBlob(column);
        }

        private int columnIndex(Property<?> property, SquidCursor<?> cursor) {
            return cursor.getColumnIndexOrThrow(property.getName());
        }

    }

}
