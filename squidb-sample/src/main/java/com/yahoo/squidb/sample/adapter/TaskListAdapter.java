/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.sample.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import com.yahoo.squidb.sample.models.Task;
import com.yahoo.squidb.utility.SquidCursorAdapter;

public class TaskListAdapter extends SquidCursorAdapter<Task> {

    public TaskListAdapter(Context context, Task model) {
        super(context, model);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return null;
    }
}
