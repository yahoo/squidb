/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.sample.models;

import com.yahoo.squidb.annotations.ColumnSpec;
import com.yahoo.squidb.annotations.ModelMethod;
import com.yahoo.squidb.annotations.TableModelSpec;

@TableModelSpec(className = "Task", tableName = "tasks")
public class TaskSpec {

    @ColumnSpec(constraints = "NOT NULL")
    String title;

    @ColumnSpec(defaultValue = "0")
    long completionDate;

    @ColumnSpec(defaultValue = "0")
    long dueDate;

    @ColumnSpec(defaultValue = "0")
    int priority;

    @ModelMethod
    public static boolean isCompleted(Task instance) {
        return instance.getCompletionDate() > 0;
    }
}
