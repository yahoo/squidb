/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.data;

import android.database.Cursor;
import android.database.CursorWrapper;
import android.os.Bundle;

import com.yahoo.squidb.sql.Field;
import com.yahoo.squidb.sql.Property;
import com.yahoo.squidb.sql.Property.PropertyVisitor;

import java.util.List;

/**
 * A wrapper around a {@link Cursor} that allows clients to extract individual {@link Property properties} or read an
 * entire {@link AbstractModel model} from a row in the cursor. After obtaining a cursor (such as from
 * {@link DatabaseDao#query(Class, com.yahoo.squidb.sql.Query) DatabaseDao.query}), as long as it is at a valid
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
public class SquidCursor<TYPE extends AbstractModel> extends CursorWrapper {

    /** Properties read by this cursor */
    private final List<? extends Field<?>> fields;

    /** Property reading visitor */
    private static final CursorReadingVisitor reader = new CursorReadingVisitor();

    /** Wrapped cursor */
    private final Cursor cursor;

    /** An optional Bundle that can contain out-of-band metadata about this cursor */
    private Bundle extras;

    /**
     * Create a SquidCursor from the supplied {@link Cursor}
     *
     * @param cursor the backing cursor
     * @param fields properties read from this cursor
     */
    public SquidCursor(Cursor cursor, List<? extends Field<?>> fields) {
        super(cursor);
        this.cursor = cursor;
        this.fields = fields;
        setExtras(cursor.getExtras());
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
     * @return the {@link Cursor} backing this SquidCursor
     */
    public Cursor getCursor() {
        return cursor;
    }

    /**
     * @return the list of {@link Field fields} in this cursor
     */
    public List<? extends Field<?>> getFields() {
        return fields;
    }

    /**
     * Sets a {@link Bundle} that will be returned by {@link #getExtras()}. <code>null</code> will be converted into
     * {@link Bundle#EMPTY}.
     *
     * @param extras the Bundle to set
     */
    public void setExtras(Bundle extras) {
        this.extras = extras == null ? Bundle.EMPTY : extras;
    }

    @Override
    public Bundle getExtras() {
        return extras;
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
