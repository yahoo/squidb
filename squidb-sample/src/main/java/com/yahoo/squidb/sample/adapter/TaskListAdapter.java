/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.sample.adapter;

import android.content.Context;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.yahoo.squidb.android.SquidCursorAdapter;
import com.yahoo.squidb.sample.R;
import com.yahoo.squidb.sample.models.Task;
import com.yahoo.squidb.sample.utils.TaskUtils;

public class TaskListAdapter extends SquidCursorAdapter<Task> {

    private LayoutInflater layoutInflater;

    public TaskListAdapter(Context context, Task model) {
        super(model);
        layoutInflater = LayoutInflater.from(context);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = layoutInflater.inflate(R.layout.task_row, parent, false);
            convertView.setTag(new TaskRowViewHolder(convertView));
        }

        TaskRowViewHolder viewHolder = (TaskRowViewHolder) convertView.getTag();

        Task item = getItem(position);
        viewHolder.taskTitle.setText(item.getTitle());
        if (item.isCompleted()) {
            viewHolder.taskTitle.setPaintFlags(viewHolder.taskTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        } else {
            viewHolder.taskTitle.setPaintFlags(viewHolder.taskTitle.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
        }

        // Note how the model object can contain a property that isn't part of its core definition--the property
        // was in the query, so it's read into the adapter model and we can access it using the generic getter
        viewHolder.taskTags.setText(item.get(TaskUtils.TAGS_CONCAT));
        return convertView;
    }

    private static class TaskRowViewHolder {

        TextView taskTitle;
        TextView taskTags;

        public TaskRowViewHolder(View row) {
            taskTitle = (TextView) row.findViewById(R.id.task_title);
            taskTags = (TextView) row.findViewById(R.id.task_tags);
        }
    }
}
