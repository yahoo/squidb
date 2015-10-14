/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.android;

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
 * <p>
 * By default, if a subclass of {@link TableModel} is passed to the one-arg constructor, the adapter will use the ID
 * property of the associated table for {@link #getItemId(int)}. In that case {@link #hasStableIds()} will return
 * true; otherwise it returns false and {@link #getItemId(int)} returns 0. You should override both these if other
 * behavior is desired.
 * <p>
 * If you use the one-arg constructor with a subclass of TableModel, or you use the two-arg constructor with a
 * non-null second argument, be sure that the appropriate ID column is present in any cursor given to this adapter.
 *
 * @param <T> the model type of the SquidCursor backing this adapter
 */
public abstract class SquidCursorAdapter<T extends AbstractModel> extends BaseAdapter {

    private SquidCursor<? extends T> cursor;
    private final T model;
    private final Property<Long> columnForId;

    /**
     * Construct a SquidCursorAdapter that will use the model class's default id property to implement
     * {@link #getItemId(int)}.
     *
     * @param model an instance of the model type to use for this cursor. See note at the top of this file.
     * @see #SquidCursorAdapter(AbstractModel, Property)
     */
    public SquidCursorAdapter(T model) {
        this(model, model instanceof TableModel ? ((TableModel) model).getIdProperty() : null);
    }

    /**
     * Construct a SquidCursorAdapter. If <code>columnForId</code> is not null, it will be used to implement {@link
     * #getItemId(int)}. This should be a column that is distinct and non-null for every row in the cursor.
     *
     * @param model an instance of the model type to use for this cursor. See note at the top of this file.
     * @param columnForId a column to use for {@link #getItemId(int)}.
     */
    public SquidCursorAdapter(T model, Property<Long> columnForId) {
        super();
        this.model = model;
        this.columnForId = columnForId;
    }

    /**
     * @return the cursor backing this adapter
     */
    public SquidCursor<? extends T> getCursor() {
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
        if (hasStableIds()) {
            if (cursor != null && cursor.moveToPosition(position)) {
                return cursor.get(columnForId);
            }
        }
        return 0;
    }

    @Override
    public boolean hasStableIds() {
        return columnForId != null;
    }

    /**
     * Change the cursor backing this adapter and return the old cursor. This does <em>not</em> close the old cursor.
     *
     * @param newCursor the new cursor
     * @return The old cursor. If there was no previously set cursor or the new Cursor and the old cursor are the same
     * instance, this method returns {@code null}.
     */
    public SquidCursor<? extends T> swapCursor(SquidCursor<? extends T> newCursor) {
        if (newCursor == this.cursor) {
            return null;
        }

        SquidCursor<? extends T> oldCursor = this.cursor;
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
    public void changeCursor(SquidCursor<? extends T> newCursor) {
        SquidCursor<? extends T> oldCursor = swapCursor(newCursor);
        if (oldCursor != null) {
            oldCursor.close();
        }
    }
}
