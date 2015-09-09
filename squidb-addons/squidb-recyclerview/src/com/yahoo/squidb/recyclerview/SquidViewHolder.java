/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.recyclerview;

import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.yahoo.squidb.data.AbstractModel;

/**
 * Base RecyclerView.ViewHolder that also holds a SquiDB model instance called {@link #item}, which is recycled along
 * with the itemView. An empty model instance should be passed to the ViewHolder's
 * constructor. For most subclasses, a pattern like the following is sufficient:
 *
 * <pre>
 * public class SampleViewHolder extends SquidViewHolder&lt;SampleModel&gt; {
 *
 *     public SampleViewHolder(View itemView) {
 *         super(itemView, new SampleModel());
 *     }
 * }
 * </pre>
 * When used with {@link SquidRecyclerAdapter}, the item member can be used inside of {@link
 * SquidRecyclerAdapter#onBindSquidViewHolder(SquidViewHolder, int) onBindSquidViewHolder} to help bind data to the
 * itemView. At this time the item has already been populated with data from the cursor.
 */
public abstract class SquidViewHolder<T extends AbstractModel> extends RecyclerView.ViewHolder {

    public final T item;

    /**
     * Create a new SquidViewHolder instance.
     *
     * @param itemView the item view
     * @param item a model instance. This item does not need to be populated with data; it will be populated
     * automatically in {@link SquidRecyclerAdapter#onBindViewHolder(SquidViewHolder, int)}.
     */
    public SquidViewHolder(View itemView, T item) {
        super(itemView);
        this.item = item;
    }
}
