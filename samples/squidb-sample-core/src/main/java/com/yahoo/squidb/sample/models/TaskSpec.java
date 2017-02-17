/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.sample.models;

import com.yahoo.squidb.annotations.ColumnName;
import com.yahoo.squidb.annotations.ColumnSpec;
import com.yahoo.squidb.annotations.ModelMethod;
import com.yahoo.squidb.annotations.PrimaryKey;
import com.yahoo.squidb.annotations.TableModelSpec;
import com.yahoo.squidb.annotations.defaults.DefaultLong;

@TableModelSpec(className = "Task", tableName = "tasks")
public class TaskSpec {

    @PrimaryKey
    @ColumnName("_id")
    long id;

    @ColumnSpec(constraints = "NOT NULL")
    String title;

    @DefaultLong(0)
    long completionDate;

    @DefaultLong(0)
    long dueDate;

    @DefaultLong(0)
    int priority;

    @ModelMethod
    public static boolean isCompleted(Task instance) {
        return instance.getCompletionDate() > 0;
    }
}
