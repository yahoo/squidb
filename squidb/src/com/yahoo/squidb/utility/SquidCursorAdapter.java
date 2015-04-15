/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.utility;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.Adapter;
import android.widget.BaseAdapter;

import com.yahoo.squidb.data.AbstractModel;
import com.yahoo.squidb.data.SquidCursor;
import com.yahoo.squidb.data.TableModel;
import com.yahoo.squidb.sql.Property;

/**
 * A base {@link Adapter} implementation backed by a {@link SquidCursor}. Subclass implementations typically supply a
 * new instance of the model class inside the constructor like so:
 *
 * <pre>
 * public ExampleAdapter(Context context) {
 *     super(context, new Model());
 * }
 * </pre>
 *
 * Note that this model instance will be reused whenever {@link #getItem(int)} is called. If you need to compare items
 * in the data set, you should either clone the value returned by {@link #getItem(int)}, construct new model instances
 * using {@link AbstractModel#readPropertiesFromCursor(SquidCursor) readPropertiesFromCursor}, or read values from the
 * backing cursor directly.
 *
 * By default, {@link #hasStableIds()} returns false. You should override it to return true if your adapter will have
 * stable ids.
 *
 * @param <T> the model type of the SquidCursor backing this adapter
 */
public abstract class SquidCursorAdapter<T extends AbstractModel> extends BaseAdapter {

    private SquidCursor<T> cursor;
    private final Context context;
    private final LayoutInflater inflater;
    private final T model;
    private final Property<Long> columnForId;

    /**
     * Equivalent to SquidCursorAdapter(context, model, null). Should be used for TableModel cursors where the _id
     * column is present.
     *
     * @param model an instance of the model type to use for this cursor. See note at the top of this file.
     */
    public SquidCursorAdapter(Context context, T model) {
        this(context, model, null);
    }

    /**
     * @param model an instance of the model type to use for this cursor. See note at the top of this file.
     * @param columnForId a column to use for {@link #getItemId(int)}. This should be a column that is distinct and
     * non-null for every row in the cursor. If one is not specified, getItemId() will fall back to reading
     * the _id column. It will throw an exception if no column is specified and the cursor doesn't contain an
     * _id column.
     */
    public SquidCursorAdapter(Context context, T model, Property<Long> columnForId) {
        super();
        this.context = context;
        this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.model = model;
        this.columnForId = columnForId;
    }

    /**
     * @return the cursor backing this adapter
     */
    public SquidCursor<T> getCursor() {
        return this.cursor;
    }

    /**
     * @return the current position of the backing cursor
     */
    protected int getPosition() {
        if (this.cursor == null) {
            return -1;
        }
        return this.cursor.getPosition();
    }

    /**
     * @return an internal {@link Context} object
     */
    protected Context getContext() {
        return this.context;
    }

    /**
     * @return an internal {@link LayoutInflater} for creating views
     */
    protected LayoutInflater getLayoutInflater() {
        return this.inflater;
    }

    @Override
    public int getCount() {
        return this.cursor != null ? this.cursor.getCount() : 0;
    }

    /**
     * Get a model instance representing the item at the specified position. The object returned by this method is a
     * singleton/shared instance. If you do this:
     *
     * <pre>
     * Model item0 = getItem(0);
     * Model item1 = getItem(1);
     * </pre>
     *
     * Then item0 and item1 will be the same object with the same values (those at position 1). We recommend you not
     * call getItem() outside of {@link #getView(int, android.view.View, android.view.ViewGroup) getView()} or on
     * different positions in the same method scope, and definitely don't call it from other threads. If you do need a
     * non-shared instance, you can clone the object returned by this method.
     *
     * @return the model object at the specified cursor position
     */
    @Override
    public T getItem(int position) {
        if (this.cursor == null) {
            return null;
        }
        cursor.moveToPosition(position);
        model.readPropertiesFromCursor(cursor);
        return model;
    }

    @Override
    public long getItemId(int position) {
        Property<Long> idProperty = columnForId != null ? columnForId : TableModel.ID_PROPERTY;
        if (cursor != null && cursor.moveToPosition(position)) {
            return cursor.get(idProperty);
        }
        return 0;
    }

    /**
     * Change the cursor backing this adapter and return the old cursor. This does <em>not</em> close the old cursor.
     *
     * @param newCursor the new cursor
     * @return The old cursor. If there was no previously set cursor or the new Cursor and the old cursor are the same
     * instance, this method returns {@code null}.
     */
    public SquidCursor<T> swapCursor(SquidCursor<T> newCursor) {
        if (newCursor == this.cursor) {
            return null;
        }

        SquidCursor<T> oldCursor = this.cursor;
        this.cursor = newCursor;
        if (newCursor != null) {
            notifyDataSetChanged();
        } else {
            notifyDataSetInvalidated();
        }
        return oldCursor;
    }

    /**
     * Change the cursor backing this adapter and close the old cursor if necessary
     *
     * @param newCursor the new cursor
     */
    public void changeCursor(SquidCursor<T> newCursor) {
        SquidCursor<T> oldCursor = swapCursor(newCursor);
        if (oldCursor != null) {
            oldCursor.close();
        }
    }
}
