/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.recyclerview;

import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.Adapter;

import com.yahoo.squidb.data.AbstractModel;
import com.yahoo.squidb.data.SquidCursor;
import com.yahoo.squidb.sql.Property;

/**
 * RecyclerView.Adapter implementation backed by a {@link SquidCursor}. Subclasses must also define an implentation
 * of {@link SquidViewHolder}, which recycles a SquiDB model instance along with the associated itemView. This
 * adapter automatically populates the model instance with data from the backing SquidCursor, so that it can be used
 * to help bind data to the itemView inside of {@link #onBindSquidViewHolder(SquidViewHolder, int)
 * onBindSquidViewHolder}.
 * <p>
 * If you specify an ID property, the adapter will report that it has stable item IDs (see {@link Adapter#hasStableIds()
 * hasStableIds()}) and uses this property to implement {@link #getItemId(int)}. Make sure any cursor given to this
 * adapter contains the appropriate ID column.
 *
 * @param <M> the model type of the backing SquidCursor
 * @param <V> a SquidViewHolder implementation
 */
public abstract class SquidRecyclerAdapter<M extends AbstractModel, V extends SquidViewHolder<? extends M>>
        extends RecyclerView.Adapter<V> {

    private SquidCursor<? extends M> cursor;
    private Property<Long> idProperty;

    /**
     * Construct a new SquidRecyclerAdapter
     */
    public SquidRecyclerAdapter() {
        this(null);
    }

    /**
     * Construct a new SquidRecyclerAdapter that uses the specified column to determine item IDs in {@link
     * #getItemId(int)}. This should be a column that is distinct and non-null for every row in the cursor. If this
     * argument is not null, the adapter will report that it has stable item IDs (see {@link Adapter#hasStableIds()
     * hasStableIds()}).
     *
     * @param idProperty the column to use for item IDs
     */
    public SquidRecyclerAdapter(Property<Long> idProperty) {
        this.idProperty = idProperty;
        if (idProperty != null) {
            setHasStableIds(true);
        }
    }

    @Override
    public int getItemCount() {
        return cursor == null ? 0 : cursor.getCount();
    }

    @Override
    public long getItemId(int position) {
        if (hasStableIds()) {
            if (cursor != null && cursor.moveToPosition(position)) {
                return cursor.get(idProperty);
            }
            return RecyclerView.NO_ID;
        }
        return super.getItemId(position);
    }

    /**
     * @return the SquidCursor backing the adapter
     */
    public SquidCursor<? extends M> getCursor() {
        return cursor;
    }

    @Override
    public void onBindViewHolder(V holder, int position) {
        if (cursor == null || !cursor.moveToPosition(position)) {
            throw new IllegalStateException("this should only be called when the cursor is valid");
        }
        holder.item.readPropertiesFromCursor(cursor);
        onBindSquidViewHolder(holder, position);
    }

    /**
     * Update the contents of the ViewHolder.itemView to reflect the item at the given position. At this point the
     * ViewHolder.item is populated with values from the backing cursor, so it is not necessary to populate the item
     * yourself.
     *
     * @param holder the SquidViewHolder that should represent the contents of the item at the given position
     * @param position the position of the item in the data set
     */
    public abstract void onBindSquidViewHolder(V holder, int position);

    /**
     * Change the SquidCursor backing the adapter. If there is an existing SquidCursor it will be closed.
     *
     * @param newCursor the new SquidCursor
     */
    public void changeCursor(SquidCursor<? extends M> newCursor) {
        SquidCursor<?> oldCursor = swapCursor(newCursor);
        if (oldCursor != null) {
            oldCursor.close();
        }
    }

    /**
     * Change the SquidCursor backing the adapter, returning the old one. Unlike {@link #changeCursor(SquidCursor)},
     * the returned old SquidCursor is <em>not</em> closed.
     *
     * @param newCursor the new SquidCursor
     * @return the previously set SquidCursor. If no SquidCursor was previously set, or if the new SquidCursor
     * is the same instance as the previously set one, null is returned.
     */
    public SquidCursor<? extends M> swapCursor(SquidCursor<? extends M> newCursor) {
        if (cursor == newCursor) {
            return null;
        }
        SquidCursor<? extends M> oldCursor = cursor;
        cursor = newCursor;
        notifyDataSetChanged();
        return oldCursor;
    }
}
