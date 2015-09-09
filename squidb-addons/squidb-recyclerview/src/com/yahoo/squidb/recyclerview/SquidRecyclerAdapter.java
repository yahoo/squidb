/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.recyclerview;

import android.support.v7.widget.RecyclerView;

import com.yahoo.squidb.data.AbstractModel;
import com.yahoo.squidb.data.SquidCursor;

/**
 * RecyclerView.Adapter implementation backed by a {@link SquidCursor}
 *
 * @param <M> the model type of the backing SquidCursor
 * @param <V> a RecyclerView.ViewHolder implementation
 */
public abstract class SquidRecyclerAdapter<M extends AbstractModel, V extends SquidViewHolder<? extends M>>
        extends RecyclerView.Adapter<V> {

    private SquidCursor<M> cursor;

    /**
     * Construct a new SquidRecyclerAdapter
     */
    public SquidRecyclerAdapter() {
        this(null);
    }

    /**
     * Construct a new SquidRecyclerAdapter backed by the specified SquidCursor
     */
    public SquidRecyclerAdapter(SquidCursor<M> cursor) {
        this.cursor = cursor;
    }

    @Override
    public int getItemCount() {
        return cursor == null ? 0 : cursor.getCount();
    }

    /**
     * @return the SquidCursor backing the adapter
     */
    public SquidCursor<M> getCursor() {
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
     * ViewHolder.item is populated with valiues from the backing cursor, so it is not necessary to populate the item
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
    public void changeCursor(SquidCursor<M> newCursor) {
        SquidCursor<M> oldCursor = swapCursor(newCursor);
        if (oldCursor != null) {
            oldCursor.close();
        }
    }

    /**
     * Change the SquidCursor backing the adapter, returning the old one. Unlike {@link #changeCursor(SquidCursor)},
     * the returned old SquidCursor is <em>not</em> closed.
     *
     * @param newCursor the new SquidCursor
     * @return Returns the previously set SquidCursor. If no SquidCursor was previously set, new SquidCursor is the
     * same instance is the previously set one, null is returned.
     */
    public SquidCursor<M> swapCursor(SquidCursor<M> newCursor) {
        if (cursor == newCursor) {
            return null;
        }
        SquidCursor<M> oldCursor = cursor;
        cursor = newCursor;
        notifyDataSetChanged();
        return oldCursor;
    }
}
